package com.example.periodtapp.ui.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.periodtapp.data.PeriodtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EditPeriodViewModel(
    private val repository: PeriodtRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            // Recalculate averages from existing DB data on startup
            repository.updateCycleAverages()
        }
    }

    // STATE
    private val _startDate = MutableStateFlow<LocalDate?>(null)
    val startDate: StateFlow<LocalDate?> = _startDate

    private val _endDate = MutableStateFlow<LocalDate?>(null)
    val endDate: StateFlow<LocalDate?> = _endDate

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // INITIALIZE
    fun loadCycle(cycleId: Int, passedDateMillis: Long) {
        viewModelScope.launch {
            if (cycleId != -1) {
                // EDIT MODE: Load existing cycle
                val cycle = repository.getCycleById(cycleId)
                if (cycle != null) {
                    _startDate.value = cycle.start_date.toLocalDate()
                    _endDate.value = cycle.end_date?.toLocalDate()
                }
            } else {
                // CREATE MODE: Start fresh on the clicked date
                val date = Instant.ofEpochMilli(passedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                _startDate.value = date
                _endDate.value = date.plusDays(4) // Default to 5 days
            }
            _isLoading.value = false
        }
    }

    // ACTIONS
    fun onDateClicked(date: LocalDate) {
        val start = _startDate.value
        val end = _endDate.value

        if (start == null) {
            _startDate.value = date
        } else {
            // Logic: Adjust start or end based on what was clicked
            if (date.isBefore(start)) {
                _startDate.value = date // Extend backwards
            } else {
                _endDate.value = date // Extend forwards or shorten
            }
        }
    }

    fun saveCycle(cycleId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Get the current StateFlow values
            val start = _startDate.value ?: return@launch
            val end = _endDate.value

            // Ensure end date is not before start date before saving.
            // This prevents "negative" days which break the cycle prediction math.
            if (end != null && end.isBefore(start)) {
                // If invalid, we stop here (you could also show an error message)
                return@launch
            }

            // Convert LocalDates to Epoch Millis for the Repository/DB
            val startMillis = start.toEpochMillis()
            val endMillis = end?.toEpochMillis()

            // 1. First, save the actual data (Create or Update)
            if (cycleId == -1) {
                repository.addCycle(startMillis, endMillis)
            } else {
                repository.updateCycle(cycleId, startMillis, endMillis)
            }

            // 2. Refresh the prediction math AFTER the save is committed.
            // This allows the engine to see the new dates and calculate your 19/29 day averages.
            repository.updateCycleAverages()

            // 3. Navigate back
            onComplete()
        }
    }

    fun deleteCycle(cycleId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (cycleId != -1) {
                repository.deleteCycle(cycleId)

                repository.updateCycleAverages()
            }
            onComplete()
        }
    }

    // HELPERS
    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    private fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}