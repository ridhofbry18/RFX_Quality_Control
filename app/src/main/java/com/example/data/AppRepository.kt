package com.example.data

import android.content.Context
import android.util.Log
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    private val TAG = "AppRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allDiaryEntries: Flow<List<DiaryEntry>> = appDao.getAllDiaryEntries()
    val allWeeklyTargets: Flow<List<WeeklyTarget>> = appDao.getAllWeeklyTargets()
    val allSchedules: Flow<List<Schedule>> = appDao.getAllSchedules()

    suspend fun getDiaryEntryById(id: Long): DiaryEntry? = withContext(Dispatchers.IO) {
        appDao.getDiaryEntryById(id)
    }

    /**
     * Memasukkan catatan harian baru secara lokal, lalu secara asinkron
     * mencoba melakukan sinkronisasi ke cloud Turso.
     */
    suspend fun insertDiaryEntry(entry: DiaryEntry): Long = withContext(Dispatchers.IO) {
        val id = appDao.insertDiaryEntry(entry)
        val insertedEntry = entry.copy(id = id)
        
        // Coba sinkronisasi di background agar UI responsif instan luring (offline-first)
        repositoryScope.launch {
            val success = SyncEngine.syncDiaryToTurso(context, insertedEntry)
            if (success) {
                appDao.updateSyncStatus(id, true, insertedEntry.imageUri)
            }
        }
        return@withContext id
    }

    suspend fun deleteDiaryEntry(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        appDao.deleteDiaryEntry(entry)
    }

    /**
     * Mengunggah gambar lokal ke Google Drive, mengupdate DB lokal, 
     * dan menyinkronkan data final beserta URL Drive baru ke Turso.
     */
    suspend fun uploadAndSyncDiary(entryId: Long, localImagePath: String, oauthToken: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = appDao.getDiaryEntryById(entryId) ?: return@withContext false
            Log.d(TAG, "Memulai proses upload ke Google Drive untuk Catatan ID: $entryId")

            // 1. Upload ke Google Drive dulu
            val driveUrl = SyncEngine.uploadPhotoToGoogleDrive(context, localImagePath, oauthToken)
            if (driveUrl != null) {
                // Update URI ke URL Drive di database lokal
                appDao.updateSyncStatus(entryId, false, driveUrl)
                
                // 2. Kirim update ke Turso
                val updatedEntry = entry.copy(imageUri = driveUrl)
                val tursoSuccess = SyncEngine.syncDiaryToTurso(context, updatedEntry)
                if (tursoSuccess) {
                    appDao.updateSyncStatus(entryId, true, driveUrl)
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunggah & menyelaraskan", e)
            return@withContext false
        }
    }

    /**
     * Menyisipkan target proyek mingguan, lalu menjadwalkan notifikasi realtime
     * jika tenggat waktu (dueDate) berada di masa depan.
     */
    suspend fun insertWeeklyTarget(target: WeeklyTarget): Long = withContext(Dispatchers.IO) {
        val id = appDao.insertWeeklyTarget(target)
        
        if (!target.isCompleted && target.dueDate > System.currentTimeMillis()) {
            AlarmScheduler.scheduleAlarm(
                context = context,
                id = id + 100000, // Namespace unik untuk target
                title = "Proyek: ${target.title}",
                description = "Batas waktu target proyek mingguan Anda hari ini! (${target.category})",
                triggerTimeMillis = target.dueDate
            )
        }
        return@withContext id
    }

    suspend fun updateTargetStatus(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateTargetStatus(id, completed)
        if (completed) {
            AlarmScheduler.cancelAlarm(context, id + 100000)
        }
    }

    suspend fun deleteWeeklyTarget(target: WeeklyTarget) = withContext(Dispatchers.IO) {
        appDao.deleteWeeklyTarget(target)
        AlarmScheduler.cancelAlarm(context, target.id + 100000)
    }

    /**
     * Memasukkan jadwal rutin kalender dan menjadwalkan alarm realtime.
     */
    suspend fun insertSchedule(schedule: Schedule): Long = withContext(Dispatchers.IO) {
        val id = appDao.insertSchedule(schedule)
        
        if (schedule.isNotificationEnabled && schedule.dateTime > System.currentTimeMillis()) {
            AlarmScheduler.scheduleAlarm(
                context = context,
                id = id,
                title = "Jadwal: ${schedule.title}",
                description = schedule.description,
                triggerTimeMillis = schedule.dateTime
            )
        }
        return@withContext id
    }

    suspend fun deleteSchedule(schedule: Schedule) = withContext(Dispatchers.IO) {
        appDao.deleteSchedule(schedule)
        AlarmScheduler.cancelAlarm(context, schedule.id)
    }
}
