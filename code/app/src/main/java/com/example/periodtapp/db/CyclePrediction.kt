package com.example.periodtapp.db

import java.util.*
import kotlin.math.roundToInt
import com.example.periodtapp.db.FlowType
import com.example.periodtapp.db.DischargeType
import com.example.periodtapp.db.MoodType
import com.example.periodtapp.db.SymptomType
import com.example.periodtapp.db.Energy
import com.example.periodtapp.db.SexType


//MODELS
data class CyclePrediction(
    val predictedPeriodStartDate: Date,
    val predictedPeriodEndDate: Date,
    val follicularPhaseEndDate: Date,
    val ovulationDate: Date,
    val fertileWindowStartDate: Date,
    val fertileWindowEndDate: Date,
    val lutealPhaseEndDate: Date,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val isIrregular: Boolean,
    val follicularPhaseLength: Int,
    val fertileWindowLength: Int,
    val lutealPhaseLength: Int,
    val nonFertileLength: Int,
    val averageFertileDuration: Int
)

data class PeriodLog(
    val date: Date,
    val flow: FlowType? = null,
    val discharge: DischargeType? = null,
    val mood: MoodType? = null,
    val symptoms: List<SymptomType>? = null,
    val energy: Energy? = null,
    val sex: SexType? = null
)

private data class Period(
    val start: Date,
    val end: Date,
    val length: Int,
    val cycleLengthToNext: Int? = null
)

//DATE UTILITIES

private fun Date.plusDays(days: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.add(Calendar.DAY_OF_YEAR, days)
    return cal.time
}

private fun daysBetween(start: Date, end: Date): Int {
    val diff = end.time - start.time
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

//CORE LOGIC

fun predictNextCycle(logs: List<PeriodLog>): CyclePrediction {
    val startFallback = Date()

    // Fallback for empty logs
    if (logs.isEmpty()) {
        return CyclePrediction(startFallback, startFallback.plusDays(4), startFallback, startFallback, startFallback, startFallback, startFallback, 28, 4, false, 14, 6, 14, 8, 6)
    }

    val periods = extractPeriods(logs)
    if (periods.isEmpty()) {
        val start = logs.last().date
        return CyclePrediction(start, start.plusDays(4), start, start, start, start, start, 28, 4, false, 14, 6, 14, 8, 6)
    }

    // 1. Calculate weighted metrics FIRST
    val avgCycle = calculateWeightedCycleLength(periods)
    val avgPeriod = calculateWeightedPeriodLength(periods)
    val avgLuteal = calculateAverageLutealPhase(periods, logs)
    val isIrregular = checkIsIrregular(periods)
    val lastPeriod = periods.last()

    // 2. Determine Predictions
    val predictedPeriodStart = lastPeriod.start.plusDays(avgCycle)
    val predictedPeriodEnd = predictedPeriodStart.plusDays(avgPeriod - 1)

    // 3. Ovulation Logic
    val ovulationDate = estimateOvulationDate(lastPeriod.start, predictedPeriodStart, avgLuteal, logs)

    // 4. Window Calculations
    // We calculate a dynamic fertile duration based on symptoms if possible
    val avgFertileDuration = calculateWeightedFertileDuration(logs)

    // The fertile window typically ends 1 day after ovulation
    val fertileStart = ovulationDate.plusDays(-(avgFertileDuration - 1))
    val fertileEnd = ovulationDate.plusDays(1)

    val follicularEnd = ovulationDate.plusDays(-1)
    val lutealEnd = predictedPeriodStart.plusDays(-1)

    // 5. Statistics for Profile/Circle (The Logic you added)
    // MATH CHECK: follicularLen + lutealLen MUST equal avgCycle
    val follicularLen = daysBetween(lastPeriod.start, ovulationDate)
    val lutealLen = avgCycle - follicularLen - avgPeriod - 1

    // MATH CHECK: fertileLen + nonFertileLen MUST equal avgCycle
    val fertileLen = avgFertileDuration
    val nonFertileLen = avgCycle - fertileLen



    return CyclePrediction(
        predictedPeriodStartDate = predictedPeriodStart,
        predictedPeriodEndDate = predictedPeriodEnd,
        follicularPhaseEndDate = follicularEnd,
        ovulationDate = ovulationDate,
        fertileWindowStartDate = fertileStart,
        fertileWindowEndDate = fertileEnd,
        lutealPhaseEndDate = lutealEnd,
        averageCycleLength = avgCycle,
        averagePeriodLength = avgPeriod,
        isIrregular = isIrregular,
        follicularPhaseLength = follicularLen,
        fertileWindowLength = fertileLen,
        lutealPhaseLength = lutealLen,
        nonFertileLength = nonFertileLen,
        averageFertileDuration = avgFertileDuration
    )
}

private fun calculateWeightedFertileDuration(logs: List<PeriodLog>): Int {
    val fertileDaysPerCycle = mutableListOf<Int>()
    var currentStreak = 0
    val sortedLogs = logs.sortedBy { it.date }

    for (log in sortedLogs) {
        val isFertile = log.discharge == DischargeType.EGGWHITE || log.sex == SexType.HIGH_SEX_DRIVE
        if (isFertile) {
            currentStreak++
        } else {
            if (currentStreak > 0) fertileDaysPerCycle.add(currentStreak)
            currentStreak = 0
        }
    }

    if (fertileDaysPerCycle.isEmpty()) return 6 // Standard medical average

    return if (fertileDaysPerCycle.size >= 3) {
        val recent = fertileDaysPerCycle.takeLast(3).average()
        val historical = fertileDaysPerCycle.average()
        ((recent * 0.8) + (historical * 0.2)).roundToInt().coerceIn(3, 9)
    } else {
        fertileDaysPerCycle.average().roundToInt().coerceIn(3, 9)
    }
}

private fun extractPeriods(logs: List<PeriodLog>): List<Period> {
    val sorted = logs.filter { it.flow != null && it.flow != FlowType.NO_PERIOD }.sortedBy { it.date }
    if (sorted.isEmpty()) return emptyList()

    val periods = mutableListOf<Period>()
    var currentStart = sorted.first().date
    var currentLast = sorted.first().date

    for (i in 1 until sorted.size) {
        val log = sorted[i]
        val gap = daysBetween(currentLast, log.date)

        if (gap > 3) {
            periods.add(Period(currentStart, currentLast, daysBetween(currentStart, currentLast) + 1))
            currentStart = log.date
        }
        currentLast = log.date
    }
    periods.add(Period(currentStart, currentLast, daysBetween(currentStart, currentLast) + 1))

    return periods.mapIndexed { index, period ->
        if (index < periods.size - 1) {
            period.copy(cycleLengthToNext = daysBetween(period.start, periods[index + 1].start))
        } else period
    }
}

private fun calculateWeightedCycleLength(periods: List<Period>): Int {
    val lengths = periods.mapNotNull { it.cycleLengthToNext }.filter { it in 15..50 }
    if (lengths.isEmpty()) return 28 // Default for brand new users

    return if (lengths.size >= 2) {
        // We prioritize your most recent data (the 29-day cycle you just logged)
        // Recent cycles get 80% weight, older history gets 20%
        val recentAverage = lengths.takeLast(3).average()
        val historicalAverage = lengths.average()

        ((recentAverage * 0.8) + (historicalAverage * 0.2)).roundToInt()
    } else {
        // If only one cycle is recorded, use it exactly
        lengths.first().toFloat().roundToInt()
    }
}

private fun calculateWeightedPeriodLength(periods: List<Period>): Int {
    val lengths = periods.map { it.length }
    if (lengths.isEmpty()) return 4 // Set default to 4

    return if (lengths.size >= 3) {
        // HIGHER WEIGHTING:
        // Last 3 logs get 80% importance, older logs get 20%
        val recentAverage = lengths.takeLast(3).average()
        val historicalAverage = lengths.average()

        ((recentAverage * 0.8) + (historicalAverage * 0.2)).roundToInt()
    } else {
        // If we only have 1 or 2 logs, just use the absolute average
        lengths.average().roundToInt()
    }
}

private fun calculateAverageLutealPhase(periods: List<Period>, logs: List<PeriodLog>): Int {
    val lutealGaps = mutableListOf<Int>()
    for (i in 0 until periods.size - 1) {
        val currentPeriod = periods[i]
        val nextPeriod = periods[i+1]
        val ovulationLog = logs.find {
            it.date > currentPeriod.start && it.date < nextPeriod.start && it.discharge == DischargeType.EGGWHITE
        }
        if (ovulationLog != null) lutealGaps.add(daysBetween(ovulationLog.date, nextPeriod.start))
    }
    return if (lutealGaps.isNotEmpty()) lutealGaps.average().roundToInt() else 14
}

private fun estimateOvulationDate(lastPeriodStart: Date, predictedNextPeriod: Date, avgLuteal: Int, logs: List<PeriodLog>): Date {
    val currentSymptomLogs = logs.filter { it.date >= lastPeriodStart }
    val peakSymptom = currentSymptomLogs.maxByOrNull { log ->
        var score = 0
        if (log.discharge == DischargeType.EGGWHITE) score += 10
        if (log.sex == SexType.HIGH_SEX_DRIVE) score += 3
        if (log.energy == Energy.ENERGETIC) score += 2
        score
    }
    return if (peakSymptom != null && (peakSymptom.discharge == DischargeType.EGGWHITE)) peakSymptom.date
    else predictedNextPeriod.plusDays(-avgLuteal)
}

private fun checkIsIrregular(periods: List<Period>): Boolean {
    val lengths = periods.mapNotNull { it.cycleLengthToNext }
    if (lengths.size < 3) return false
    val avg = lengths.average()
    return lengths.any { Math.abs(it - avg) > 6 }
}