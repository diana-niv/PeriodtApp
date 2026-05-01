package com.example.periodtapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.periodtapp.db.prettyName
import com.example.periodtapp.ui.ViewModels.CalendarDayType
import com.example.periodtapp.ui.ViewModels.CalendarViewModel
import com.example.periodtapp.ui.theme.PeriodRed
import com.example.periodtapp.ui.theme.Red700
import com.example.periodtapp.ui.theme.Rose
import com.example.periodtapp.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.ZoneId

// COLORS MATCHING THE IMAGE
private val DayBackground = Color(0xFFF2E6E1)   // The beige/peach color for normal days
private val TextDark = Color(0xFF4A4A4A)
private val OffWhite = Color(0xFFFDF7F5)        // Main screen background

// Dot Colors for Popup
private val FlowColor = Color(0xFF7C0F0F)
private val SpottingColor = Color(0xFFA1887F)
private val DischargeColor = Color(0xFF90CAF9)
private val CollectionColor = Color(0xFFBA68C8)
private val MoodColor = Color(0xFF7986CB)
private val SymptomColor = Color(0xFFA5D6A7)
private val ActivityColor = Color(0xFFFFD54F)
private val CravingColor = Color(0xFFFFB74D)
private val SexColor = Color(0xFFF48FB1)
private val EnergyColor = Color(0xFF80CBC4)

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToEditDates: (cycleId: Int?, dateInMillis: Long) -> Unit,
    onNavigateToLog: (LocalDate) -> Unit
) {

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // STATE
    val periodDays by viewModel.periodDays.collectAsState()
    val calendarDays by viewModel.calendarDays.collectAsState() // Observe combined actual/predicted data
    val selectedDate by viewModel.selectedDate.collectAsState()
    val logDataMap by viewModel.logDataMap.collectAsState()

    val selectedCycleId by viewModel.selectedCycleId.collectAsState()

    // Log Details
    val selectedLog by viewModel.selectedLog.collectAsState()
    val cycleDayNum by viewModel.cycleDayNum.collectAsState()

    // SCROLL STATE
    val currentMonth = YearMonth.now()
    val startMonth = currentMonth.minusMonths(24)
    val totalMonths = 48
    val todayMonthIndex = 24 // The index for the current month

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = todayMonthIndex)
    val coroutineScope = rememberCoroutineScope()

    // Show button if the "today" month is not visible
    val showButton by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.none { it.index == todayMonthIndex }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhite)
        ) {
            //  CONTINUOUS LIST OF MONTHS
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp) // Increased padding for the button
            ) {
                items(totalMonths) { index ->
                    val monthForPage = startMonth.plusMonths(index.toLong())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = monthForPage.format(DateTimeFormatter.ofPattern("MMMM")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 24.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark.copy(alpha = 0.6f)
                                )
                            }
                        }

                        SimpleCalendarGrid(
                            yearMonth = monthForPage,
                            calendarDays = calendarDays,
                            periodDays = periodDays,
                            logDataMap = logDataMap,
                            onDateClick = { date -> viewModel.onDateSelected(date) }
                        )
                    }
                }
            }
        }

        // POPUP DIALOG
        if (selectedDate != null) {
            Dialog(onDismissRequest = { viewModel.onPopupDismissed() }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()), // Added Scroll here
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { viewModel.onPopupDismissed() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Text("✕", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Logged on ${selectedDate!!.format(DateTimeFormatter.ofPattern("d MMM"))}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        if (cycleDayNum != null) {
                            Text(
                                text = "Cycle day $cycleDayNum",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )
                        } else {
                            Spacer(Modifier.height(24.dp))
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val flowText = selectedLog?.log?.flow?.prettyName() ?: "None"
                            LogDetailRow("Flow", flowText, FlowColor)

                            selectedLog?.log?.spotting?.prettyName()?.let { LogDetailRow("Spotting", it, SpottingColor) }
                            selectedLog?.log?.discharge?.prettyName()?.let { if (it != "None") LogDetailRow("Discharge", it, DischargeColor) }
                            selectedLog?.log?.collectionMethod?.prettyName()?.let { LogDetailRow("Collection", it, CollectionColor) }

                            val moodList = selectedLog?.moods?.joinToString { it.moodType.prettyName() }
                            if (!moodList.isNullOrEmpty()) LogDetailRow("Mood", moodList, MoodColor)

                            val symptomList = selectedLog?.symptoms?.joinToString { it.symptomType.prettyName() }
                            if (!symptomList.isNullOrEmpty()) LogDetailRow("Symptoms", symptomList, SymptomColor)

                            val cravingList = selectedLog?.cravings?.joinToString { it.cravingType.prettyName() }
                            if (!cravingList.isNullOrEmpty()) LogDetailRow("Cravings", cravingList, CravingColor)

                            val activityList = selectedLog?.activities?.joinToString { it.activityType.prettyName() }
                            if (!activityList.isNullOrEmpty()) LogDetailRow("Activity", activityList, ActivityColor)

                            val sexList = selectedLog?.sex?.joinToString { it.sexType.prettyName() }
                            if (!sexList.isNullOrEmpty()) LogDetailRow("Sex", sexList, SexColor)

                            val energyList = selectedLog?.energy?.joinToString { it.energyType.prettyName() }
                            if (!energyList.isNullOrEmpty()) LogDetailRow("Energy", energyList, EnergyColor)
                        }

                        Spacer(modifier = Modifier.height(32.dp))


                        val periodBtnText = if (selectedCycleId != null) "Edit period dates" else "Start period here"
                        Button(
                            onClick = {
                                val dateToEdit = selectedDate!!
                                viewModel.onPopupDismissed()
                                val dateMillis = dateToEdit.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                onNavigateToEditDates(selectedCycleId, dateMillis)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(periodBtnText, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val dateToEdit = selectedDate!!
                                viewModel.onPopupDismissed()
                                onNavigateToLog(dateToEdit)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Edit symptoms", fontWeight = FontWeight.Bold)
                        }

                        if (selectedCycleId != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = White,
                                    containerColor = Red700
                                )
                            ) {
                                Text(
                                    text = "Delete Period",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(text = "Delete Period?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this period? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedCycleId?.let { viewModel.deleteCycle(it) }
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red700)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // GO TO TODAY BUTTON
        AnimatedVisibility(
            visible = showButton,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp), // Position above the nav bar
            enter = slideInVertically(initialOffsetY = { it + 50 }),
            exit = slideOutVertically(targetOffsetY = { it + 50 })
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(todayMonthIndex)
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Rose),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Go to Today", color = White, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun LogDetailRow(label: String, value: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, modifier = Modifier.padding(end = 8.dp))
            Box(Modifier.size(12.dp).background(dotColor, CircleShape))
        }
    }
}

@Composable
fun SimpleCalendarGrid(    yearMonth: YearMonth,
                           calendarDays: Map<LocalDate, CalendarDayType>,
                           periodDays: Map<LocalDate, Boolean>,
                           logDataMap: Map<LocalDate, com.example.periodtapp.db.PopulatedDailyLog>,
                           onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val totalCells = daysInMonth + (firstDayOfWeek - 1)
    val rows = (totalCells / 7) + if (totalCells % 7 != 0) 1 else 0

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = (row * 7) + col
                    val day = index - (firstDayOfWeek - 1) + 1

                    if (day in 1..daysInMonth) {
                        val date = yearMonth.atDay(day)
                        val isToday = date == LocalDate.now()
                        val logEntry = logDataMap[date]
                        val dayType = calendarDays[date] ?: com.example.periodtapp.ui.ViewModels.CalendarDayType.NONE

                        val boxColor = when (dayType) {
                            CalendarDayType.ACTUAL_PERIOD -> Rose
                            CalendarDayType.PREDICTED_PERIOD -> Rose.copy(alpha = 0.3f)

                            // --- THIS IS THE NEW BLUE LOGIC ---
                            CalendarDayType.PREDICTED_OVULATION -> Color(0x6F4682B4) // Steel Blue
                            CalendarDayType.PREDICTED_FERTILE -> Color(0xB74682B4).copy(alpha = 0.2f) // Very Light Blue

                            // --- PAST/HISTORICAL (Darker) ---
                            CalendarDayType.PAST_OVULATION -> Color(0xFF2C5272) // Deep Steel Blue
                            CalendarDayType.PAST_FERTILE -> Color(0xFF4682B4).copy(alpha = 0.35f)

                            else -> if (isToday) DayBackground else Color.Transparent
                        }

                        val textColor = if (dayType == CalendarDayType.ACTUAL_PERIOD ||
                            dayType == CalendarDayType.PAST_OVULATION) {
                            Color.White
                        } else {
                            TextDark
                        }

                        val fontWeight = if (isToday ||
                            dayType == CalendarDayType.ACTUAL_PERIOD ||
                            dayType == CalendarDayType.PAST_OVULATION ||
                            dayType == CalendarDayType.PREDICTED_OVULATION) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(boxColor)
                                .clickable { onDateClick(date) }
                        ) {
                            // Container for Number (Center) and Dots (Bottom Left)
                            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                // Day Number in the Center
                                Text(
                                    text = day.toString(),
                                    color = textColor,
                                    fontWeight = fontWeight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                // SYMPTOM DOTS (Bottom Left)
                                val hasFlow = logEntry?.log?.flow != null && logEntry.log.flow != com.example.periodtapp.db.FlowType.NO_PERIOD
                                val hasMoods = !logEntry?.moods.isNullOrEmpty()
                                val hasSymptoms = !logEntry?.symptoms.isNullOrEmpty()
                                val hasCravings = !logEntry?.cravings.isNullOrEmpty()
                                val hasActivities = !logEntry?.activities.isNullOrEmpty()
                                val hasSex = !logEntry?.sex.isNullOrEmpty()
                                val hasEnergy = !logEntry?.energy.isNullOrEmpty()


                                if (hasFlow || hasMoods || hasSymptoms || hasCravings || hasActivities || hasSex || hasEnergy) {
                                    // CATEGORY DOTS (In order of Enum/Popup)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Flow (Exclude NO_PERIOD)
                                        if (logEntry?.log?.flow != null && logEntry.log.flow != com.example.periodtapp.db.FlowType.NO_PERIOD) {
                                            Dot(FlowColor)
                                        }
                                        // 2. Spotting
                                        if (logEntry?.log?.spotting != null) {
                                            Dot(SpottingColor)
                                        }
                                        // 3. Discharge (Exclude NONE)
                                        if (logEntry?.log?.discharge != null && logEntry.log.discharge != com.example.periodtapp.db.DischargeType.NONE) {
                                            Dot(DischargeColor)
                                        }
                                        // 4. Collection Method
                                        if (logEntry?.log?.collectionMethod != null) {
                                            Dot(CollectionColor)
                                        }
                                        // 5. Moods
                                        if (!logEntry?.moods.isNullOrEmpty()) {
                                            Dot(MoodColor)
                                        }
                                        // 6. Symptoms
                                        if (!logEntry?.symptoms.isNullOrEmpty()) {
                                            Dot(SymptomColor)
                                        }
                                        // 7. Activity
                                        if (!logEntry?.activities.isNullOrEmpty()) {
                                            Dot(ActivityColor)
                                        }
                                        // 8. Cravings
                                        if (!logEntry?.cravings.isNullOrEmpty()) {
                                            Dot(CravingColor)
                                        }
                                        // 10. Sex
                                        if (hasSex) {
                                            Dot(SexColor)
                                        }
                                        // 11. Energy
                                        if (hasEnergy) {
                                            Dot(EnergyColor)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape)
    )
}