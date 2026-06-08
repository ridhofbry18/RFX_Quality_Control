package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val date: Long, // timestamp
    val imageUri: String? = null, // Path to uploaded photo or local Uri
    val mood: String = "Neutral", // Mood indicator
    val isSynced: Boolean = false // Synced to Turso Cloud
)

@Entity(tableName = "weekly_targets")
data class WeeklyTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val weekNumber: Int, // Calendar week number of the year
    val isCompleted: Boolean = false,
    val dueDate: Long,
    val category: String = "Umum" // Kategori: Kerja, Pribadi, dll
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dateTime: Long, // Alarm / Schedule time
    val isNotificationEnabled: Boolean = true
)

@Dao
interface AppDao {
    // Diary Entries Queries
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry): Long

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getDiaryEntryById(id: Long): DiaryEntry?

    @Query("UPDATE diary_entries SET isSynced = :synced, imageUri = :driveUrl WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean, driveUrl: String?)

    // Weekly Targets Queries
    @Query("SELECT * FROM weekly_targets ORDER BY dueDate ASC")
    fun getAllWeeklyTargets(): Flow<List<WeeklyTarget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyTarget(target: WeeklyTarget): Long

    @Query("UPDATE weekly_targets SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTargetStatus(id: Long, completed: Boolean)

    @Delete
    suspend fun deleteWeeklyTarget(target: WeeklyTarget)

    // Schedule Queries
    @Query("SELECT * FROM schedules ORDER BY dateTime ASC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)
}

@Database(entities = [DiaryEntry::class, WeeklyTarget::class, Schedule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
