package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(viewModel: AppViewModel, isDarkMode: Boolean) {
    // 2 hours max -> 7200 seconds. Let's assume we read from viewModel or simple local state for demo.
    // Time tracking state
    var timeRemaining by remember { mutableStateOf(7200) } // in seconds, 2 hours
    var isPlaying by remember { mutableStateOf(false) }

    // Countdown effect when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && timeRemaining > 0) {
            delay(1000)
            timeRemaining -= 1
            if (timeRemaining <= 0) {
                isPlaying = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Area Hiburan",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Relaksasi dibatasi 2 jam/hari",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) Zinc400 else Zinc600
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (timeRemaining > 0) RfxRedAccent else Zinc600)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val h = timeRemaining / 3600
                val m = (timeRemaining % 3600) / 60
                val s = timeRemaining % 60
                Text(
                    text = String.format("%02d:%02d:%02d", h, m, s),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (timeRemaining <= 0) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(64.dp), tint = Zinc500)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Batas waktu hiburan (2 jam) telah habis. Kembalilah bekerja dan berkembang esok hari!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            PetGame(
                isDarkMode = isDarkMode,
                onGameStart = { isPlaying = true }
            )
        }
    }
}

// Model & Logic for Pet Game
@Composable
fun PetGame(isDarkMode: Boolean, onGameStart: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("rfx_pet_prefs", android.content.Context.MODE_PRIVATE)

    var petAgeStage by remember { mutableStateOf(prefs.getInt("stage", 0)) } // 0: Baby, 1: Child, 2: Adult
    var hunger by remember { mutableStateOf(prefs.getInt("hunger", 50)) }
    var cleanliness by remember { mutableStateOf(prefs.getInt("cleanliness", 50)) }
    var happiness by remember { mutableStateOf(prefs.getInt("happiness", 50)) }
    var energy by remember { mutableStateOf(prefs.getInt("energy", 50)) }
    var hasPoop by remember { mutableStateOf(prefs.getBoolean("poop", false)) }
    var playCount by remember { mutableStateOf(prefs.getInt("playCount", 0)) }
    var isSleeping by remember { mutableStateOf(prefs.getBoolean("sleeping", false)) }

    val coroutineScope = rememberCoroutineScope()
    // Simpan ke SharedPreferences setiap ada perubahan
    LaunchedEffect(petAgeStage, hunger, cleanliness, happiness, energy, hasPoop, playCount, isSleeping) {
        prefs.edit().apply {
            putInt("stage", petAgeStage)
            putInt("hunger", hunger)
            putInt("cleanliness", cleanliness)
            putInt("happiness", happiness)
            putInt("energy", energy)
            putBoolean("poop", hasPoop)
            putInt("playCount", playCount)
            putBoolean("sleeping", isSleeping)
            apply()
        }
        // Sinkronisasi background ke Turso
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.SyncEngine.syncPetToTurso(
               context, petAgeStage, hunger, cleanliness, happiness, energy, hasPoop, playCount
            )
        }
    }

    LaunchedEffect(Unit) {
        onGameStart()
        // Kurangi stats perlahan jika dibiarkan (simulasi berjalan)
        while(true) {
            delay(10000) // tiap 10 detik turun sedikit untuk demo
            if (!isSleeping) {
                if (hunger > 0) hunger -= 1
                if (happiness > 0) happiness -= 1
                if (energy > 0) energy -= 1
            }
        }
    }

    // Efek tidur (menambah energi secara bertahap)
    LaunchedEffect(isSleeping) {
        while(isSleeping) {
            delay(5000)
            if (energy < 100) {
                energy = (energy + 10).coerceAtMost(100)
            } else {
                isSleeping = false // otomatis bangun jika energi penuh
            }
        }
    }

    // Auto tumbuh jika sudah sering dimainkan
    LaunchedEffect(playCount) {
        if (playCount > 10 && petAgeStage == 0) petAgeStage = 1
        if (playCount > 25 && petAgeStage == 1) petAgeStage = 2
    }

    val petIcon = when {
        isSleeping -> "💤"
        energy < 20 -> "😴"
        hunger < 30 -> "😿"
        hasPoop -> "😾"
        petAgeStage == 0 -> "🐱"
        petAgeStage == 1 -> "🐈"
        else -> "🦁"
    }

    val petSize = when(petAgeStage) {
        0 -> 64.sp
        1 -> 96.sp
        else -> 128.sp
    }

    // Evaluasi notifikasi berdasarkan status
    val notificationMessage = when {
        isSleeping -> "Ssst... Peliharaanmu sedang tidur pulas."
        hasPoop -> "Wah, ada kotoran! Ayo cepat bersihkan."
        hunger < 30 -> "Meow! Aku sangat lapar, beri aku makan!"
        energy < 30 -> "Hoamm... Aku sangat ngantuk, matikan lampu yuk."
        happiness < 30 -> "Aku bosan, ayo main denganku!"
        else -> "Peliharaanmu tampak sangat senang dan sehat!"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) CoreDarkSurfaceElevated else CoreLightSurfaceElevated),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Dashboard Anabul",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fase: ${if (petAgeStage == 0) "Bayi" else if (petAgeStage == 1) "Anak" else "Dewasa"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Zinc400 else Zinc600
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Status bar yang disesuaikan
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatusColumn("Lapar", hunger, Color(0xFFF59E0B))
                    StatusColumn("Bersih", cleanliness, Color(0xFF3B82F6))
                    StatusColumn("Senang", happiness, Color(0xFF10B981))
                    StatusColumn("Energi", energy, Color(0xFF8B5CF6))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pesan Status (Notifikasi)
        Surface(
            color = if (isDarkMode) Color(0xFF3f3f46).copy(alpha = 0.4f) else Color(0xFFf4f4f5),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = notificationMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pet Area - Gelap jika tidur (Lampu Dimatikan)
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSleeping) Color(0xFF111118) else if(isDarkMode) CoreDarkSurfaceElevated else CoreLightSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (hasPoop && !isSleeping) {
                Text(
                    text = "💩",
                    fontSize = 32.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clickable { hasPoop = false; cleanliness = (cleanliness + 20).coerceAtMost(100) }
                )
            }
            Text(text = petIcon, fontSize = petSize)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            PetActionButton(icon = "🥩", label = "Makan", isEnabled = !isSleeping, onClick = {
                if (!isSleeping) {
                    hunger = (hunger + 25).coerceAtMost(100)
                    if ((0..10).random() > 7) hasPoop = true
                    playCount++
                }
            })
            PetActionButton(icon = "🚿", label = "Mandi", isEnabled = !isSleeping, onClick = {
                if (!isSleeping) {
                    cleanliness = (cleanliness + 30).coerceAtMost(100)
                    hasPoop = false
                    playCount++
                }
            })
            PetActionButton(icon = "⚽", label = "Main", isEnabled = !isSleeping, onClick = {
                if (!isSleeping) {
                    happiness = (happiness + 20).coerceAtMost(100)
                    energy = (energy - 10).coerceAtLeast(0)
                    hunger = (hunger - 10).coerceAtLeast(0)
                    playCount++
                }
            })
            PetActionButton(icon = "💡", label = if (isSleeping) "Bangun" else "Tidur", isEnabled = true, onClick = {
                if (!isSleeping) {
                    isSleeping = true
                } else {
                    isSleeping = false // Membangunkan sebelum waktunya
                }
                playCount++
            })
        }
    }
}

@Composable
fun StatusColumn(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .width(60.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun PetActionButton(icon: String, label: String, isEnabled: Boolean = true, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(8.dp)
            .background(if (isEnabled) Color.Transparent else Color.Black.copy(alpha = 0.1f))
    ) {
        Text(text = icon, fontSize = 32.sp, modifier = Modifier.alpha(if (isEnabled) 1f else 0.5f))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = if (isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}
