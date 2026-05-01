package com.example.periodtapp.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.periodtapp.db.PopulatedDailyLog
import com.example.periodtapp.ui.ViewModels.CalendarDayType
import com.example.periodtapp.ui.theme.PeriodRed
import com.example.periodtapp.ui.theme.Rose
import com.example.periodtapp.ui.theme.White
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarStrip(
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, CalendarDayType> = emptyMap(),
    logMap: Map<LocalDate, PopulatedDailyLog> = emptyMap(),
    onDateSelected: (LocalDate) -> Unit,
    onEditPeriod: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    // Using a fixed anchor allows infinite scrolling in both directions
    val anchorDate = LocalDate.of(2000, 1, 1)
    val totalPageCount = 100_000

    val pagerState = rememberPagerState(
        initialPage = ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt(),
        pageCount = { totalPageCount }
    )

    val viewedDate = remember(pagerState.currentPage) {
        anchorDate.plusDays(pagerState.currentPage.toLong())
    }

    fun scrollToDate(date: LocalDate) {
        scope.launch {
            val targetPage = ChronoUnit.DAYS.between(anchorDate, date).toInt()
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync pager if selectedDate changes externally
    LaunchedEffect(selectedDate) {
        val targetPage = ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER (Month & Arrows)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (viewedDate != today) {
                    val isPast = today.isBefore(viewedDate)
                    Button(
                        onClick = {
                            // 1. Select the date
                            onDateSelected(today)

                            scrollToDate(today)
                        },
                        modifier = Modifier
                            .align(if (isPast) Alignment.TopStart else Alignment.TopEnd)
                            .padding(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose,
                            contentColor = White
                        ),
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text("Today", color = White, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scrollToDate(viewedDate.minusMonths(1).withDayOfMonth(1)) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = viewedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF37323E)
                    )
                    IconButton(onClick = { scrollToDate(viewedDate.plusMonths(1).withDayOfMonth(1)) }) {
                        Icon(Icons.Default.KeyboardArrowRight, null, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DAY PAGER
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val horizontalPadding = (screenWidth - 32.dp - 60.dp) / 2

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    // FADE EFFECT
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.White,
                                0.2f to Color.Transparent,
                                0.8f to Color.Transparent,
                                1f to Color.White
                            )
                        )
                    },
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                pageSize = PageSize.Fixed(60.dp),
                beyondViewportPageCount = 5,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
            ) { page ->
                val date = anchorDate.plusDays(page.toLong())
                val logEntry = logMap[date]

                DayItem(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    dayType = dayMap[date] ?: CalendarDayType.NONE,
                    hasFlow = logEntry?.log?.flow != null && logEntry.log.flow != com.example.periodtapp.db.FlowType.NO_PERIOD,
                    hasSpotting = logEntry?.log?.spotting != null,
                    hasDischarge = logEntry?.log?.discharge != null && logEntry.log.discharge != com.example.periodtapp.db.DischargeType.NONE,
                    hasCollection = logEntry?.log?.collectionMethod != null,
                    hasMoods = !logEntry?.moods.isNullOrEmpty(),
                    hasSymptoms = !logEntry?.symptoms.isNullOrEmpty(),
                    hasActivities = !logEntry?.activities.isNullOrEmpty(),
                    hasCravings = !logEntry?.cravings.isNullOrEmpty(),
                    onClick = {
                        onDateSelected(it)
                    }
                )
            }

            if (onEditPeriod != null) {
                Spacer(modifier = Modifier.height(16.dp))

                val isPeriodDay = dayMap[selectedDate] == CalendarDayType.ACTUAL_PERIOD

                Button(
                    onClick = onEditPeriod,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Rose
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (isPeriodDay) "Edit period dates" else "Start period here",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    dayType: CalendarDayType,
    hasFlow: Boolean,
    hasSpotting: Boolean,
    hasDischarge: Boolean,
    hasCollection: Boolean,
    hasMoods: Boolean,
    hasSymptoms: Boolean,
    hasActivities: Boolean,
    hasCravings: Boolean,
    onClick: (LocalDate) -> Unit

) {
    val today = LocalDate.now()
    val isFuture = date.isAfter(today)

    // Calculate highlight color based on the parameter passed from CalendarStrip
    val dayHighlightColor = when (dayType) {
        CalendarDayType.ACTUAL_PERIOD -> PeriodRed
        CalendarDayType.PREDICTED_PERIOD -> PeriodRed.copy(alpha = 0.25f) // Faded Ghost Streak
        // Add this line for the ovulation color (Steel Blue)
        CalendarDayType.PREDICTED_OVULATION -> Color(0xFF4682B4).copy(alpha = 0.2f)
        CalendarDayType.PAST_OVULATION -> Color(0xFF4682B4).copy(alpha = 0.2f)

        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .width(55.dp)
            .background(
                if (isToday) Color(0xFF37323E) else Color.Transparent,
                RoundedCornerShape(28.dp)
            )
            .clickable(enabled = !isFuture) { onClick(date) } // Disable clicks for future dates
            .padding(vertical = 12.dp)
            .graphicsLayer(alpha = if (isFuture && dayType != CalendarDayType.PREDICTED_PERIOD) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            color = if (isToday) Color.White else Color(0xFFBDBDBD),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        PeriodRed
                    } else if (dayType == CalendarDayType.PAST_OVULATION) {
                        Color(0xFF2C5272) // Solid DARK Blue for Past
                    } else if (dayType == CalendarDayType.PREDICTED_OVULATION) {
                        Color(0xFF4682B4) // Solid Steel Blue for Ovulation
                    } else {
                        dayHighlightColor // Fallback to your Red/Ghost highlights
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected || isToday || dayType == CalendarDayType.PAST_OVULATION) {
                    Color.White // White text for selection and dark past blue
                } else {
                    Color(0xFF37323E)
                },
                fontWeight = if (isSelected || isToday ||
                    dayType == CalendarDayType.PAST_OVULATION ||
                    dayType == CalendarDayType.PREDICTED_OVULATION) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LogDots(
            hasFlow = hasFlow,
            hasSpotting = hasSpotting,
            hasDischarge = hasDischarge,
            hasCollection = hasCollection,
            hasMoods = hasMoods,
            hasSymptoms = hasSymptoms,
            hasActivities = hasActivities,
            hasCravings = hasCravings
        )
    }
}

@Composable
fun LogDots(
    hasFlow: Boolean,
    hasSpotting: Boolean,
    hasDischarge: Boolean,
    hasCollection: Boolean,
    hasMoods: Boolean,
    hasSymptoms: Boolean,
    hasActivities: Boolean,
    hasCravings: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasFlow) Dot(Color(0xFFE57373))       // FlowColor
        if (hasSpotting) Dot(Color(0xFFA1887F))   // SpottingColor
        if (hasDischarge) Dot(Color(0xFF90CAF9))  // DischargeColor
        if (hasCollection) Dot(Color(0xFFBA68C8)) // CollectionColor
        if (hasMoods) Dot(Color(0xFF7986CB))      // MoodColor
        if (hasSymptoms) Dot(Color(0xFFA5D6A7))   // SymptomColor
        if (hasActivities) Dot(Color(0xFFFFD54F)) // ActivityColor
        if (hasCravings) Dot(Color(0xFFFFB74D))   // CravingColor
    }
}

@Composable
fun Dot(color: Color) {
    Box(
        Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}