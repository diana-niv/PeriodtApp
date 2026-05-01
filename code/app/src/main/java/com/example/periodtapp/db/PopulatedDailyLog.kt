package com.example.periodtapp.db

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedDailyLog(
    @Embedded val log: CycleLogsEntity,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val moods: List<MoodLogsEntity>,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val symptoms: List<SymptomLogsEntity>,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val activities: List<ActivityLogsEntity>,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val cravings: List<CravingLogsEntity>,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val sex: List<SexTypeLogsEntity>,

    @Relation(parentColumn = "id", entityColumn = "log_id")
    val energy: List<EnergyLogsEntity>
)
