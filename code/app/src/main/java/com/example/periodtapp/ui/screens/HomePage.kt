package com.example.periodtapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.periodtapp.db.CyclePrediction
import com.example.periodtapp.db.CyclesEntity
import com.example.periodtapp.db.PopulatedDailyLog
import com.example.periodtapp.ui.ViewModels.CalendarDayType
import com.example.periodtapp.ui.components.CalendarStrip
import com.example.periodtapp.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*


// --- 1. UPDATED PHASE DESCRIPTIONS ---
private val PhaseDescriptions = mapOf(
    "Period" to "This is your time to \"recharge.\" Since your iron levels can drop during your period, focus on iron-rich foods like spinach, lentils, or lean meats, paired with Vitamin C (like oranges) to help absorption.\n" +
            "\n" +
            "Activity Tip: Keep it low-impact. This is the perfect week for gentle stretching, yoga, or just taking a long nap. If you have \"period brain\" or feel extra tired, don't push yourself to pull an all-nighter for school - prioritize sleep.",
    "Follicular" to "As your energy climbs, support your gut health with fermented foods like yogurt or kimchi, and eat plenty of vibrant veggies like broccoli and carrots to help your body process rising estrogen.\n" +
            "\n" +
            "Activity Tip: Your brain is at its most \"plastic\" and ready to learn right now. This is the best time to start a new hobby, study a difficult subject, or say \"yes\" to social plans. You’ll likely find you have the mental stamina for complex problem-solving.",
    "Ovulation" to "Your metabolism is slightly higher now, so you might feel a bit hungrier. Reach for anti-inflammatory foods like berries, nuts, and fatty fish (or flaxseeds).\n" +
            "\n" +
            "Activity Tip: This is your \"power\" window. If you have a big presentation, a tryout, or a difficult conversation you’ve been putting off, do it now! You are naturally more communicative and confident. High-energy workouts like HIIT or dance are great during this phase.",
    "Luteal" to "As your body prepares for a new cycle, progesterone rises and then drops. This shift is the primary cause of PMS (Premenstrual Syndrome). PMS is a collection of physical and emotional symptoms that can start a week or two before your period. You might notice physical changes like bloating, breast tenderness, and skin breakouts, or emotional shifts like irritability, anxiety, or \"crying over nothing.\" It happens because your brain chemicals (like serotonin) react to the changing hormones. To help, focus on complex carbohydrates (sweet potatoes or oats) and magnesium-rich foods like dark chocolate to help stabilize your mood and reduce those \"hangry\" cravings.\n" +
            "\n" +
            "Activity Tip: You might feel more \"homebound\" or easily overwhelmed. This is your body’s signal to turn inward. Use this week for solo tasks, like journaling, organizing your room, or catching up on reading. If you feel sluggish or irritable, try a steady walk outdoors rather than a high-intensity workout. Remember - being a little extra sensitive right now is a biological response, not a personality flaw!",
    "Fertile" to "The fertile window is the time during your cycle when it is biologically possible to become pregnant. This window usually lasts about 6 days - the 5 days leading up to ovulation plus the day of ovulation itself. This is because sperm can live inside the body for up to 5 days, waiting for the egg to be released. During this time, you might notice your \"cervical mucus\" (discharge) becomes clear and stretchy, like raw egg whites.\n" +
            "\n" +
            "What to do: This is your peak energy time! It’s great for being social and active.\n" +
            "\n" +
            "What to eat: Focus on antioxidant-rich foods like blueberries and leafy greens to support your body during this high-energy hormonal peak.",
    "Not Fertile" to "The non-fertile phase makes up the majority of your cycle. It occurs in the days following ovulation until the start of your next period, and again for a short time immediately after your period ends. During these days, there is no egg present to be fertilized, and the hormone progesterone makes the environment less \"travel-friendly\" for sperm. While no phase is 100% guaranteed without medical tracking, this is the time when your body is focused on either preparing for a period or just finishing one.\n" +
            "\n" +
            "What to do: Focus on consistency and routine. Use this time to stay on top of schoolwork and get plenty of sleep.\n" +
            "\n" +
            "What to eat: Support your hormones with healthy fats like avocado or walnuts to keep your energy steady as your body transitions between phases."
)

@Composable
fun HomePage(
    selectedDate: LocalDate,
    currentCycleDay: Int,
    todayCycleDay: Int?,
    prediction: CyclePrediction?,
    calendarDays: Map<LocalDate, CalendarDayType>,
    logMap: Map<LocalDate, PopulatedDailyLog>,
    cycles: List<CyclesEntity>,
    onDateChange: (LocalDate) -> Unit,
    onNavigateToLog: () -> Unit,
    cycleLength: Int,
    periodLength: Int,
    onNavigateToEditCycle: (Int, Long) -> Unit,
    onNavigateToCalendarPopup: (LocalDate) -> Unit
) {

    var selectedPhaseInfo by remember { mutableStateOf<Pair<String, Color>?>(null) }


    val (phaseName, phaseColor) = if (prediction != null) {
        val pLen = prediction.averagePeriodLength
        val fLen = prediction.follicularPhaseLength
        val oEnd = fLen + 1 // Ovulation day
        val fertStart = prediction.fertileWindowLength // Usually around ovulation

        when (currentCycleDay) {
            in 1..pLen -> "Menstrual" to PeriodRed
            in (pLen + 1)..fLen -> "Follicular" to Color(0xFFB0C4DE)
            in (fLen + 1)..(fLen + 1) -> "Ovulation" to OvulationPurple
            else -> "Luteal" to Color(0xFF4682B4)
        }
    } else {
        // Fallback for new users
        when (currentCycleDay) {
            in 1..5 -> "Menstrual" to PeriodRed
            else -> "Follicular" to Color(0xFFB0C4DE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
    ) {
        CalendarStrip(
            selectedDate = selectedDate,
            dayMap = calendarDays,
            logMap = logMap,
            onDateSelected = { newDate ->
                if (newDate == selectedDate) {
                    // It's already selected -> Go to Calendar Popup
                    onNavigateToCalendarPopup(newDate)
                } else {
                    // It's a new date -> Just select it on Home
                    onDateChange(newDate)
                }
            },
            onEditPeriod = {

                // Convert selectedDate to Millis
                val dateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Search the list of cycles to see if this date falls inside one
                val foundCycle = cycles.find { cycle ->
                    val start = cycle.start_date
                    val end = cycle.end_date ?: Long.MAX_VALUE // Handle ongoing cycles
                    dateMillis in start..end
                }

                // If found, use that ID. If not, use -1 (Create Mode).
                val cycleId = foundCycle?.id ?: -1

                // Navigate
                onNavigateToEditCycle(cycleId, dateMillis)
            }
        )

        CycleWheel(
            selectedDate = selectedDate,
            currentCycleDay = currentCycleDay,
            todayCycleDay = todayCycleDay,
            onDateChange = onDateChange,
            currentDay = currentCycleDay,
            prediction = prediction ?: CyclePrediction(
                java.util.Date(), java.util.Date(), java.util.Date(), java.util.Date(),
                java.util.Date(), java.util.Date(), java.util.Date(), 30, 5, false, 14, 6, 14, 8, 6
            ),
            currentPhaseName = phaseName,
            currentPhaseColor = phaseColor,
            cycleLength = cycleLength,
            periodLength = periodLength
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(24.dp))
            PhaseLegendChip("Period", PeriodRed) { selectedPhaseInfo = "Period" to PeriodRed }
            PhaseLegendChip("Follicular", Color(0xFFB0C4DE)) { selectedPhaseInfo = "Follicular" to Color(0xFFB0C4DE) }
            PhaseLegendChip("Ovulation", OvulationPurple) { selectedPhaseInfo = "Ovulation" to OvulationPurple }
            PhaseLegendChip("Luteal", Color(0xFF4682B4)) { selectedPhaseInfo = "Luteal" to Color(0xFF4682B4) }
            PhaseLegendChip("Fertile", FertileGreen) { selectedPhaseInfo = "Fertile" to FertileGreen }
            PhaseLegendChip("Not Fertile", SoftPeach) { selectedPhaseInfo = "Not Fertile" to SoftPeach }
            Spacer(Modifier.width(24.dp))
        }

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClarificationCard(
                    label = "Cycle Day",
                    value = "$currentCycleDay",
                    icon = "🗓",
                    accentColor = PeriodRed,
                    modifier = Modifier.weight(1f)
                )
                ClarificationCard(
                    label = "Period in Days",
                    value = "${30 - currentCycleDay}",
                    icon = "🩸",
                    accentColor = Color(0xFFD38D82),
                    modifier = Modifier.weight(1f)
                )
                ClarificationCard(
                    label = "Phase",
                    value = phaseName,
                    icon = "🔄",
                    accentColor = phaseColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = onNavigateToLog,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+ Log Symptoms", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${selectedDate.monthValue}/${selectedDate.dayOfMonth}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (selectedPhaseInfo != null) {
        val (title, color) = selectedPhaseInfo!!
        val description = PhaseDescriptions[title] ?: "Information about this phase."

        AlertDialog(
            onDismissRequest = { selectedPhaseInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Colored dot to show phase color
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37323E), // Dark color for readability
                        fontSize = 24.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = description,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFF37323E)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedPhaseInfo = null }
                ) {
                    Text("Close", color = Color(0xFF37323E), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CycleWheel(
    selectedDate: LocalDate,
    currentCycleDay: Int,
    todayCycleDay: Int?,
    onDateChange: (LocalDate) -> Unit,
    prediction: CyclePrediction,
    currentDay: Int,
    currentPhaseName: String,
    currentPhaseColor: Color,
    cycleLength: Int,
    periodLength: Int
) {
    val isTodayInThisCycle = todayCycleDay != null

    //val currentPredictedCycleLen = prediction.averageCycleLength.toFloat()
    //val currentPredictedPeriodLen = prediction.averagePeriodLength.toFloat()

    fun calculateDayFromOffset(offset: Offset, size: androidx.compose.ui.unit.IntSize): Int {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val angle = atan2(offset.y - centerY, offset.x - centerX) * (180 / PI).toFloat()
        val normalizedAngle = (angle + 90 + 360) % 360
        return (normalizedAngle / (360f / cycleLength)).toInt() + 1
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .pointerInput(selectedDate) {
                detectTapGestures { offset ->
                    val day = calculateDayFromOffset(offset, size)
                    val difference: Int = day - currentCycleDay
                    onDateChange(selectedDate.plusDays(difference.toLong()))
                    //onDateChange(selectedDate.withDayOfMonth(day.coerceIn(1, cycleLength)))
                }
            }
            .pointerInput(selectedDate) {
                detectDragGestures { change, _ ->
                    val day = calculateDayFromOffset(change.position, size).coerceIn(1, cycleLength)
                    if (day != currentCycleDay) {
                        val difference: Int = day - currentCycleDay
                        onDateChange(selectedDate.plusDays(difference.toLong()))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {


            val sweepAnglePerDay = 360f / cycleLength

// FLEXIBLE EQUATIONS BASED ON PREDICTION
            val periodDays = periodLength.toFloat()

// Follicular phase is the time from Day 1 to Ovulation minus the period
            val follicularDays = (prediction.follicularPhaseLength - periodDays).coerceAtLeast(0f)
            val ovulationWindowDays = 1f // The specific event day
            //val ovulationDay = cycleLength - 14f
            val lutealDays = (cycleLength - (periodDays + follicularDays + ovulationWindowDays)).coerceAtLeast(0f)

// Fertility Window (The "Green" track) - usually starts before ovulation
            val fertileDays = prediction.averageFertileDuration.toFloat()
            val fertileStartDay = (prediction.follicularPhaseLength + 1) - (fertileDays - 1)

            val outerStroke = 14f
            val outerRadius = (size.minDimension / 2) - 10f
            val innerStroke = 65f
            val innerRadius = outerRadius - 55f


            val notFertileStart = periodDays + follicularDays

            // Outer Ring
            drawArc(SoftPeach, -90f, 360f, false, style = Stroke(outerStroke))
            drawArc(
                FertileGreen,
                -90f + ((fertileStartDay - 1) * sweepAnglePerDay),
                fertileDays * sweepAnglePerDay,
                false,
                style = Stroke(outerStroke, cap = StrokeCap.Round)
            )
            drawArc(SoftPeach, -90f + ((notFertileStart + ovulationWindowDays) * sweepAnglePerDay), lutealDays * sweepAnglePerDay, false, style = Stroke(outerStroke, cap = StrokeCap.Round))

            // Inner Ring (Main Phases)
            var startAngle = -90f

// 1. Menstrual
            drawArc(PeriodRed, startAngle, periodDays * sweepAnglePerDay, false, style = Stroke(innerStroke), topLeft = Offset(center.x - innerRadius, center.y - innerRadius), size = Size(innerRadius * 2, innerRadius * 2))
            startAngle += periodDays * sweepAnglePerDay

// 2. Follicular
            drawArc(Color(0xFFB0C4DE), startAngle, follicularDays * sweepAnglePerDay, false, style = Stroke(innerStroke), topLeft = Offset(center.x - innerRadius, center.y - innerRadius), size = Size(innerRadius * 2, innerRadius * 2))
            startAngle += follicularDays * sweepAnglePerDay

// 3. Ovulation
            drawArc(OvulationPurple, startAngle, ovulationWindowDays * sweepAnglePerDay, false, style = Stroke(innerStroke), topLeft = Offset(center.x - innerRadius, center.y - innerRadius), size = Size(innerRadius * 2, innerRadius * 2))
            startAngle += ovulationWindowDays * sweepAnglePerDay

// 4. Luteal
            drawArc(Color(0xFF4682B4), startAngle, lutealDays * sweepAnglePerDay, false, style = Stroke(innerStroke), topLeft = Offset(center.x - innerRadius, center.y - innerRadius), size = Size(innerRadius * 2, innerRadius * 2))
            // Today Indicator
            if (isTodayInThisCycle && todayCycleDay <= cycleLength) {
                val tAngle = (todayCycleDay - 1) * sweepAnglePerDay - 90f
                val tRad = tAngle * (PI / 180f).toFloat()
                val indicatorDistance = innerRadius + 43f

                drawCircle(
                    color = Color.DarkGray.copy(0.3f),
                    radius = 80f, // Slightly larger than the selection handle to "hold" it
                    center = Offset(
                        center.x + 108.dp.toPx() * cos(tRad), // Using dp.toPx() ensures exact alignment with the handle offset
                        center.y + 108.dp.toPx() * sin(tRad)
                    ),
                    //  style = Stroke(width = 15f) // Makes it a ring/holder instead of a solid circle
                ) }
        }

        // CENTRAL INFORMATION DISC
        Surface(
            modifier = Modifier.size(195.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(20.dp)
            ) {
                // 1. Phase Label (e.g., Menstrual)
                Text(

                    text = currentPhaseName, // Use the dynamic name
                    color = currentPhaseColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )

                // 2. Main Cycle Day Text
                Text(
                    text = "Day $currentDay",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF37323E)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Status Description
                Text(
                    text = if (currentDay in 1..5) {
                        "Currently on period"
                    } else {
                        val daysUntil = cycleLength - currentDay + 1
                        "Next period expected in $daysUntil days"
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // DRAGGABLE SELECTION HANDLE (Positioned on the track)
        val handleAngle = (currentDay - 1) * (360f / cycleLength) - 90f
        val angleRad = handleAngle * (PI / 180f).toFloat()
        val handleDistance = 108f

        Box(
            modifier = Modifier.offset(
                x = (handleDistance * cos(angleRad)).dp,
                y = (handleDistance * sin(angleRad)).dp
            )
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, PeriodRed),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                        color = PeriodRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// CLICKABLE COMPONENT
@Composable
fun PhaseLegendChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF37323E)
                )
            )
        }
    }
}

@Composable
fun ClarificationCard(
    label: String,
    value: String,
    icon: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray, fontWeight = FontWeight.Normal))
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF37323E), fontWeight = FontWeight.Bold, fontSize = 20.sp))
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.size(60.dp).background(accentColor, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Text(text = icon, fontSize = 26.sp)
        }
    }
}