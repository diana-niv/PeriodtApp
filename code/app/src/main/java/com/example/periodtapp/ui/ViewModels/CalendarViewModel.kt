package com.example.periodtapp.ui.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.periodtapp.data.PeriodtRepository
import com.example.periodtapp.db.CyclePrediction
import com.example.periodtapp.db.CyclesEntity
import com.example.periodtapp.db.PopulatedDailyLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class CalendarDayType { NONE, ACTUAL_PERIOD, PREDICTED_PERIOD, PREDICTED_OVULATION, PREDICTED_FERTILE, PAST_OVULATION, PAST_FERTILE
}

class CalendarViewModel(private val repository: PeriodtRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.refreshAllPredictions()
        }
    }

    val prediction: StateFlow<CyclePrediction?> = repository.getPredictionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calendarDays: StateFlow<Map<LocalDate, CalendarDayType>> = combine(
        repository.allCycles, // Use raw cycles to get start dates
        prediction
    ) { actualCycles, pred ->
        val combinedMap = mutableMapOf<LocalDate, CalendarDayType>()
        val actualMap = mapCyclesToDays(actualCycles)
        val today = LocalDate.now()

        // 1. Fill Actual History (Solid Red)
        actualMap.forEach { (date, isPeriod) ->
            if (isPeriod) combinedMap[date] = CalendarDayType.ACTUAL_PERIOD
        }

        pred?.let { p ->
            val pStart = p.predictedPeriodStartDate.toLocalDate()
            val avgCycle = p.averageCycleLength.toLong()
            val avgPeriod = p.averagePeriodLength.toLong()
            val follicularLen = p.follicularPhaseLength.toLong()

            val firstPeriodDate = actualMap.keys.minOrNull() ?: today
            val cyclesBack = (java.time.temporal.ChronoUnit.DAYS.between(firstPeriodDate, pStart) / avgCycle).toInt() + 1

            for (i in -cyclesBack..6) {
                val dayOffset = avgCycle * i
                val streakStart = pStart.plusDays(dayOffset)
                val streakEnd = streakStart.plusDays(avgPeriod - 1)

                // --- 1. PERIOD PREDICTIONS (GHOST STREAKS) ---
                val isOverlap = actualMap.keys.any { actualDate ->
                    Math.abs(java.time.temporal.ChronoUnit.DAYS.between(actualDate, streakStart)) < 14
                }

                if (!isOverlap) {
                    var current = streakStart
                    while (!current.isAfter(streakEnd)) {
                        if (!combinedMap.containsKey(current) &&
                            current.isAfter(firstPeriodDate.minusDays(1)) &&
                            current.isBefore(today.plusMonths(6))) {
                            combinedMap[current] = CalendarDayType.PREDICTED_PERIOD
                        }
                        current = current.plusDays(1)
                    }
                }

                // --- 2. OVULATION & FERTILE WINDOW (BIOLOGICAL ALIGNMENT) ---
                // We find the 'anchor' for this cycle. If a real period exists nearby, we use its start.
                // Otherwise, we use the predicted start.
                val anchorDate = actualCycles.find {
                    Math.abs(java.time.temporal.ChronoUnit.DAYS.between(it.start_date.toLocalDate(), streakStart)) < 14
                }?.start_date?.toLocalDate() ?: streakStart

                val currentOvu = anchorDate.plusDays(follicularLen)
                val fStart = currentOvu.minusDays(5)
                val fEnd = currentOvu.plusDays(1)

                var currentF = fStart
                while (!currentF.isAfter(fEnd)) {
                    // Only show if not on a bleeding day and after the first ever period
                    if (combinedMap[currentF] != CalendarDayType.ACTUAL_PERIOD &&
                        currentF.isAfter(firstPeriodDate.minusDays(1))) {

                        if (combinedMap[currentF] != CalendarDayType.PREDICTED_PERIOD) {
                            val isPast = currentF.isBefore(today)
                            combinedMap[currentF] = if (currentF == currentOvu) {
                                if (isPast) CalendarDayType.PAST_OVULATION else CalendarDayType.PREDICTED_OVULATION
                            } else {
                                if (isPast) CalendarDayType.PAST_FERTILE else CalendarDayType.PREDICTED_FERTILE
                            }
                        }
                    }
                    currentF = currentF.plusDays(1)
                }
            }
        }
        combinedMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _selectedCycleId = MutableStateFlow<Int?>(null)
    val selectedCycleId: StateFlow<Int?> = _selectedCycleId

    private val _selectedCycle = MutableStateFlow<CyclesEntity?>(null)
    val selectedCycle: StateFlow<CyclesEntity?> = _selectedCycle

    private val _selectedLog = MutableStateFlow<PopulatedDailyLog?>(null)
    val selectedLog: StateFlow<PopulatedDailyLog?> = _selectedLog

    private val _cycleDayNum = MutableStateFlow<Int?>(null)
    val cycleDayNum: StateFlow<Int?> = _cycleDayNum

    private val _cycleDayNumToday = MutableStateFlow<Int?>(null)
    val cycleDayNumToday: StateFlow<Int?> = _cycleDayNumToday

    val cycles = repository.allCycles
    private var fetchJob: Job? = null

    val periodDays: StateFlow<Map<LocalDate, Boolean>> = repository.allCycles
        .map { mapCyclesToDays(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val logDataMap: StateFlow<Map<LocalDate, PopulatedDailyLog>> = repository.allHistory
        .map { it.associateBy { log -> log.log.log_date.toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /*fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            val dateMillis = date.toEpochMillis()
            val cycleId = repository.getCycleIdWithinDates(dateMillis)
            _selectedCycleId.value = cycleId

            if (cycleId != null) {
                val cycle = repository.getCycleById(cycleId)
                _selectedCycle.value = cycle
                if (cycle != null) {
                    val diff = dateMillis - cycle.start_date
                    val dayNum = TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
                    _cycleDayNum.value = if (dayNum > 0) dayNum else 1
                }

                val today = LocalDate.now()
                val todayMillis = today.toEpochMillis()
                _cycleDayNumToday.value = repository.getDayOfCycle(todayMillis, cycleId)
            } else {
                _cycleDayNum.value = null
                _cycleDayNumToday.value = null
            }

            repository.getLogForDate(dateMillis).collect { log ->
                _selectedLog.value = log
            }
        }
    }*/

    /*fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            val dateMillis = date.toEpochMillis()

            // TODO there are some difs here
            // 1. STRICT CHECK (For the Button):
            // Only find a cycle if we are strictly inside the period start/end dates.
            // This ensures the "Edit Period" button only shows when you click ON the period.
            val strictCycleId = repository.getCycleIdWithinDates(dateMillis)
            _selectedCycleId.value = strictCycleId

            // 2. BROAD CHECK (For the Wheel)
            // Use the strict cycle if we are in one.
            // Otherwise, use the NEW 'getLatestCycleBefore' to find the anchor cycle.
            val displayCycle = if (strictCycleId != null) {
                repository.getCycleById(strictCycleId)
            } else {
                repository.getLatestCycleBefore(dateMillis)
            }

            _selectedCycle.value = displayCycle

            // --- 3. ROBUST DAY CALCULATION ---
            val pred = prediction.value
            val avgLength = if (pred != null && pred.averageCycleLength > 0) pred.averageCycleLength else 28

            if (displayCycle != null) {
                // SCENARIO A: We have a past cycle to anchor to
                val diff = dateMillis - displayCycle.start_date
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(diff).toInt()

                if (strictCycleId != null) {
                    // Exact day (e.g., Day 3 of Period)
                    _cycleDayNum.value = daysElapsed + 1
                } else {
                    // Projected day (e.g., Day 45 = Day 17 of next cycle)
                    _cycleDayNum.value = (daysElapsed % avgLength) + 1
                }
            } else {
                // SCENARIO B: Date is BEFORE all history (or No Data exists)
                if (pred != null) {
                    // We have predictions, but clicked a date before the very first cycle.
                    // We project BACKWARDS from the predicted next period.
                    val diffFromFuture = pred.predictedPeriodStartDate.time - dateMillis
                    val daysBefore = TimeUnit.MILLISECONDS.toDays(diffFromFuture).toInt()

                    // Math to wrap backwards (e.g. 10 days before start of 28-day cycle = Day 18)
                    val remainder = daysBefore % avgLength
                    val backCalculatedDay = if (remainder == 0) 1 else (avgLength - remainder) + 1

                    _cycleDayNum.value = backCalculatedDay
                } else {
                    // SCENARIO C: ABSOLUTE NO DATA (First Launch)
                    // Fallback to Epoch time so the wheel spins based on a theoretical 28-day rhythm
                    val daysSinceEpoch = date.toEpochDay()
                    _cycleDayNum.value = ((daysSinceEpoch % 28) + 1).toInt()
                }
            }

            // --- 4. TODAY INDICATOR (Re-use logic) ---
            // We apply the same math to 'Today' so the little handle is always correct
            val today = LocalDate.now()
            val todayMillis = today.toEpochMillis()
            val todayCycle = repository.getLatestCycleBefore(todayMillis)

            if (todayCycle != null) {
                val diff = todayMillis - todayCycle.start_date
                val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                _cycleDayNumToday.value = (days % avgLength) + 1
            } else {
                // Fallback for Today on empty app
                _cycleDayNumToday.value = ((today.toEpochDay() % 28) + 1).toInt()
            }

            // Fetch Log Data
            repository.getLogForDate(dateMillis).collect { log ->
                _selectedLog.value = log
            }
        }
    }*/

    // In CalendarViewModel.kt

    // Replace/Complete your existing onDateSelected function:
    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            val dateMillis = date.toEpochMillis()

            // 1. STRICT CHECK (For Edit Button Visibility)
            val strictCycleId = repository.getCycleIdWithinDates(dateMillis)
            _selectedCycleId.value = strictCycleId

            // 2. BROAD CHECK (For the Wheel display anchor)
            val displayCycle = if (strictCycleId != null) {
                repository.getCycleById(strictCycleId)
            } else {
                repository.getLatestCycleBefore(dateMillis)
            }
            _selectedCycle.value = displayCycle

            // 3. DYNAMIC DAY CALCULATION
            val pred = prediction.value
            val avgLength = if (pred != null && pred.averageCycleLength > 0) pred.averageCycleLength else 28

            if (displayCycle != null) {
                // History exists. Calculate offset from the start of the cycle.
                val diff = dateMillis - displayCycle.start_date
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(diff).toInt()

                if (strictCycleId != null) {
                    _cycleDayNum.value = daysElapsed + 1
                } else {
                    _cycleDayNum.value = (daysElapsed % avgLength) + 1
                }
            } else {
                // Date is before history, project backwards from future prediction
                if (pred != null) {
                    val diffFromFuture = pred.predictedPeriodStartDate.time - dateMillis
                    val daysBefore = TimeUnit.MILLISECONDS.toDays(diffFromFuture).toInt()
                    val remainder = daysBefore % avgLength
                    val backCalculatedDay = if (remainder == 0) 1 else (avgLength - remainder) + 1
                    _cycleDayNum.value = backCalculatedDay
                } else {
                    // ABSOLUTE NO DATA (Brand new user or all deleted)
                    // We anchor "Day 1" to Today so the wheel is functional immediately
                    val today = LocalDate.now()
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, date)

                    // The modulo 28 rhythm ensures the wheel spins infinitely
                    val theoreticalDayIndex = ((daysDiff % 28) + 28) % 28
                    _cycleDayNum.value = (theoreticalDayIndex + 1).toInt()
                }
            }

            // TODAY INDICATOR
            val today = LocalDate.now()
            val todayMillis = today.toEpochMillis()
            val todayCycle = repository.getLatestCycleBefore(todayMillis)

            if (todayCycle != null) {
                val diff = todayMillis - todayCycle.start_date
                val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                _cycleDayNumToday.value = (days % avgLength) + 1
            } else {
                // Default: Today is Day 1 if no history exists
                _cycleDayNumToday.value = 1
            }

            // Fetch specific symptoms for the selected day
            repository.getLogForDate(dateMillis).collect { log ->
                _selectedLog.value = log
            }
        }
    }

    fun onPopupDismissed() {
        _selectedDate.value = null
        _selectedCycleId.value = null
        _selectedCycle.value = null
        _selectedLog.value = null
        _cycleDayNum.value = null
        fetchJob?.cancel()
    }

    private fun mapCyclesToDays(cycles: List<CyclesEntity>): Map<LocalDate, Boolean> {
        val periodMap = mutableMapOf<LocalDate, Boolean>()
        cycles.forEach { cycle ->
            val start = cycle.start_date.toLocalDate()
            val end = cycle.end_date?.toLocalDate() ?: LocalDate.now()

            var current = start
            while (!current.isAfter(end)) {
                periodMap[current] = true
                current = current.plusDays(1)
            }
        }
        return periodMap
    }


    fun deleteCycle(cycleId: Int) {
        viewModelScope.launch {
            repository.deleteCycle(cycleId)

            // Recalculate averages and refresh predictions to clear the blue dots
            repository.updateCycleAverages()
            repository.refreshAllPredictions()

            onPopupDismissed()
        }
    }

    // TIME CONVERTERS
    private fun LocalDate.toEpochMillis(): Long =
        this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun java.util.Date.toLocalDate(): LocalDate =
        this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}