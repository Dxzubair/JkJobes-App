package com.jkjobs.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jkjobs.app.notif.Notifier
import com.jkjobs.app.ui.admitcards.AdmitCardsScreen
import com.jkjobs.app.ui.feed.FeedScreen
import com.jkjobs.app.ui.home.HomeScreen
import com.jkjobs.app.ui.results.ResultsScreen
import com.jkjobs.app.ui.saved.SavedScreen
import com.jkjobs.app.ui.settings.SettingsScreen
import com.jkjobs.app.ui.theme.JKDarkColorScheme
import com.jkjobs.app.ui.theme.JKLightColorScheme
import com.jkjobs.app.worker.JobCheckWorker

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* If denied, the app still works - the user just won't get push alerts until they
           grant it later from system Settings. We don't nag; respecting the choice. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifier.ensureChannel(this)
        JobCheckWorker.schedule(this) // starts periodic background checks (default every 3h)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            // Fixed JKJobs+ brand purple theme app-wide (no Material You dynamic color),
            // so the app looks consistent across every device regardless of wallpaper.
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (isDark) JKDarkColorScheme else JKLightColorScheme) {
                AppRoot()
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("feed", "Jobs", Icons.AutoMirrored.Filled.List),
    Tab("admitCards", "Admit Cards", Icons.Filled.CreditCard),
    Tab("results", "Results", Icons.Filled.Grade),
    Tab("settings", "Settings", Icons.Filled.Settings)
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onGoToJobs = { navController.navigate("feed") { launchSingleTop = true } },
                    onGoToSaved = { navController.navigate("saved") { launchSingleTop = true } },
                    onGoToSettings = { navController.navigate("settings") { launchSingleTop = true } }
                )
            }
            composable("feed") { FeedScreen() }
            composable("admitCards") { AdmitCardsScreen() }
            composable("results") { ResultsScreen() }
            composable("saved") { SavedScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
