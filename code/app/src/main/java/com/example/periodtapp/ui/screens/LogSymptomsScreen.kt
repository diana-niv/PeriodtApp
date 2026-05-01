package com.example.periodtapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.periodtapp.db.ActivityType
import com.example.periodtapp.db.CollectionMethod
import com.example.periodtapp.db.CravingType
import com.example.periodtapp.db.DischargeType
import com.example.periodtapp.db.Energy
import com.example.periodtapp.db.FlowType
import com.example.periodtapp.db.MoodType
import com.example.periodtapp.db.PopulatedDailyLog
import com.example.periodtapp.db.SexType
import com.example.periodtapp.db.SpottingType
import com.example.periodtapp.db.SymptomType
import com.example.periodtapp.db.prettyName
import com.example.periodtapp.ui.ViewModels.CalendarDayType
import com.example.periodtapp.ui.ViewModels.LogViewModel
import com.example.periodtapp.ui.components.CalendarStrip
import com.example.periodtapp.ui.theme.Matcha300
import com.example.periodtapp.ui.theme.Matcha800
import com.example.periodtapp.ui.theme.Peach
import com.example.periodtapp.ui.theme.Peach800
import com.example.periodtapp.ui.theme.PinkClouds
import com.example.periodtapp.ui.theme.Red
import com.example.periodtapp.ui.theme.Rose100
import com.example.periodtapp.ui.theme.SummerSky100
import com.example.periodtapp.ui.theme.SummerSky700
import com.example.periodtapp.ui.theme.Yellow
import com.example.periodtapp.ui.theme.Yellow800
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Theme Colors
private val SectionTitleColor = Color(0xFF4A4A4A)
private val SaveButtonColor = Color(0xFFD46A6A)

// New Colors for Sex & Energy
private val SexColor = Color(0xFFF48FB1)      // Pink
private val SexColorBg = Color(0xFFFCE4EC)    // Light Pink
private val EnergyColor = Color(0xFF80CBC4)   // Teal
private val EnergyColorBg = Color(0xFFE0F2F1) // Light Teal

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun LogSymptomsScreen(
    viewModel: LogViewModel,
    date: LocalDate,
    calendarDays: Map<LocalDate, CalendarDayType>,
    onDateChange: (LocalDate) -> Unit,
    logDataMap: Map<LocalDate, PopulatedDailyLog>,
    onNavigateBack: () -> Unit
) {
    // 1. Load Data
    LaunchedEffect(date) {
        viewModel.loadLog(date)
        onDateChange(date)
    }

    // 2. Observe State
    val selectedFlow by viewModel.selectedFlow.collectAsState()
    val selectedSpotting by viewModel.selectedSpotting.collectAsState()
    val selectedDischarge by viewModel.selectedDischarge.collectAsState()
    val selectedCollection by viewModel.selectedCollection.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 3. New State for Custom Toast
    var showSavedMessage by remember { mutableStateOf(false) }

    // 4. Auto-hide logic for the toast
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(2000) // Show for 2 seconds
            showSavedMessage = false
        }
    }

    // 5. Derived State for UI
    val isScrolled = scrollState.value > 10

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SaveButtonColor)
        }
        return
    }

    // 6. WRAP EVERYTHING IN A BOX TO SUPPORT OVERLAY
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PinkClouds)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Log Symptoms",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SectionTitleColor
                )
                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(visible = isScrolled) {
                    Button(
                        onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = viewModel.selectedDate.value.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                            color = SectionTitleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(visible = !isScrolled) {
                    CalendarStrip(
                        selectedDate = viewModel.selectedDate.value,
                        dayMap = calendarDays,
                        logMap = logDataMap,
                        onDateSelected = { newDate ->
                            viewModel.loadLog(newDate)
                            onDateChange(newDate)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedVisibility(visible = !isScrolled) {
                    Text(
                        text = viewModel.selectedDate.value.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                        style = MaterialTheme.typography.headlineSmall,
                        color = SectionTitleColor,
                        modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                    )
                }

                // 1. MENSTRUAL FLOW
                LogCategoryCard(title = "Menstrual Flow") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowType.values().forEach { flow ->
                            CircularLogButton(
                                label = flow.prettyName(),
                                isSelected = selectedFlow == flow,
                                selectedColor = Red,
                                unselectedColor = Rose100,
                                icon = flow.iconRes,
                                onClick = { viewModel.onFlowSelected(flow) }
                            )
                        }
                    }
                }

                // 2. SPOTTING
                LogCategoryCard(title = "Spotting") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SpottingType.values().forEach { s ->
                            CircularLogButton(
                                label = s.prettyName(),
                                isSelected = selectedSpotting == s,
                                selectedColor = Red,
                                unselectedColor = Rose100,
                                icon = s.iconRes,
                                onClick = { viewModel.onSpottingSelected(s) }
                            )
                        }
                    }
                }

                // 3. MOOD
                LogCategoryCard(title = "Mood") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MoodType.values().forEach { mood ->
                            CircularLogButton(
                                label = mood.prettyName(),
                                isSelected = viewModel.selectedMoods.contains(mood),
                                selectedColor = SummerSky700,
                                unselectedColor = SummerSky100,
                                icon = mood.iconRes,
                                showOriginalColors = true,
                                onClick = { viewModel.toggleMood(mood) }
                            )
                        }
                    }
                }

                // 4. SYMPTOMS
                LogCategoryCard(title = "Symptoms") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SymptomType.values().forEach { symptom ->
                            CircularLogButton(
                                label = symptom.prettyName(),
                                isSelected = viewModel.selectedSymptoms.contains(symptom),
                                selectedColor = Matcha800,
                                unselectedColor = Matcha300,
                                icon = symptom.iconRes,
                                onClick = { viewModel.toggleSymptom(symptom) }
                            )
                        }
                    }
                }

                // 5. DISCHARGE
                LogCategoryCard(title = "Discharge") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DischargeType.values().forEach { d ->
                            if (d != DischargeType.NONE) {
                                CircularLogButton(
                                    label = d.prettyName(),
                                    isSelected = selectedDischarge == d,
                                    selectedColor = SummerSky700,
                                    unselectedColor = SummerSky100,
                                    icon = d.iconRes,
                                    onClick = { viewModel.onDischargeSelected(d) }
                                )
                            }
                        }
                    }
                }

                // 6. COLLECTION METHOD
                LogCategoryCard(title = "Collection Method") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CollectionMethod.values().forEach { c ->
                            CircularLogButton(
                                label = c.prettyName(),
                                isSelected = selectedCollection == c,
                                selectedColor = SummerSky700,
                                unselectedColor = SummerSky100,
                                icon = c.iconRes,
                                onClick = { viewModel.onCollectionSelected(c) }
                            )
                        }
                    }
                }

                // 7. CRAVINGS
                LogCategoryCard(title = "Cravings") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CravingType.values().forEach { c ->
                            CircularLogButton(
                                label = c.prettyName(),
                                isSelected = viewModel.selectedCravings.contains(c),
                                selectedColor = Peach800,
                                unselectedColor = Peach,
                                icon = c.iconRes,
                                onClick = { viewModel.toggleCraving(c) }
                            )
                        }
                    }
                }

                // 8. PHYSICAL ACTIVITY
                LogCategoryCard(title = "Physical Activity") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActivityType.values().forEach { activity ->
                            CircularLogButton(
                                label = activity.prettyName(),
                                isSelected = viewModel.selectedActivities.contains(activity),
                                selectedColor = Yellow800,
                                unselectedColor = Yellow,
                                icon = activity.iconRes,
                                onClick = { viewModel.toggleActivity(activity) }
                            )
                        }
                    }
                }

                // 9. SEX & DRIVE
                LogCategoryCard(title = "Sex & Drive") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SexType.values().forEach { sex ->
                            CircularLogButton(
                                label = sex.prettyName(),
                                isSelected = viewModel.selectedSex.contains(sex),
                                selectedColor = SexColor,
                                unselectedColor = SexColorBg,
                                icon = sex.iconRes,
                                onClick = { viewModel.toggleSex(sex) }
                            )
                        }
                    }
                }

                // 10. ENERGY
                LogCategoryCard(title = "Energy") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Energy.values().forEach { energy ->
                            CircularLogButton(
                                label = energy.prettyName(),
                                isSelected = viewModel.selectedEnergy.contains(energy),
                                selectedColor = EnergyColor,
                                unselectedColor = EnergyColorBg,
                                icon = energy.iconRes,
                                onClick = { viewModel.toggleEnergy(energy) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }

            Button(
                onClick = {
                    viewModel.saveLog(
                        viewModel.selectedDate.value,
                        onSaved = {
                            showSavedMessage = true
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaveButtonColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = showSavedMessage,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .zIndex(2f)
        ) {
            Surface(
                color = Color(0xFF37323E),
                shape = RoundedCornerShape(50),
                shadowElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Symptoms Saved",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = SectionTitleColor,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun TextChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    textColorSelected: Color = Color.White,
    icon: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) selectedColor else unselectedColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (isSelected) textColorSelected else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = label,
                color = if (isSelected) textColorSelected else Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CircularLogButton(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    icon: Int?,
    showOriginalColors: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. THE CIRCLE
        Surface(
            shape = CircleShape,
            color = if (isSelected) selectedColor else unselectedColor,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    val iconTint = if (showOriginalColors) {
                        Color.Unspecified
                    } else {
                        if (isSelected) Color.White else selectedColor
                    }

                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = iconTint
                    )
                } else {
                    Text(
                        text = label.take(1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isSelected) Color.White else selectedColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LogCategoryCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A4A4A),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }