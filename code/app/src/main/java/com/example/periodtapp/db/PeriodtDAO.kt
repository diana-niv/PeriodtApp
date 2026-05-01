package com.example.periodtapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodtDao {

        // 1. CYCLE MANAGEMENT (Period Dates)

        @Query("SELECT * FROM cycles ORDER BY start_date DESC")
        fun getAllCycles(): Flow<List<CyclesEntity>>

        @Query("SELECT * FROM cycles WHERE id = :id")
        suspend fun getCycleById(id: Int): CyclesEntity?

        @Query("SELECT * FROM cycles WHERE end_date = :endDateMillis LIMIT 1")
        suspend fun getCycleByEndDate(endDateMillis: Long): CyclesEntity?

        @Query("SELECT * FROM cycles WHERE start_date = :startDateMillis LIMIT 1")
        suspend fun getCycleByStartDate(startDateMillis: Long): CyclesEntity?

        @Query("UPDATE cycle_logs SET cycleId = :newCycleId WHERE id = :logId")
        suspend fun linkLogToCycle(logId: Int, newCycleId: Int)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertCycle(cycle: CyclesEntity)

        @Query("UPDATE cycles SET start_date = :startDate, end_date = :endDate, periodLength = :periodLength WHERE id = :id")
        abstract suspend fun updateCycleDates(id: Int, startDate: Long, endDate: Long?, periodLength: Int?)

        @Query("DELETE FROM cycles WHERE id = :id")
        abstract suspend fun deleteCycle(id: Int)

    @Query("""
            SELECT * FROM cycles 
            WHERE :date >= start_date 
            AND (
                cycleLength IS NULL 
                OR :date < (start_date + (cycleLength * 86400000))
            )
            ORDER BY start_date DESC 
            LIMIT 1
        """)
    abstract suspend fun getCycleForDate(date: Long): CyclesEntity?

//        @Query(""" SELECT * FROM cycles WHERE :date >= start_date AND ( cycleLength IS NULL OR :date < (start_date + (cycleLength * 86400000)) ) ORDER BY start_date DESC LIMIT 1 """)
//        abstract suspend fun getCycleForDate(date: Long): CyclesEntity?

        @Query(""" SELECT * FROM cycles WHERE :date >= start_date AND end_date IS NOT NULL AND :date <= end_date ORDER BY start_date DESC LIMIT 1 """)
        suspend fun getCycleWithinDates(date: Long): CyclesEntity?


    @Query("SELECT * FROM cycles WHERE start_date <= :date ORDER BY start_date DESC LIMIT 1")
    suspend fun getLatestCycleBefore(date: Long): CyclesEntity?

        @Query("SELECT * FROM cycles ORDER BY start_date ASC")
        fun getCompletedCycles(): Flow<List<CyclesEntity>>

        @Query("UPDATE cycles SET periodLength = :periodLength, cycleLength = :cycleLength WHERE id = :cycleId")
        suspend fun updateCycleLengths(cycleId: Int, periodLength: Int, cycleLength: Int)

        @Query("SELECT * FROM cycle_logs WHERE cycleId = :cycleId AND flow IS NOT NULL AND flow != 'NONE'")
        fun getPeriodDaysForCycle(cycleId: Int): Flow<List<CycleLogsEntity>>

        @Query("SELECT * FROM cycle_logs WHERE flow IS NOT NULL AND flow != 'NONE' ORDER BY log_date DESC LIMIT 1")
        fun getLatestPeriodDay(): Flow<PopulatedDailyLog?>

        // 2. DAILY LOGS MANAGEMENT
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertCycleLog(log: CycleLogsEntity): Long

        @Query(""" UPDATE cycle_logs SET flow = :flow, spotting = :spotting, discharge = :discharge, collectionMethod = :collection WHERE id = :id """)
        abstract suspend fun updateLogDetails(id: Int, flow: FlowType?, spotting: SpottingType?, discharge: DischargeType?, collection: CollectionMethod?)

        @Query("DELETE FROM cycle_logs WHERE id = :logId")
        abstract suspend fun deleteCycleLog(logId: Int)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertMoods(moods: List<MoodLogsEntity>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertSymptoms(symptoms: List<SymptomLogsEntity>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertActivities(activities: List<ActivityLogsEntity>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertCravings(cravings: List<CravingLogsEntity>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertSex(sex: List<SexTypeLogsEntity>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract suspend fun insertEnergy(energy: List<EnergyLogsEntity>)

        @Query("DELETE FROM mood_logs WHERE log_id = :logId")
        abstract suspend fun clearMoods(logId: Int)

        @Query("DELETE FROM symptom_logs WHERE log_id = :logId")
        abstract suspend fun clearSymptoms(logId: Int)

        @Query("DELETE FROM activity_logs WHERE log_id = :logId")
        abstract suspend fun clearActivities(logId: Int)

        @Query("DELETE FROM craving_logs WHERE log_id = :logId")
        abstract suspend fun clearCravings(logId: Int)

        @Query("DELETE FROM sex_type_logs WHERE log_id = :logId")
        abstract suspend fun clearSex(logId: Int)

        @Query("DELETE FROM energy_logs WHERE log_id = :logId")
        abstract suspend fun clearEnergy(logId: Int)

        @Transaction
        @Query("SELECT * FROM cycle_logs WHERE log_date = :date")
        abstract fun getLogByDate(date: Long): Flow<PopulatedDailyLog?>

        @Transaction
        @Query("SELECT * FROM cycle_logs ORDER BY log_date DESC")
        abstract fun getAllHistory(): Flow<List<PopulatedDailyLog>>

        @Transaction
        open suspend fun saveFullLog(
            cycleId: Int?,
            date: Long,
            flow: FlowType?,
            spotting: SpottingType?,
            discharge: DischargeType?,
            collection: CollectionMethod?,
            moods: List<MoodType>,
            symptoms: List<SymptomType>,
            activities: List<ActivityType>,
            cravings: List<CravingType>,
            sex: List<SexType>,
            energy: List<Energy>
        ) {
            val logEntity = CycleLogsEntity(cycleId = cycleId, log_date = date, flow = flow, spotting = spotting, discharge = discharge, collectionMethod = collection)
            val newId = insertCycleLog(logEntity).toInt()

            if (moods.isNotEmpty()) insertMoods(moods.map { MoodLogsEntity(log_id = newId, moodType = it) })
            if (symptoms.isNotEmpty()) insertSymptoms(symptoms.map { SymptomLogsEntity(log_id = newId, symptomType = it) })
            if (activities.isNotEmpty()) insertActivities(activities.map { ActivityLogsEntity(log_id = newId, activityType = it) })
            if (cravings.isNotEmpty()) insertCravings(cravings.map { CravingLogsEntity(log_id = newId, cravingType = it) })
            if (sex.isNotEmpty()) insertSex(sex.map { SexTypeLogsEntity(log_id = newId, sexType = it) })
            if (energy.isNotEmpty()) insertEnergy(energy.map { EnergyLogsEntity(log_id = newId, energyType = it) })
        }

        @Transaction
        open suspend fun updateFullLog(
            logId: Int,
            flow: FlowType?,
            spotting: SpottingType?,
            discharge: DischargeType?,
            collection: CollectionMethod?,
            moods: List<MoodType>,
            symptoms: List<SymptomType>,
            activities: List<ActivityType>,
            cravings: List<CravingType>,
            sex: List<SexType>,
            energy: List<Energy>
        ) {
            updateLogDetails(logId, flow, spotting, discharge, collection)

            clearMoods(logId)
            clearSymptoms(logId)
            clearActivities(logId)
            clearCravings(logId)
            clearSex(logId)
            clearEnergy(logId)

            if (moods.isNotEmpty()) insertMoods(moods.map { MoodLogsEntity(log_id = logId, moodType = it) })
            if (symptoms.isNotEmpty()) insertSymptoms(symptoms.map { SymptomLogsEntity(log_id = logId, symptomType = it) })
            if (activities.isNotEmpty()) insertActivities(activities.map { ActivityLogsEntity(log_id = logId, activityType = it) })
            if (cravings.isNotEmpty()) insertCravings(cravings.map { CravingLogsEntity(log_id = logId, cravingType = it) })
            if (sex.isNotEmpty()) insertSex(sex.map { SexTypeLogsEntity(log_id = logId, sexType = it) })
            if (energy.isNotEmpty()) insertEnergy(energy.map { EnergyLogsEntity(log_id = logId, energyType = it) })
        }
    }