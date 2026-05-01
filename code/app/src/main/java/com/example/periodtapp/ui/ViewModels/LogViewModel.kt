package com.example.periodtapp.ui.ViewModels

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.periodtapp.data.PeriodtRepository
import com.example.periodtapp.db.ActivityType
import com.example.periodtapp.db.CollectionMethod
import com.example.periodtapp.db.CravingType
import com.example.periodtapp.db.DischargeType
import com.example.periodtapp.db.Energy
import com.example.periodtapp.db.FlowType
import com.example.periodtapp.db.MoodType
import com.example.periodtapp.db.SexType
import com.example.periodtapp.db.SpottingType
import com.example.periodtapp.db.SymptomType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class LogViewModel(private val repository: PeriodtRepository) : ViewModel() {

    // UI STATE

    private val _selectedFlow = MutableStateFlow<FlowType?>(null)
    val selectedFlow = _selectedFlow.asStateFlow()

    private val _selectedSpotting = MutableStateFlow<SpottingType?>(null)
    val selectedSpotting = _selectedSpotting.asStateFlow()

    private val _selectedDischarge = MutableStateFlow<DischargeType?>(null)
    val selectedDischarge = _selectedDischarge.asStateFlow()

    private val _selectedCollection = MutableStateFlow<CollectionMethod?>(null)
    val selectedCollection = _selectedCollection.asStateFlow()

    val selectedMoods = mutableStateListOf<MoodType>()
    val selectedSymptoms = mutableStateListOf<SymptomType>()
    val selectedActivities = mutableStateListOf<ActivityType>()
    val selectedCravings = mutableStateListOf<CravingType>()

    val selectedSex = mutableStateListOf<SexType>()
    val selectedEnergy = mutableStateListOf<Energy>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _loadedLogId = MutableStateFlow<Int?>(null)

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()


    // INITIALIZATION

    fun loadLog(date: LocalDate) {
        viewModelScope.launch {
            _selectedDate.value = date
            _isLoading.value = true
            resetState()
            val dateMillis = date.toEpochMillis()

            val existingLog = repository.getLogForDate(dateMillis).firstOrNull()

            if (existingLog != null) {
                _loadedLogId.value = existingLog.log.id
                _selectedFlow.value = existingLog.log.flow
                _selectedSpotting.value = existingLog.log.spotting
                _selectedDischarge.value = existingLog.log.discharge
                _selectedCollection.value = existingLog.log.collectionMethod

                selectedMoods.addAll(existingLog.moods.map { it.moodType })
                selectedSymptoms.addAll(existingLog.symptoms.map { it.symptomType })
                selectedActivities.addAll(existingLog.activities.map { it.activityType })
                selectedCravings.addAll(existingLog.cravings.map { it.cravingType })
                selectedSex.addAll(existingLog.sex.map { it.sexType })
                selectedEnergy.addAll(existingLog.energy.map { it.energyType })
            }
            _isLoading.value = false
        }
    }

    private fun resetState() {
        _loadedLogId.value = null
        _selectedFlow.value = null
        _selectedSpotting.value = null
        _selectedDischarge.value = null
        _selectedCollection.value = null
        selectedMoods.clear()
        selectedSymptoms.clear()
        selectedActivities.clear()
        selectedCravings.clear()
        selectedSex.clear()
        selectedEnergy.clear()
    }



    // ACTIONS (User Toggles)

    fun onFlowSelected(flow: FlowType) {
        // Allow deselecting flow by tapping it again
        _selectedFlow.value = if (_selectedFlow.value == flow) null else flow
    }


    fun onSpottingSelected(spotting: SpottingType?) {
        if (spotting == null) {
            _selectedSpotting.value = null
        } else {
            _selectedSpotting.value = if (_selectedSpotting.value == spotting) null else spotting
        }
    }

    fun onDischargeSelected(discharge: DischargeType?) {
        if (discharge == null) {
            _selectedDischarge.value = null
        } else {
            _selectedDischarge.value = if (_selectedDischarge.value == discharge) null else discharge
        }
    }

    fun onCollectionSelected(collection: CollectionMethod?) {
        if (collection == null) {
            _selectedCollection.value = null
        } else {
            _selectedCollection.value = if (_selectedCollection.value == collection) null else collection
        }
    }

    fun toggleMood(mood: MoodType) { if (selectedMoods.contains(mood)) selectedMoods.remove(mood) else selectedMoods.add(mood) }
    fun toggleSymptom(symptom: SymptomType) { if (selectedSymptoms.contains(symptom)) selectedSymptoms.remove(symptom) else selectedSymptoms.add(symptom) }
    fun toggleActivity(activity: ActivityType) { if (selectedActivities.contains(activity)) selectedActivities.remove(activity) else selectedActivities.add(activity) }
    fun toggleCraving(craving: CravingType) { if (selectedCravings.contains(craving)) selectedCravings.remove(craving) else selectedCravings.add(craving) }
    fun toggleSex(sex: SexType) { if (selectedSex.contains(sex)) selectedSex.remove(sex) else selectedSex.add(sex) }
    fun toggleEnergy(energy: Energy) { if (selectedEnergy.contains(energy)) selectedEnergy.remove(energy) else selectedEnergy.add(energy) }

    // SAVE
    fun saveLog(date: LocalDate, onSaved: () -> Unit) {
        viewModelScope.launch {
            // 1. Prepare Data
            repository.updateCycleAverages()
            val dateMillis = date.toEpochMillis()
            val currentFlow = _selectedFlow.value
            val hasFlow = currentFlow != null && currentFlow != FlowType.NO_PERIOD

            // This is the cycle currently associated with this date (if any)
            var cycleId = repository.getCycleIdWithinDates(dateMillis)
            val logIdToUpdate = _loadedLogId.value

            // User Logged a Period (Flow exists)

            if (hasFlow) {
                // Did a cycle end yesterday?
                val yesterdayMillis = date.minusDays(1).toEpochMillis()
                val cycleEndingYesterday = repository.getCycleByEndDate(yesterdayMillis)

                val tomorrowMillis = date.plusDays(1).toEpochMillis()
                val cycleStartingTomorrow = repository.getCycleByStartDate(tomorrowMillis)

                if (cycleEndingYesterday != null) {
                    // Extend the previous cycle to include today
                    repository.updateCycle(
                        cycleEndingYesterday.id,
                        cycleEndingYesterday.start_date,
                        dateMillis // New End Date is Today
                    )
                    // Link this log to the previous cycle
                    cycleId = cycleEndingYesterday.id
                } else if (cycleStartingTomorrow != null) {
                    repository.updateCycle(
                        cycleStartingTomorrow.id,
                        dateMillis, // New Start Date is Today
                        cycleStartingTomorrow.end_date
                    )
                    cycleId = cycleStartingTomorrow.id
                }
                else if (cycleId == null) {
                    // no previous cycle & No current cycle -> Start a NEW Default Cycle
                    repository.createDefaultCycle(startDate = dateMillis)
                    // Fetch the ID of the brand new cycle to link the log
                    cycleId = repository.getCycleIdForDate(dateMillis)
                }
            }

            // User Removed Period (No Flow)
            else if (!hasFlow && cycleId != null) {
                // The user had a period logged here, but now removed it.
                // We need to shrink the cycle boundaries.

                val cycle = repository.getCycleById(cycleId)

                if (cycle != null) {
                    val cycleStartDate = LocalDate.ofEpochDay(cycle.start_date / (24 * 60 * 60 * 1000) + 1)
                    val cycleEndDate = cycle.end_date?.let {
                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000) + 1)
                    }

                    when {
                        //  Deleting the START date -> Shift start forward
                        date.isEqual(cycleStartDate) -> {
                            val newStartDate = cycleStartDate.plusDays(1)

                            // If new start is after end, the cycle is invalid -> Delete it
                            if (cycleEndDate != null && newStartDate.isAfter(cycleEndDate)) {
                                repository.deleteCycle(cycleId)
                            } else {
                                repository.updateCycle(
                                    cycleId,
                                    newStartDate.toEpochMillis(),
                                    cycle.end_date
                                )
                                cycleId = repository.getCycleIdForDate(dateMillis) // TODO: null? not working, but maybe the linking of logs to cycles is not even needed
                            }
                        }

                        // Deleting the END date -> Shift end backward
                        cycleEndDate != null && date.isEqual(cycleEndDate) -> {
                            val newEndDate = cycleEndDate.minusDays(1)

                            // If new end is before start, invalid -> Delete it
                            if (newEndDate.isBefore(cycleStartDate)) {
                                repository.deleteCycle(cycleId)
                            } else {
                                repository.updateCycle(
                                    cycleId,
                                    cycle.start_date,
                                    newEndDate.toEpochMillis()
                                )
                            }
                        }
                    }
                }
            }

            // SAVE THE LOG
            if (logIdToUpdate != null) {
                repository.updateDailyLog(
                    logId = logIdToUpdate,
                    flow = _selectedFlow.value,
                    spotting = _selectedSpotting.value,
                    discharge = _selectedDischarge.value,
                    collection = _selectedCollection.value,
                    moods = selectedMoods.toList(),
                    symptoms = selectedSymptoms.toList(),
                    activities = selectedActivities.toList(),
                    cravings = selectedCravings.toList(),
                    sex = selectedSex.toList(),
                    energy = selectedEnergy.toList()
                )

                // Manage Linkage
                if (cycleId != null) {
                    repository.linkLogToCycle(logIdToUpdate, cycleId)
                } /*else {
                    // If cycleId is null (e.g. flow removed), verify if we need to UNLINK in DB
                    // (Your linkLogToCycle might handle this, or you might need a unlink function)
                    repository.linkLogToCycle(logIdToUpdate, 0) // Or null, depending on your DAO
                } */
            } else {
                repository.createDailyLog(
                    cycleId = cycleId,
                    date = dateMillis,
                    flow = _selectedFlow.value,
                    spotting = _selectedSpotting.value,
                    discharge = _selectedDischarge.value,
                    collection = _selectedCollection.value,
                    moods = selectedMoods.toList(),
                    symptoms = selectedSymptoms.toList(),
                    activities = selectedActivities.toList(),
                    cravings = selectedCravings.toList(),
                    sex = selectedSex.toList(),
                    energy = selectedEnergy.toList()
                )
            }

            onSaved()
        }
    }
    // Helper to convert LocalDate to milliseconds since epoch
    private fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}