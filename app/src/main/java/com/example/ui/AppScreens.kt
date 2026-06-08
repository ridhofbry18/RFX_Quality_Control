package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.data.DiaryEntry
import com.example.data.Schedule
import com.example.data.WeeklyTarget
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Helper Formater Tanggal Indonesia
fun formatTimestampFull(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun formatTimestampShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun formatTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

// -------------------------------------------------------------
// 1. LOCK SCREEN (SIDIK JARI)
// -------------------------------------------------------------
@Composable
fun LockScreen(
    onUnlockSuccess: () -> Unit,
    onBypassUnlock: () -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Animasi Pulse untuk sensor sidik jari
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )

    // Trigger biometric prompt secara aman di Android
    val triggerBiometric = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            errorMessage = null
            showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            errorMessage = "Gagal memuat sistem biometrik perangkat."
        }
    }

    // Jalankan prompt pertama kali dimuat
    LaunchedEffect(Unit) {
        triggerBiometric()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Logo
            Text(
                text = "RFX SECURE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("lock_title_tag")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RFX Journal",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sistem Pengunci Jurnal & Target Proyek",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) Zinc400 else Zinc600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Visual Glowing Fingerprint Scanner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .clickable { triggerBiometric() }
                    .testTag("fingerprint_verify_button")
            ) {
                // Denyut Glowing Di Luar
                Box(
                    modifier = Modifier
                        .size(120.dp * scale)
                        .drawBehind {
                            drawCircle(
                                color = RfxRedAccent,
                                style = Stroke(width = 3.dp.toPx()),
                                alpha = opacity
                            )
                        }
                )

                // Kotak Lingkaran Tengah
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RfxRedAccent.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(1.dp, RfxRedAccent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Sensor Sidik Jari",
                        tint = RfxRedAccent,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = RfxRedAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tombol verifikasi manual / bypass
            OutlinedButton(
                onClick = { triggerBiometric() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Filled.LockOpen, contentDescription = null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coba Pindai Ulang")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fallback luring / emulator test bypass
            TextButton(
                onClick = { onBypassUnlock() },
                colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) Zinc500 else Zinc700),
                modifier = Modifier.testTag("bypass_fingerprint_button")
            ) {
                Text("Gunakan PIN Luring / Bypass Emulator", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// -------------------------------------------------------------
// PROFILE EDIT COMPOSABLES
// -------------------------------------------------------------
@Composable
fun ProfileInitials(name: String) {
    val initials = name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
    Text(
        text = initials.ifEmpty { "RFX" },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = RfxRedAccent
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentDesc: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?) -> Unit,
    isDarkMode: Boolean
) {
    var name by remember { mutableStateOf(currentName) }
    var desc by remember { mutableStateOf(currentDesc) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Profil QUALITY CONTROL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Unggah Foto Profil baru atau ubah data nama & deskripsi di bawah:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) Zinc400 else Zinc600
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, RfxRedAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RfxRedAccent)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Foto Profil Baru", fontSize = 12.sp)
                    }

                    if (selectedImageUri != null) {
                        Text(
                            "Foto Terpilih!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi / Jabatan") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && desc.isNotEmpty()) {
                        onSave(name, desc, selectedImageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RfxRedAccent, contentColor = Color.White),
                modifier = Modifier.testTag("submit_edit_profile_button")
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) Zinc400 else Zinc700)
            ) {
                Text("Batal")
            }
        }
    )
}

// -------------------------------------------------------------
// 2. DASHBOARD VIEW (PROGRESS & KALENDER)
// -------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val entries by viewModel.diaryEntries.collectAsState()
    val targets by viewModel.weeklyTargets.collectAsState()
    val schedulesList by viewModel.schedules.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()

    val profileName by viewModel.profileName.collectAsState()
    val profileDesc by viewModel.profileDesc.collectAsState()
    val profileImg by viewModel.profileImg.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Hitung progress mingguan
    val currentWeekTargets = remember(targets) {
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        targets.filter { it.weekNumber == currentWeek }
    }
    
    val totalCount = currentWeekTargets.size
    val completedCount = currentWeekTargets.count { it.isCompleted }
    val progressPercentage = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0f

    // Total diary entry count acts as streak
    val journalStreak = entries.size

    // Agenda Terpilih Hari Ini
    val filteredSchedules = remember(schedulesList, selectedDate) {
        schedulesList.filter { isSameDay(it.dateTime, selectedDate) }
    }

    val scaffoldColor = MaterialTheme.colorScheme.background

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen_tag")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isDarkMode) {
                                listOf(CoreDarkSurface, CoreDarkSurfaceElevated)
                            } else {
                                listOf(CoreLightSurface, CoreLightSurfaceElevated)
                            }
                        )
                    )
                    .border(
                        1.dp, 
                        if (isDarkMode) CoreDarkBorder else CoreLightBorder, 
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RfxRedAccent.copy(alpha = 0.15f))
                                .border(1.dp, RfxRedAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImg.isNotEmpty()) {
                                val file = File(profileImg)
                                if (file.exists()) {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Foto Profil",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        ProfileInitials(profileName)
                                    }
                                } else {
                                    ProfileInitials(profileName)
                                }
                            } else {
                                ProfileInitials(profileName)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Selamat datang,",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkMode) Zinc400 else Zinc600
                            )
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profileDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = RfxRedAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Streak Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RfxRedAccent.copy(alpha = 0.15f))
                            .border(1.dp, RfxRedAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = RfxRedAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$journalStreak Jurnal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RfxRedAccent
                            )
                        }
                    }
                }
            }
        }

        // Section: ANALITIK PROGRES MINGGUAN (Up to date UI)
        item {
            Text(
                text = "Dashboard Analitik Progres",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
                    .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Circular Progress Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        // Background circle arc
                        drawArc(
                            color = if (isDarkMode) CoreDarkBorder else CoreLightBorder,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Active progress arc
                        drawArc(
                            color = RfxRedAccent,
                            startAngle = -90f,
                            sweepAngle = progressPercentage * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Selesai",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Zinc500 else Zinc700
                        )
                    }
                }

                // Analytics Summary Labels
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "Proyek Minggu Ini",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(RfxRedAccent, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Terselesaikan: $completedCount / $totalCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Zinc300 else Zinc700
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(if (isDarkMode) CoreDarkBorder else CoreLightBorder, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sedang Jalan: ${totalCount - completedCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Zinc300 else Zinc700
                        )
                    }
                }
            }
        }

        // Section: KALENDER INTEGRASI & JADWAL HARIAN
        item {
            Text(
                text = "Integrasi Kalender & Agenda",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Kalender Mini - Slider Tanggal Hari Ini & Sekitar nya (7 hari ke depan)
            val calendarDays = remember {
                val list = mutableListOf<CalendarDate>()
                val cal = Calendar.getInstance()
                for (i in 0..6) {
                    if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                    list.add(
                        CalendarDate(
                            timestamp = cal.timeInMillis,
                            dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale("id", "ID")) ?: "",
                            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
                        )
                    )
                }
                list
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(calendarDays) { date ->
                    val isSelected = isSameDay(date.timestamp, selectedDate)
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) RfxRedAccent else {
                                    if (isDarkMode) CoreDarkSurface else CoreLightSurface
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) RfxRedAccent else (if (isDarkMode) CoreDarkBorder else CoreLightBorder),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.selectCalendarDate(date.timestamp)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.dayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else (if (isDarkMode) Zinc400 else Zinc600),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = date.dayOfMonth,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (filteredSchedules.isNotEmpty()) {
            items(filteredSchedules) { agenda ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
                        .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Alarm, contentDescription = null, tint = RfxRedAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatTimeOnly(agenda.dateTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RfxRedAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = agenda.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = agenda.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkMode) Zinc400 else Zinc600
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.deleteSchedule(agenda) }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = RfxRedAccent)
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) CoreDarkSurfaceElevated else CoreLightSurfaceElevated)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = if (isDarkMode) Zinc600 else Zinc400, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada agenda jadwal hari ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkMode) Zinc500 else Zinc700,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(72.dp)) // Padding agar tidak tertutup bottom navigation
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = profileName,
            currentDesc = profileDesc,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newDesc, uri ->
                viewModel.updateProfile(newName, newDesc, uri)
                showEditProfileDialog = false
            },
            isDarkMode = isDarkMode
        )
    }
}

data class CalendarDate(val timestamp: Long, val dayName: String, val dayOfMonth: String)

fun isSameDay(t1: Long, t2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// -------------------------------------------------------------
// 3. SECURE DIARY WRITER & VIEWER (CATATAN HARIAN)
// -------------------------------------------------------------
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CatatanScreen(
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val entries by viewModel.diaryEntries.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMsg by viewModel.syncMessage.collectAsState()

    var showWriteSheet by remember { mutableStateOf(false) }

    // Toast jika ada sinkronisasi pesan
    LaunchedEffect(syncStatusMsg) {
        if (syncStatusMsg != null) {
            Toast.makeText(context, syncStatusMsg, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catatan_screen_tag")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Catatan Kebenaran",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tersimpan luring & disinkronkan ke Turso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Zinc400 else Zinc600
                    )
                }

                // Tambah Catatan Floating-style action
                IconButton(
                    onClick = { showWriteSheet = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RfxRedAccent)
                        .testTag("add_journal_modal_trigger")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tulis Catatan", tint = Color.White)
                }
            }

            // List of Diaries Group
            if (entries.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entries) { entry ->
                        DiaryEntryCard(
                            entry = entry,
                            isDarkMode = isDarkMode,
                            onDelete = { viewModel.deleteDiaryEntry(entry) },
                            onForceSync = { viewModel.triggerTursoManually(entry) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(84.dp)) }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Create,
                            contentDescription = null,
                            tint = if (isDarkMode) Zinc700 else Zinc300,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada catatan hari ini.\nMulai ekspresikan karya rasa Anda.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkMode) Zinc500 else Zinc700,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Sheet Bottom / Dialog Overlay untuk Tulis Catatan Baru
        if (showWriteSheet) {
            WriteDiaryModal(
                onDismiss = { showWriteSheet = false },
                onSave = { title, content, imageUri, mood ->
                    viewModel.addDiaryEntry(title, content, imageUri, mood)
                    showWriteSheet = false
                },
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    isDarkMode: Boolean,
    onDelete: () -> Unit,
    onForceSync: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
            .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tanggal & Status Sync
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestampFull(entry.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkMode) Zinc400 else Zinc600,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Sync badge
                    if (entry.isSynced) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Clouddb Synced", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A), fontSize = 8.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD97706).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFD97706).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .clickable { onForceSync() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Offline / Ketuk Sync", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706), fontSize = 8.sp)
                        }
                    }
                }

                // Mood Indicator icon
                Text(
                    text = when(entry.mood) {
                        "Senang" -> "😊 Senang"
                        "Sedih" -> "😢 Sedih"
                        "Fokus" -> "🔥 Fokus"
                        "Lelah" -> "😴 Lelah"
                        else -> "😐 Netral"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = RfxRedAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) Zinc300 else Zinc700,
                lineHeight = 22.sp
            )

            // Render attachment photo
            if (!entry.imageUri.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                if (entry.imageUri.startsWith("http")) {
                    AsyncImage(
                        model = entry.imageUri,
                        contentDescription = "Lampiran Foto Jurnal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    // Berkas lokal
                    val file = File(entry.imageUri)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto Jurnal Lokal",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = if (isDarkMode) CoreDarkBorder else CoreLightBorder)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Jurnal",
                        tint = RfxRedAccent
                    )
                }
            }
        }
    }
}

@Composable
fun WriteDiaryModal(
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?, String) -> Unit,
    isDarkMode: Boolean
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("Netral") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Tulis Kisah Hari Ini",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input Judul
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Jurnal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RfxRedAccent,
                        focusedLabelColor = RfxRedAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_journal_title")
                )

                // Input Isi
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mulai kreasikan kata...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RfxRedAccent,
                        focusedLabelColor = RfxRedAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("add_journal_content")
                )

                // Indeks Mood
                Text(
                    "Pilih Vibe Terkini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("Netral", "Senang", "Sedih", "Fokus", "Lelah")) { mood ->
                        val active = selectedMood == mood
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) RfxRedAccent else (if (isDarkMode) CoreDarkSurfaceElevated else CoreLightSurfaceElevated))
                                .border(1.dp, if (active) RfxRedAccent else (if (isDarkMode) CoreDarkBorder else CoreLightBorder), RoundedCornerShape(10.dp))
                                .clickable { selectedMood = mood }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
            Text(
                mood,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color.White else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false
            )
                        }
                    }
                }

                // Unggah Media via Google Drive (Lampiran)
                Text(
                    "Unggah Media Foto (Google Drive Backend Sync)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, RfxRedAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RfxRedAccent)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Berkas Foto")
                    }

                    if (selectedImageUri != null) {
                        Text(
                            "Foto Terlampir",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        onSave(title, content, selectedImageUri, selectedMood)
                    } else {
                        Toast.makeText(context, "Judul dan konten tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RfxRedAccent, contentColor = Color.White),
                modifier = Modifier.testTag("save_journal_button")
            ) {
                Text("Simpan & Sinkronkan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) Zinc400 else Zinc700)
            ) {
                Text("Batal")
            }
        }
    )
}

// -------------------------------------------------------------
// 4. SCHEDULE & WEEKLY PROJECT TARGETS (REALTIME NOTIFIKASI)
// -------------------------------------------------------------
@Composable
fun ProyekScreen(
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val targets by viewModel.weeklyTargets.collectAsState()
    
    var showTargetModal by remember { mutableStateOf(false) }
    var showAgendaModal by remember { mutableStateOf(false) }

    val currentWeekNum = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weekly_projects_screen_tag")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Target Proyek",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Target Minggu Ini (W-$currentWeekNum)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Zinc400 else Zinc600
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Buat Agenda Kalender
                    IconButton(
                        onClick = { showAgendaModal = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDarkMode) CoreDarkSurfaceElevated else CoreLightSurfaceElevated)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Buat Agenda", tint = RfxRedAccent)
                    }

                    // Buat Target Proyek
                    IconButton(
                        onClick = { showTargetModal = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(RfxRedAccent)
                            .testTag("add_weekly_project_trigger")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah Target", tint = Color.White)
                    }
                }
            }

            // Target proyek mingguan list
            if (targets.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(targets) { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
                                .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Kategori pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RfxRedAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            target.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RfxRedAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "W-${target.weekNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkMode) Zinc500 else Zinc700,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = target.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textDecoration = if (target.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )

                                Text(
                                    text = target.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkMode) Zinc400 else Zinc600
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Deadline: ${formatTimestampShort(target.dueDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RfxRedAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Selesai checkbox & Hapus
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = target.isCompleted,
                                    onCheckedChange = { viewModel.toggleTargetCompletion(target) },
                                    colors = CheckboxDefaults.colors(checkedColor = RfxRedAccent),
                                    modifier = Modifier.testTag("target_checkbox_${target.id}")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.deleteWeeklyTarget(target) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = RfxRedAccent)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(84.dp)) }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            tint = if (isDarkMode) Zinc700 else Zinc300,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada target minggu ini.\nBuat target untuk mendongkrak performa.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkMode) Zinc500 else Zinc700,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Dialog tambah target mingguan
        if (showTargetModal) {
            AddTargetModal(
                onDismiss = { showTargetModal = false },
                onSave = { title, desc, cat, date ->
                    viewModel.addWeeklyTarget(title, desc, cat, date)
                    showTargetModal = false
                }
            )
        }

        // Dialog tambah agenda/jadwal kalender
        if (showAgendaModal) {
            AddAgendaModal(
                onDismiss = { showAgendaModal = false },
                onSave = { title, desc, datetime ->
                    viewModel.addSchedule(title, desc, datetime)
                    showAgendaModal = false
                }
            )
        }
    }
}

@Composable
fun AddTargetModal(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Kerja") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, yr, mo, dy ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, yr)
                set(Calendar.MONTH, mo)
                set(Calendar.DAY_OF_MONTH, dy)
            }
            selectedDate = cal.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Target Proyek Mingguan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Proyek / Target") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth().testTag("weekly_project_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Singkat") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth().testTag("weekly_project_desc_input")
                )

                // Kategori pemilihan
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kerja", "Pendidikan", "Pribadi").forEach { cat ->
                        val active = category == cat
                        Button(
                            onClick = { category = cat },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) RfxRedAccent else CoreLightBorder,
                                contentColor = if (active) Color.White else Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(cat, fontSize = 10.sp)
                        }
                    }
                }

                // Date Picker Button
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    border = BorderStroke(1.dp, RfxRedAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tentukan Deadline: ${formatTimestampShort(selectedDate)}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) onSave(title, description, category, selectedDate)
                },
                modifier = Modifier.testTag("save_weekly_project_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RfxRedAccent, contentColor = Color.White)
            ) {
                Text("Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Zinc500)) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddAgendaModal(
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val selectedDateTime = remember { Calendar.getInstance() }
    var selectedDateText by remember { mutableStateOf("Atur Tanggal") }
    var selectedTimeText by remember { mutableStateOf("Atur Waktu") }

    val datePicker = DatePickerDialog(
        context,
        { _, yr, mo, dy ->
            selectedDateTime.set(Calendar.YEAR, yr)
            selectedDateTime.set(Calendar.MONTH, mo)
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dy)
            selectedDateText = "$dy/${mo+1}/$yr"
        },
        selectedDateTime.get(Calendar.YEAR),
        selectedDateTime.get(Calendar.MONTH),
        selectedDateTime.get(Calendar.DAY_OF_MONTH)
    )

    val timePicker = TimePickerDialog(
        context,
        { _, hr, min ->
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hr)
            selectedDateTime.set(Calendar.MINUTE, min)
            selectedDateTime.set(Calendar.SECOND, 0)
            selectedTimeText = String.format("%02d:%02d", hr, min)
        },
        selectedDateTime.get(Calendar.HOUR_OF_DAY),
        selectedDateTime.get(Calendar.MINUTE),
        true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Jadwal & Pengingat Notifikasi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Agenda / Kegiatan") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Agenda") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { datePicker.show() },
                        border = BorderStroke(1.dp, RfxRedAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RfxRedAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedDateText, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { timePicker.show() },
                        border = BorderStroke(1.dp, RfxRedAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RfxRedAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedTimeText, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) onSave(title, description, selectedDateTime.timeInMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RfxRedAccent, contentColor = Color.White)
            ) {
                Text("Pasang Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Zinc500)) {
                Text("Batal")
            }
        }
    )
}

// -------------------------------------------------------------
// 5. SETELAN & INSTRUKSI CONFIG SECRETS (SETTINGS FRAME)
// -------------------------------------------------------------
@Composable
fun SetelanScreen(
    viewModel: AppViewModel,
    isDarkMode: Boolean
) {
    val lockEnabled by viewModel.isFingerprintLockEnabled.collectAsState()

    // Ambil nilai simpanan integrasi
    val dbUrlSaved by viewModel.tursoDbUrl.collectAsState()
    val tokenSaved by viewModel.tursoToken.collectAsState()
    val folderIdSaved by viewModel.googleDriveFolderId.collectAsState()
    val oauthTokenSaved by viewModel.googleDriveOAuthToken.collectAsState()

    var dbUrl by remember(dbUrlSaved) { mutableStateOf(dbUrlSaved) }
    var token by remember(tokenSaved) { mutableStateOf(tokenSaved) }
    var folderId by remember(folderIdSaved) { mutableStateOf(folderIdSaved) }
    var oauthToken by remember(oauthTokenSaved) { mutableStateOf(oauthTokenSaved) }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_tag")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Konfigurasi sistem & server keamanan Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) Zinc400 else Zinc600
            )
        }

        // Toggles Group Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
                    .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Mode & Keamanan Lokal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Sidik Jari Lock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sistem Pengunci Sidik Jari", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Minta verifikasi biometrik saat mulai", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Zinc500 else Zinc700)
                        }
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = { viewModel.toggleFingerprintLock(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RfxRedAccent),
                            modifier = Modifier.testTag("fingerprint_lock_toggle")
                        )
                    }

                    Divider(color = if (isDarkMode) CoreDarkBorder else CoreLightBorder)

                    // 2. Mode Gelap Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mode Tema Visual", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(if (isDarkMode) "Cinematic Dark Mode (Aktif)" else "Classic Day Light (Aktif)", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Zinc500 else Zinc700)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RfxRedAccent),
                            modifier = Modifier.testTag("theme_mode_toggle")
                        )
                    }
                }
            }
        }

        // Instructions details on Cloud Databases & Drive Configurations
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkMode) CoreDarkSurface else CoreLightSurface)
                    .border(1.dp, if (isDarkMode) CoreDarkBorder else CoreLightBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Integrasi Cloud & Driver API",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RfxRedAccent
                    )

                    Text(
                        text = "Aplikasi Anda dikonfigurasi menggunakan file rahasia (.env) di AI Studio Google Cloud. Anda juga dapat memasukkan properti credentials kustom di bawah ini untuk mengaktifkan sinkronisasi awan Turso dan Google Drive API penuh secara realtime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Zinc300 else Zinc700,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = dbUrl,
                        onValueChange = { dbUrl = it },
                        label = { Text("URL Database Turso") },
                        placeholder = { Text("https://your-database.turso.io") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                        modifier = Modifier.fillMaxWidth().testTag("settings_turso_db_url_input")
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Auth Token Turso") },
                        placeholder = { Text("your_turso_auth_token") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                        modifier = Modifier.fillMaxWidth().testTag("settings_turso_token_input")
                    )

                    OutlinedTextField(
                        value = folderId,
                        onValueChange = { folderId = it },
                        label = { Text("ID Folder Google Drive") },
                        placeholder = { Text("your_google_drive_folder_id") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                        modifier = Modifier.fillMaxWidth().testTag("settings_drive_folder_id_input")
                    )

                    OutlinedTextField(
                        value = oauthToken,
                        onValueChange = { oauthToken = it },
                        label = { Text("OAuth Access Token Google Drive") },
                        placeholder = { Text("Bearer atau OAuth2 access token") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RfxRedAccent, focusedLabelColor = RfxRedAccent),
                        modifier = Modifier.fillMaxWidth().testTag("settings_drive_oauth_token_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.updateIntegrationSettings(dbUrl, token, folderId, oauthToken)
                            Toast.makeText(context, "Sertifikasi Integrasi Cloud Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RfxRedAccent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("settings_save_integrations_button")
                    ) {
                        Text("Simpan Konfigurasi Integrasi")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = if (isDarkMode) CoreDarkBorder else CoreLightBorder)

                    // Kredensial Status
                    Text(
                        "Status Setup:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BulletText("1. Sinkronisasi Turso: Menggunakan query raw pipeline.")
                    BulletText("2. Google Drive Multipart API: Menyimpan aset foto lampiran otomatis.")

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RfxRedAccent.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Keamanan: Kode APK dienkripsi dengan compiler obfuscator. Tidak menyimpan mentah-mentah API keys di client terbuka.",
                            style = MaterialTheme.typography.labelSmall,
                            color = RfxRedAccent
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
fun BulletText(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(5.dp).background(RfxRedAccent, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Zinc500)
    }
}

// -------------------------------------------------------------
// SYSTEM BIOMETRIC IMPLEMENTATION CONTROLLER
// -------------------------------------------------------------
fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val biometricPrompt = androidx.biometric.BiometricPrompt(
        activity,
        executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Jika user cancel atau tidak diset, kirim string
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Pindai gagal. Sidik jari tidak dikenali.")
            }
        }
    )

    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Keamanan Biometrik Jurnal")
        .setSubtitle("Sentuh sensor sidik jari perangkat untuk masuk")
        .setNegativeButtonText("Batalkan")
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError("Perangkat tidak bersetifikasi atau sidik jari belum terdaftar.")
    }
}
