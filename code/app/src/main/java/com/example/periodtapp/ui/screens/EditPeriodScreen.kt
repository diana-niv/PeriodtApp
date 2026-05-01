package com.example.periodtapp.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.periodtapp.ui.ViewModels.EditPeriodViewModel
import com.example.periodtapp.ui.theme.Red700
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Colors
private val PeriodRed = Color(0xFFD46A6A)
private val SelectionRed = Color(0xFF8B0000) // Darker red for start/end
private val OffWhite = Color(0xFFFDF7F5)
private val TextDark = Color(0xFF4A4A4A)

@Composable
fun EditPeriodScreen(
    viewModel: EditPeriodViewModel,
    cycleId: Int,
    initialDateMillis: Long,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    // 1. Load Data ONCE when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadCycle(cycleId, initialDateMillis)
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Scroll State Setup
    val initialDate = remember {
        Instant.ofEpochMilli(initialDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val startMonth = remember { YearMonth.now().minusMonths(24) }
    val initialMonth = remember { YearMonth.from(initialDate) }

    val monthsDifference = remember {
        ChronoUnit.MONTHS.between(startMonth, initialMonth)
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = monthsDifference.toInt().coerceAtLeast(0)
    )

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(OffWhite)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Period Dates", style = MaterialTheme.typography.headlineSmall, color = TextDark)
            if (cycleId != -1) {
                Button(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Red700)
                ) {
                    Text("Delete")
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(text = "Delete Period?", fontWeight = FontWeight.Bold, color = Color.Black) },
                text = { Text("Are you sure you want to delete this period? This action cannot be undone.", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCycle(cycleId, onSave)
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

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            items(48) { index ->
                val month = startMonth.plusMonths(index.toLong())

                Column(Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
                        color = TextDark
                    )

                    SelectionCalendarGrid(
                        yearMonth = month,
                        startDate = startDate,
                        endDate = endDate,
                        onDateClick = { viewModel.onDateClicked(it) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.saveCycle(cycleId, onSave) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PeriodRed)
            ) { Text("Save") }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel", color = TextDark) }
        }
    }
}

@Composable
fun SelectionCalendarGrid(
    yearMonth: YearMonth,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val totalCells = daysInMonth + (firstDayOfWeek - 1)
    val rows = (totalCells / 7) + if (totalCells % 7 != 0) 1 else 0

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = (row * 7) + col
                    val day = index - (firstDayOfWeek - 1) + 1

                    if (day in 1..daysInMonth) {
                        val date = yearMonth.atDay(day)

                        val isStart = date == startDate
                        val isEnd = date == endDate
                        val isInRange = if (startDate != null && endDate != null) {
                            date.isAfter(startDate) && date.isBefore(endDate)
                        } else false

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    color = when {
                                        isStart || isEnd -> SelectionRed
                                        isInRange -> PeriodRed
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onDateClick(date) }
                        ) {
                            Text(
                                text = day.toString(),
                                color = if (isStart || isEnd || isInRange) Color.White else TextDark,
                                fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
