package com.example.periodtapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.periodtapp.db.CyclePrediction
import com.example.periodtapp.ui.theme.OffWhite
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.CircleShape
import com.example.periodtapp.ui.theme.PeriodRed

@Composable
fun ProfilePage(prediction: CyclePrediction?) {
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Cycle Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
            color = Color.Black
        )



        if (prediction == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Insufficient data to generate a detailed report. Keep logging your symptoms and period dates.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // --- SECTION 1: ACTIVE CYCLE STATS (Current Cycle Breakdown) ---
            ProfileHeader("Current Cycle Breakdown")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {

                Column(Modifier.padding(20.dp)) {
                    // Calculation of phase lengths based on prediction data
                    val follicularLen = prediction.follicularPhaseLength
                    val lutealLen = prediction.lutealPhaseLength
                    val fertileLen = prediction.fertileWindowLength
                    val nonFertileLen = prediction.nonFertileLength

                    StatRowWithDot("Cycle Length", "${prediction.averageCycleLength} days", Color.DarkGray)
                    StatRowWithDot("Menstruation Length", "${prediction.averagePeriodLength} days", PeriodRed)
                    StatRowWithDot("Follicular Phase", "$follicularLen days", Color(0xFFA5D6A7))
                    StatRowWithDot("Ovulation Day", "Day ${follicularLen + 1}", Color(0xFF7986CB))
                    StatRowWithDot("Luteal Phase", "$lutealLen days", Color(0xFFFFB74D))

                    Divider(Modifier.padding(vertical = 12.dp))

                    StatRowWithDot("Fertility Window", "$fertileLen days", Color(0xFF81C784))
                    StatRowWithDot("Non-Fertile Days", "$nonFertileLen days", Color(0xFFBDBDBD))


                }
            }

            Spacer(Modifier.height(24.dp))

            // --- SECTION 2: PREDICTION OVERVIEW ---
            ProfileHeader("Upcoming Predictions")
            DetailCard(listOf(
                "Predicted Start" to dateFormat.format(prediction.predictedPeriodStartDate),
                "Predicted End" to dateFormat.format(prediction.predictedPeriodEndDate),
                "Ovulation Date" to dateFormat.format(prediction.ovulationDate)
            ))

            Spacer(Modifier.height(24.dp))
// --- SECTION 3: HEALTH AVERAGES ---
            ProfileHeader("History & Status")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    val statusText = if (prediction.isIrregular) "Irregular" else "Regular"
                    val statusColor = if (prediction.isIrregular) Color(0xFFE57373) else Color(0xFF4CAF50)

                    StatRow("Average Cycle", "${prediction.averageCycleLength} days")
                    StatRow("Average Period", "${prediction.averagePeriodLength} days")
                    StatRow("Cycle Status", statusText, statusColor)

                    Divider(Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Calculations are based on a weighted average of your most recent cycles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun ProfileHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun DetailCard(items: List<Pair<String, String>>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            items.forEach { (label, value) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Color.DarkGray, fontSize = 14.sp)
                    Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 15.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, color = valueColor, fontSize = 15.sp)
    }
}


@Composable
fun StatRowWithDot(label: String, value: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(label, color = Color.DarkGray, fontSize = 15.sp)
        }
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF37323E),
            fontSize = 15.sp
        )
    }
}