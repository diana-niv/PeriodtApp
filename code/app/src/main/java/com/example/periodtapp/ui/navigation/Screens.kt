package com.example.periodtapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home")
    object LogSymptoms : Screen("log/{selectedDate}", "Log Symptoms")
    object Calendar : Screen("calendar", "Calendar")
    object Profile : Screen("profile", "Profile")
}

val bottomNavItems = listOf(Screen.Home, Screen.LogSymptoms, Screen.Calendar)
