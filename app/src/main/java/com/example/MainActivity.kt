package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.firebase.FirebaseManager
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase helper
        FirebaseManager.initialize(this)

        // Request notifications permissions on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            MyApplicationTheme {
                PeriodCareApp()
            }
        }
    }
}

sealed class AppRoute {
    object Splash : AppRoute()
    object Auth : AppRoute()
    object Main : AppRoute()
    object Notifications : AppRoute()
    object AdminPanel : AppRoute()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodCareApp() {
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Splash) }
    var currentTab by remember { mutableStateOf(0) } // 0: Tracker, 1: Yoga, 2: History, 3: Support

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Crossfade(targetState = currentRoute, label = "ScreenTransition") { route ->
            when (route) {
                AppRoute.Splash -> {
                    SplashScreen { isAlreadyLoggedIn ->
                        currentRoute = if (isAlreadyLoggedIn) AppRoute.Main else AppRoute.Auth
                    }
                }
                
                AppRoute.Auth -> {
                    AuthScreen(
                        onAuthSuccess = {
                            currentRoute = AppRoute.Main
                        },
                        onAdminUnlocked = {
                            currentRoute = AppRoute.AdminPanel
                        }
                    )
                }

                AppRoute.Main -> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Period Care",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                actions = {
                                    // Notification Center bell
                                    IconButton(
                                        onClick = { currentRoute = AppRoute.Notifications }
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                // Dynamic unread dot (shows up because we pre-seed mock announcements)
                                                Badge { Text("2") }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Notification Center Bell",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Secret Direct Admin Link in header if clicked
                                    IconButton(
                                        onClick = { currentRoute = AppRoute.AdminPanel }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = "Quick Admin Panel Link",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    label = { Text("Tracker") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 0) Icons.Default.Analytics else Icons.Outlined.Analytics,
                                            contentDescription = "Tracker dashboard"
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    label = { Text("Yoga") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 1) Icons.Default.SelfImprovement else Icons.Outlined.SelfImprovement,
                                            contentDescription = "Yoga comfortable relief"
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    label = { Text("Logs") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 2) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                                            contentDescription = "Biological Log Book"
                                        )
                                    }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    label = { Text("Support") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == 3) Icons.Default.Person else Icons.Outlined.Person,
                                            contentDescription = "Support and profile"
                                        )
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                0 -> DashboardScreen()
                                1 -> YogaScreen()
                                2 -> HistoryScreen()
                                3 -> SupportScreen(
                                    onLogout = {
                                        currentRoute = AppRoute.Auth
                                        currentTab = 0
                                    }
                                )
                            }
                        }
                    }
                }

                AppRoute.Notifications -> {
                    NotificationScreen(
                        onBack = { currentRoute = AppRoute.Main }
                    )
                }

                AppRoute.AdminPanel -> {
                    AdminScreen(
                        onClose = {
                            currentRoute = if (FirebaseManager.currentUserFlow.value != null) AppRoute.Main else AppRoute.Auth
                        }
                    )
                }
            }
        }
    }
}
