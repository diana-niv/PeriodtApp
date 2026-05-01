package com.example.periodtapp.data

import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.periodtapp.db.ActivityType
import com.example.periodtapp.db.CollectionMethod
import com.example.periodtapp.db.CravingType
import com.example.periodtapp.db.CyclePrediction
import com.example.periodtapp.db.CyclesEntity
import com.example.periodtapp.db.DischargeType
import com.example.periodtapp.db.Energy
import com.example.periodtapp.db.FlowType
import com.example.periodtapp.db.MoodType
import com.example.periodtapp.db.PeriodtDao
import com.example.periodtapp.db.PopulatedDailyLog
import com.example.periodtapp.db.SexType
import com.example.periodtapp.db.SpottingType
import com.example.periodtapp.db.SymptomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

class PeriodtRepository(private val periodtDao: PeriodtDao) {

    val allHistory: Flow<List<PopulatedDailyLog>> = periodtDao.getAllHistory()

    fun getLogForDate(date: Long): Flow<PopulatedDailyLog?> {
        return periodtDao.getLogByDate(date)
    }

    val allCycles: Flow<List<CyclesEntity>> = periodtDao.getAllCycles()

    suspend fun refreshAllPredictions() {
        updateCycleAverages()
    }

    suspend fun getCycleById(id: Int): CyclesEntity? {
        return periodtDao.getCycleById(id)
    }

    suspend fun getCycleByEndDate(endDateMillis: Long): CyclesEntity? {
        return periodtDao.getCycleByEndDate(endDateMillis)
    }

    suspend fun getCycleByStartDate(startDateMillis: Long): CyclesEntity? {
        return periodtDao.getCycleByStartDate(startDateMillis)
    }

    suspend fun linkLogToCycle(logId: Int, newCycleId: Int) {
        periodtDao.linkLogToCycle(logId, newCycleId)
    }

    suspend fun addCycle(startDate: Long, endDate: Long?) {
        val periodLen = calculatePeriodLength(startDate, endDate)
        val cycle = CyclesEntity(start_date = startDate, end_date = endDate, periodLength = periodLen)
        periodtDao.insertCycle(cycle)
    }

    suspend fun updateCycle(id: Int, startDate: Long, endDate: Long?) {
        val periodLen = calculatePeriodLength(startDate, endDate)
        periodtDao.updateCycleDates(id, startDate, endDate, periodLen)
    }

    private fun calculatePeriodLength(startDate: Long, endDate: Long?): Int? {
        if (endDate != null) {
            val diffMillisPeriod = endDate - startDate
            return (diffMillisPeriod.toDouble() / (1000 * 60 * 60 * 24)).roundToInt() + 1
        } else {
            return null
        }
    }

    suspend fun deleteCycle(id: Int) {
        periodtDao.deleteCycle(id)
    }

    suspend fun getCycleIdForDate(date: Long): Int? {
        return periodtDao.getCycleForDate(date)?.id
    }

    suspend fun getCycleIdWithinDates(date: Long): Int? {
        return periodtDao.getCycleWithinDates(date)?.id
    }

    suspend fun getLatestCycleBefore(date: Long): CyclesEntity? {
        return periodtDao.getLatestCycleBefore(date)
    }

    suspend fun getCycleForDate(date: Long): CyclesEntity? {
        return periodtDao.getCycleForDate(date)
    }

    suspend fun getDayOfCycle(date: Long, cycleId: Int? = null): Int? {
        val cycle = if (cycleId != null) {
            getCycleById(cycleId)
        } else {
            getCycleForDate(date)
        }


        if (cycle != null) {
            val diff = date - cycle.start_date
            val dayNum = TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
            return if (dayNum > 0) dayNum else 1
        } else {
            return null
        }
    }


    suspend fun createDefaultCycle(startDate: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(Calendar.DAY_OF_YEAR, 4)
        val endDate = calendar.timeInMillis

        addCycle(startDate, endDate)
    }

    suspend fun createDailyLog(
        cycleId: Int?,
        date: Long,
        flow: FlowType? = null,
        spotting: SpottingType? = null,
        discharge: DischargeType? = null,
        collection: CollectionMethod? = null,
        moods: List<MoodType> = emptyList(),
        symptoms: List<SymptomType> = emptyList(),
        activities: List<ActivityType> = emptyList(),
        cravings: List<CravingType> = emptyList(),
        sex: List<SexType> = emptyList(),
        energy: List<Energy> = emptyList()
    ) {
        periodtDao.saveFullLog(
            cycleId, date,
            flow, spotting, discharge, collection,
            moods, symptoms, activities, cravings,
            sex, energy
        )
    }

    suspend fun updateDailyLog(
        logId: Int,
        flow: FlowType? = null,
        spotting: SpottingType? = null,
        discharge: DischargeType? = null,
        collection: CollectionMethod? = null,
        moods: List<MoodType> = emptyList(),
        symptoms: List<SymptomType> = emptyList(),
        activities: List<ActivityType> = emptyList(),
        cravings: List<CravingType> = emptyList(),
        sex: List<SexType> = emptyList(),
        energy: List<Energy> = emptyList()
    ) {
        periodtDao.updateFullLog(
            logId,
            flow, spotting, discharge, collection,
            moods, symptoms, activities, cravings,
            sex, energy
        )
    }

    suspend fun deleteDailyLog(logId: Int) {
        periodtDao.deleteCycleLog(logId)
    }

    suspend fun updateCycleAverages() {
        val completedCycles = periodtDao.getCompletedCycles().firstOrNull() ?: return

        completedCycles.forEach { cycle ->
            val nextCycle = completedCycles.find { it.start_date > cycle.start_date }
            if (nextCycle != null) {
                val diffMillis = nextCycle.start_date - cycle.start_date
                val cycleLen = (diffMillis.toDouble() / (1000 * 60 * 60 * 24)).roundToInt()


                val periodLen: Int = calculatePeriodLength(cycle.start_date, cycle.end_date) ?: 0

                periodtDao.updateCycleLengths(cycle.id, periodLen, cycleLen)
            }
        }
    }

    suspend fun getCyclePrediction(): CyclePrediction? {
        val completedCycles = periodtDao.getCompletedCycles().firstOrNull() ?: return generateDefaultPrediction()
        if (completedCycles.isEmpty()) return generateDefaultPrediction()

        val cycleLengths = completedCycles.mapNotNull { it.cycleLength }
        val periodLengths = completedCycles.mapNotNull { it.periodLength }

        val avgCycleLength = if (cycleLengths.isNotEmpty()) {
            cycleLengths.takeLast(3).average().roundToInt()
        } else 28
        val avgPeriodLength = if (periodLengths.isNotEmpty()) {
            periodLengths.takeLast(3).average().toInt()
        } else 5

        val follicularLen = (avgCycleLength - 14).coerceAtLeast(7)
        val lutealLen = avgCycleLength - follicularLen

        val avgFertileDuration = 6
        val nonFertileLen = avgCycleLength - avgFertileDuration

        val maxCycle = cycleLengths.maxOrNull() ?: 0
        val minCycle = cycleLengths.minOrNull() ?: 0
        val isIrregular = (maxCycle - minCycle) > 7

        val lastCycle = completedCycles.maxByOrNull { it.start_date } ?: return null
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastCycle.start_date
        calendar.add(Calendar.DAY_OF_YEAR, avgCycleLength)
        val nextPeriodStartDate = calendar.time

        return CyclePrediction(
            predictedPeriodStartDate = nextPeriodStartDate,
            predictedPeriodEndDate = addDays(nextPeriodStartDate, avgPeriodLength - 1),
            follicularPhaseEndDate = addDays(nextPeriodStartDate, follicularLen - 1),
            ovulationDate = addDays(nextPeriodStartDate, follicularLen),
            fertileWindowStartDate = addDays(nextPeriodStartDate, follicularLen - 5),
            fertileWindowEndDate = addDays(nextPeriodStartDate, follicularLen + 1),
            lutealPhaseEndDate = addDays(nextPeriodStartDate, avgCycleLength - 1),
            averageCycleLength = avgCycleLength,
            averagePeriodLength = avgPeriodLength,
            isIrregular = isIrregular,
            follicularPhaseLength = follicularLen,
            fertileWindowLength = avgFertileDuration,
            lutealPhaseLength = lutealLen,
            nonFertileLength = nonFertileLen,
            averageFertileDuration = avgFertileDuration
        )
    }

    private suspend fun generateDefaultPrediction(): CyclePrediction? {
        val latestLogWithPeriod = periodtDao.getLatestPeriodDay().firstOrNull() ?: return null
        val lastPeriodStartDate = latestLogWithPeriod.log.log_date

        val defaultCycleLength = 28
        val defaultPeriodLength = 5
        val defaultFollicularLen = 14
        val defaultFertileLen = 6

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastPeriodStartDate
        calendar.add(Calendar.DAY_OF_YEAR, defaultCycleLength)
        val nextPeriodStartDate = calendar.time

        return CyclePrediction(
            predictedPeriodStartDate = nextPeriodStartDate,
            predictedPeriodEndDate = addDays(nextPeriodStartDate, defaultPeriodLength - 1),
            follicularPhaseEndDate = addDays(nextPeriodStartDate, defaultFollicularLen - 1),
            ovulationDate = addDays(nextPeriodStartDate, defaultFollicularLen),
            fertileWindowStartDate = addDays(nextPeriodStartDate, defaultFollicularLen - 5),
            fertileWindowEndDate = addDays(nextPeriodStartDate, defaultFollicularLen + 1),
            lutealPhaseEndDate = addDays(nextPeriodStartDate, defaultCycleLength - 1),
            averageCycleLength = defaultCycleLength,
            averagePeriodLength = defaultPeriodLength,
            isIrregular = false,
            follicularPhaseLength = defaultFollicularLen,
            fertileWindowLength = defaultFertileLen,
            lutealPhaseLength = defaultCycleLength - defaultFollicularLen - defaultPeriodLength - 1,
            nonFertileLength = defaultCycleLength - defaultFertileLen,
            averageFertileDuration = defaultFertileLen
        )
    }

    private fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.time
    }

    fun getPredictionFlow(): Flow<CyclePrediction?> =
        kotlinx.coroutines.flow.combine(allHistory, allCycles) { _, _ ->
            // Every time history or cycle dates change, re-run the math
            getCyclePrediction()
        }
}