package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isLocked by viewModel.isAppLocked.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                if (isLocked) {
                    LockScreen(
                        onUnlockSuccess = { viewModel.unlockApp() },
                        onBypassUnlock = { viewModel.unlockApp() },
                        isDarkMode = isDarkMode
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("rfx_bottom_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = NavigationBarDefaults.Elevation
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "dashboard",
                                    onClick = { viewModel.changeTab("dashboard") },
                                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") },
                                    modifier = Modifier.testTag("nav_dashboard")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "catatan",
                                    onClick = { viewModel.changeTab("catatan") },
                                    icon = { Icon(Icons.Filled.Book, contentDescription = "Catatan") },
                                    label = { Text("Catatan") },
                                    modifier = Modifier.testTag("nav_catatan")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "proyek",
                                    onClick = { viewModel.changeTab("proyek") },
                                    icon = { Icon(Icons.Filled.Assignment, contentDescription = "Proyek") },
                                    label = { Text("Proyek") },
                                    modifier = Modifier.testTag("nav_proyek")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "setelan",
                                    onClick = { viewModel.changeTab("setelan") },
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Setelan") },
                                    label = { Text("Setelan") },
                                    modifier = Modifier.testTag("nav_setelan")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                "dashboard" -> DashboardScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                "catatan" -> CatatanScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                "proyek" -> ProyekScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                                "setelan" -> SetelanScreen(viewModel = viewModel, isDarkMode = isDarkMode)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Kunci kembali aplikasi demi proteksi keamanan tinggi saat diminimalisasi
        viewModel.lockApp()
    }
}
