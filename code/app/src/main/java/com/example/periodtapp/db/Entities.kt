package com.example.periodtapp.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 1. CYCLES TABLE (The Parent Cycle)
@Entity(tableName = "cycles")
data class CyclesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val start_date: Long,
    val end_date: Long? = null, // Nullable if the cycle is currently ongoing
    var periodLength: Int? = null, // The length of the period (bleeding days) for this specific cycle
    var cycleLength: Int? = null
)


// 2. DAILY LOGS (The Main Entry for a Day)
@Entity(
    tableName = "cycle_logs",
    foreignKeys = [
        ForeignKey(
            entity = CyclesEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE // If Cycle is deleted, delete all days in it
        )
    ],
    indices = [Index("cycleId")] // Speeds up looking for days in a specific cycle
)
data class CycleLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cycleId: Int? = null,
    val log_date: Long,

    // --- Merged Single-Choice Data ---
    // These are nullable (?) because a user might log a date without logging these specifics.
    val flow: FlowType? = null,
    val spotting: SpottingType? = null,
    val discharge: DischargeType? = null,
    val collectionMethod: CollectionMethod? = null
)

// 3. MULTI-CHOICE TABLES

// --- MOODS ---
@Entity(
    tableName = "mood_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE // If Daily Log is deleted, delete these moods
        )
    ],
    indices = [Index("log_id")]
)
data class MoodLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val moodType: MoodType
)

// --- SYMPTOMS ---
@Entity(
    tableName = "symptom_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class SymptomLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val symptomType: SymptomType
)

// --- ACTIVITIES ---
@Entity(
    tableName = "activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class ActivityLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val activityType: ActivityType
)

// --- CRAVINGS ---
@Entity(
    tableName = "craving_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class CravingLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val cravingType: CravingType
)

@Entity(
    tableName = "sex_type_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class SexTypeLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val sexType: SexType
)

@Entity(
    tableName = "energy_logs",
    foreignKeys = [
        ForeignKey(
            entity = CycleLogsEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class EnergyLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val log_id: Int,
    val energyType: Energy
)