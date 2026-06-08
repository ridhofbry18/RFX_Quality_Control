package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"
    private val context = application.applicationContext

    // 1. Inisialisasi Database Room & Repository secara mandiri dan aman
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rfx_journal_db"
        ).fallbackToDestructiveMigration().build()
    }
    
    private val repository: AppRepository by lazy {
        AppRepository(context, database.appDao())
    }

    // 1.1 Integration SharedPreferences Configuration
    private val prefs = context.getSharedPreferences("rfx_journal_prefs", Context.MODE_PRIVATE)

    // Persistent Profile Configuration
    private val _profileName = MutableStateFlow(prefs.getString("profile_name", "Muhammad Ridho F.") ?: "Muhammad Ridho F.")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileDesc = MutableStateFlow(prefs.getString("profile_desc", "Aesthetic Videography & Code") ?: "Aesthetic Videography & Code")
    val profileDesc: StateFlow<String> = _profileDesc.asStateFlow()

    private val _profileImg = MutableStateFlow(prefs.getString("profile_img", "") ?: "")
    val profileImg: StateFlow<String> = _profileImg.asStateFlow()

    fun updateProfile(name: String, desc: String, uri: Uri?) {
        viewModelScope.launch {
            var savedPath = _profileImg.value
            if (uri != null) {
                val newSavedPath = SyncEngine.saveImageToInternalStorage(context, uri)
                if (newSavedPath != null) {
                    savedPath = newSavedPath
                }
            }
            prefs.edit().apply {
                putString("profile_name", name)
                putString("profile_desc", desc)
                putString("profile_img", savedPath)
                apply()
            }
            _profileName.value = name
            _profileDesc.value = desc
            _profileImg.value = savedPath
        }
    }

    private val _tursoDbUrl = MutableStateFlow(prefs.getString("turso_db_url", "") ?: "")
    val tursoDbUrl: StateFlow<String> = _tursoDbUrl.asStateFlow()

    private val _tursoToken = MutableStateFlow(prefs.getString("turso_token", "") ?: "")
    val tursoToken: StateFlow<String> = _tursoToken.asStateFlow()

    private val _googleDriveFolderId = MutableStateFlow(prefs.getString("google_drive_folder_id", "") ?: "")
    val googleDriveFolderId: StateFlow<String> = _googleDriveFolderId.asStateFlow()

    private val _googleDriveOAuthToken = MutableStateFlow(prefs.getString("google_drive_oauth_token", "") ?: "")
    val googleDriveOAuthToken: StateFlow<String> = _googleDriveOAuthToken.asStateFlow()

    fun updateIntegrationSettings(dbUrl: String, token: String, folderId: String, oauthToken: String) {
        prefs.edit().apply {
            putString("turso_db_url", dbUrl)
            putString("turso_token", token)
            putString("google_drive_folder_id", folderId)
            putString("google_drive_oauth_token", oauthToken)
            apply()
        }
        _tursoDbUrl.value = dbUrl
        _tursoToken.value = token
        _googleDriveFolderId.value = folderId
        _googleDriveOAuthToken.value = oauthToken
    }

    // 2. State UI Global - Mode Gelap / Terang & Keamanan Sidik Jari
    private val _isDarkMode = MutableStateFlow(true) // Default cinematic dark mode (porto rfx style)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isFingerprintLockEnabled = MutableStateFlow(true) // Sistem pengunci sidik jari aktif
    val isFingerprintLockEnabled: StateFlow<Boolean> = _isFingerprintLockEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(true) // Status aplikasi terkunci saat mulai
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _currentTab = MutableStateFlow("dashboard") // "dashboard", "catatan", "proyek", "sandi"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // 3. State Data dari Repository yang Reactive
    val diaryEntries: StateFlow<List<DiaryEntry>> = repository.allDiaryEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyTargets: StateFlow<List<WeeklyTarget>> = repository.allWeeklyTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<Schedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Seleksi Tanggal Kalender (Default hari ini)
    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // 5. State Feedbacks Sinkronisasi Status
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Ganti preferensi tema warna
    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Ganti status aktif kunci sidik jari
    fun toggleFingerprintLock(enabled: Boolean) {
        _isFingerprintLockEnabled.value = enabled
        // Jika dimatikan, otomatis matikan status terkunci agar bebas
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    // Buka kunci aplikasi
    fun unlockApp() {
        _isAppLocked.value = false
    }

    // Kunci kembali aplikasi
    fun lockApp() {
        if (_isFingerprintLockEnabled.value) {
            _isAppLocked.value = true
        }
    }

    // Ganti tab navigasi dasar
    fun changeTab(tab: String) {
        _currentTab.value = tab
    }

    // Pilih tanggal baru di kalender mini
    fun selectCalendarDate(timestamp: Long) {
        _selectedCalendarDate.value = timestamp
    }

    // --- CATATAN HARIAN ACTIONS ---
    fun addDiaryEntry(title: String, content: String, imageUri: Uri?, mood: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Menyimpan catatan harian secara lokal..."
            
            var savedPath: String? = null
            
            // Simpan foto secara lokal ke scoped app storage
            if (imageUri != null) {
                savedPath = SyncEngine.saveImageToInternalStorage(context, imageUri)
            }

            val entry = DiaryEntry(
                title = title,
                content = content,
                date = System.currentTimeMillis(),
                imageUri = savedPath,
                mood = mood,
                isSynced = false
            )

            // Masukkan data ke DB lokal (ini secara asinkron memicu sync Turso di background)
            val insertedId = repository.insertDiaryEntry(entry)
            
            // Cepat upload ke Google Drive di background jika foto tersedia
            if (savedPath != null) {
                _syncMessage.value = "Mengunggah foto lampiran ke Google Drive..."
                // Menggunakan OAuth Token nyata dari setelan jika dikonfigurasikan
                val tokenToUse = _googleDriveOAuthToken.value.ifEmpty { "MOCK_TOKEN" }
                val success = repository.uploadAndSyncDiary(insertedId, savedPath, tokenToUse)
                if (success) {
                    _syncMessage.value = "Selesai! Catatan berhasil dicadangkan ke Turso & Google Drive."
                } else {
                    _syncMessage.value = "Catatan disimpan secara luring. Foto akan disinkronkan saat online."
                }
            } else {
                _syncMessage.value = "Selesai! Catatan disimpan ke Turso Cloud."
            }
            
            _isSyncing.value = false
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            repository.deleteDiaryEntry(entry)
        }
    }

    fun triggerTursoManually(entry: DiaryEntry) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Menyinkronkan ulang dengan Turso Cloud..."
            val success = SyncEngine.syncDiaryToTurso(context, entry)
            if (success) {
                database.appDao().updateSyncStatus(entry.id, true, entry.imageUri)
                _syncMessage.value = "Sinkronisasi Turso sukses!"
            } else {
                _syncMessage.value = "Gagal menyambung ke server Turso. Cek konfigurasi rahasia."
            }
            _isSyncing.value = false
        }
    }

    // --- TARGET & PROYEK MINGGUAN ACTIONS ---
    fun addWeeklyTarget(title: String, description: String, category: String, dueDate: Long) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
            val weekNum = cal.get(Calendar.WEEK_OF_YEAR)

            val target = WeeklyTarget(
                title = title,
                description = description,
                category = category,
                weekNumber = weekNum,
                dueDate = dueDate,
                isCompleted = false
            )
            repository.insertWeeklyTarget(target)
        }
    }

    fun toggleTargetCompletion(target: WeeklyTarget) {
        viewModelScope.launch {
            repository.updateTargetStatus(target.id, !target.isCompleted)
        }
    }

    fun deleteWeeklyTarget(target: WeeklyTarget) {
        viewModelScope.launch {
            repository.deleteWeeklyTarget(target)
        }
    }

    // --- SCHEDULE ACTIONS ---
    fun addSchedule(title: String, description: String, dateTime: Long) {
        viewModelScope.launch {
            val schedule = Schedule(
                title = title,
                description = description,
                dateTime = dateTime,
                isNotificationEnabled = true
            )
            repository.insertSchedule(schedule)
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
