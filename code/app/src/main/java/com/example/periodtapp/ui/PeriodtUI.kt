package com.example.periodtapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.periodtapp.R
import com.example.periodtapp.ui.ViewModels.CalendarViewModel
import com.example.periodtapp.ui.ViewModels.EditPeriodViewModel
import com.example.periodtapp.ui.ViewModels.LogViewModel
import com.example.periodtapp.ui.navigation.Screen
import com.example.periodtapp.ui.screens.*
import java.time.LocalDate
import androidx.compose.runtime.collectAsState
import com.example.periodtapp.ui.theme.PeriodRed
import com.example.periodtapp.ui.theme.Rose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodtApp() {
    val navController = rememberNavController()
    var globalSelectedDate by remember { mutableStateOf(LocalDate.now()) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBars = currentRoute != "splash"

    Scaffold(
        topBar = {
            if (shouldShowBars) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.periodt_logo),
                                contentDescription = "Periodt Logo",
                                modifier = Modifier.size(45.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Periodt.",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = White,
                        titleContentColor = Color(0xFF37323E)
                    ),
                    actions = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                val items = listOf(Screen.Home, Screen.LogSymptoms, Screen.Calendar)
                NavigationBar(
                    containerColor = White
                ) {
                    items.forEach { screen ->
                        val baseRoute = screen.route.substringBefore("/")
                        val isSelected = navBackStackEntry?.destination?.route?.startsWith(baseRoute) == true

                        val iconRes = when (screen) {
                            Screen.Home -> R.drawable.home
                            Screen.LogSymptoms -> R.drawable.plus_log_symptoms
                            Screen.Calendar -> R.drawable.calendar
                            else -> R.drawable.home
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val route = if (screen == Screen.LogSymptoms) "log/$globalSelectedDate" else screen.route.substringBefore("?")
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Rose else Color.Transparent)
                            ) {
                                val tint = if (isSelected) White else Rose

                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = tint
                                )
                            }
                            Text(
                                text = screen.title,
                                color = if (isSelected) Rose else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Splash.route, Modifier.padding(padding)) {
            composable("profile") {
                val calendarViewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val prediction by calendarViewModel.prediction.collectAsState()
                ProfilePage(prediction = prediction)
            }

            composable(Screen.Splash.route) {
                SplashScreen {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            composable(Screen.Home.route) {

                val calendarViewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val currentCycleDay by calendarViewModel.cycleDayNum.collectAsState()
                val todayCycleDay by calendarViewModel.cycleDayNumToday.collectAsState()
                val prediction by calendarViewModel.prediction.collectAsState()
                val calendarPeriodDays by calendarViewModel.calendarDays.collectAsState()
                val selectedCycle by calendarViewModel.selectedCycle.collectAsState()
                val logMap by calendarViewModel.logDataMap.collectAsState()

                val calendarDays by calendarViewModel.calendarDays.collectAsState()
                val cycles by calendarViewModel.cycles.collectAsState(initial = emptyList())

                val cycleLength = selectedCycle?.cycleLength ?: prediction?.averageCycleLength ?: 28
                val periodLength = selectedCycle?.periodLength ?: prediction?.averagePeriodLength ?: 5

                calendarViewModel.onDateSelected(globalSelectedDate)

                HomePage(
                    selectedDate = globalSelectedDate,
                    currentCycleDay = currentCycleDay ?: cycleLength,
                    todayCycleDay = todayCycleDay,
                    prediction = prediction,
                    calendarDays = calendarPeriodDays,
                    cycles = cycles,
                    onDateChange = {
                        globalSelectedDate = it
                        calendarViewModel.onDateSelected(it)
                    },
                    logMap = calendarViewModel.logDataMap.collectAsState().value,
                    onNavigateToLog = { navController.navigate("log/$globalSelectedDate") },
                    cycleLength = cycleLength,
                    periodLength = periodLength,
                    onNavigateToEditCycle = { cycleId, dateMillis ->
                        navController.navigate("edit_period/$cycleId/$dateMillis")
                    },
                    onNavigateToCalendarPopup = { date ->
                        navController.navigate("${Screen.Calendar.route}?date=$date")
                    }
                )
            }

            composable(
                route = "${Screen.Calendar.route}?date={date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val calendarViewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val dateStr = backStackEntry.arguments?.getString("date")
                LaunchedEffect(dateStr) {
                    if (dateStr != null) {
                        val date = LocalDate.parse(dateStr)
                        calendarViewModel.onDateSelected(date)
                    }
                }

                CalendarScreen(
                    viewModel = calendarViewModel,
                    onNavigateToEditDates = { cycleId, dateInMillis ->
                        val idToSend = cycleId ?: -1
                        navController.navigate("edit_period/$idToSend/$dateInMillis")
                    },
                    onNavigateToLog = { date -> navController.navigate("log/$date") }
                )
            }

            composable(
                route = "log/{selectedDate}",
                arguments = listOf(navArgument("selectedDate") { type = NavType.StringType })
            ) { backStackEntry ->
                val calendarViewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
                val dateStr = backStackEntry.arguments?.getString("selectedDate") ?: LocalDate.now().toString()
                val date = LocalDate.parse(dateStr)
                val calendarDays by calendarViewModel.calendarDays.collectAsState()
                val logMap by calendarViewModel.logDataMap.collectAsState()

                val logViewModel: LogViewModel = viewModel(factory = AppViewModelProvider.Factory)

                LogSymptomsScreen(
                    viewModel = logViewModel,
                    date = date,
                    calendarDays = calendarDays,
                    onDateChange = { globalSelectedDate = it },
                    logDataMap = logMap,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_period/{cycleId}/{date}",
                arguments = listOf(
                    navArgument("cycleId") { type = NavType.IntType },
                    navArgument("date") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val cycleId = backStackEntry.arguments?.getInt("cycleId") ?: -1
                val dateLong = backStackEntry.arguments?.getLong("date") ?: 0L

                val editViewModel: EditPeriodViewModel = viewModel(factory = AppViewModelProvider.Factory)

                EditPeriodScreen(
                    viewModel = editViewModel,
                    cycleId = cycleId,
                    initialDateMillis = dateLong,
                    onSave = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}