package com.example.periodtapp.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromFlowType(value: FlowType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFlowType(value: String?): FlowType? {
        return value?.let { FlowType.valueOf(it) }
    }

    @TypeConverter
    fun fromSpottingType(value: SpottingType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSpottingType(value: String?): SpottingType? {
        return value?.let { SpottingType.valueOf(it) }
    }

    @TypeConverter
    fun fromDischargeType(value: DischargeType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDischargeType(value: String?): DischargeType? {
        return value?.let { DischargeType.valueOf(it) }
    }

    @TypeConverter
    fun fromCollectionMethod(value: CollectionMethod?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCollectionMethod(value: String?): CollectionMethod? {
        return value?.let { CollectionMethod.valueOf(it) }
    }

    @TypeConverter
    fun fromMoodType(value: MoodType): String {
        return value.name
    }

    @TypeConverter
    fun toMoodType(value: String): MoodType {
        return MoodType.valueOf(value)
    }

    @TypeConverter
    fun fromSymptomType(value: SymptomType): String {
        return value.name
    }

    @TypeConverter
    fun toSymptomType(value: String): SymptomType {
        return SymptomType.valueOf(value)
    }

    @TypeConverter
    fun fromActivityType(value: ActivityType): String {
        return value.name
    }

    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return ActivityType.valueOf(value)
    }

    @TypeConverter
    fun fromCravingType(value: CravingType): String {
        return value.name
    }

    @TypeConverter
    fun toCravingType(value: String): CravingType {
        return CravingType.valueOf(value)
    }

    @TypeConverter
    fun fromSexType(value: SexType): String {
        return value.name
    }

    @TypeConverter
    fun toSexType(value: String): SexType {
        return SexType.valueOf(value)
    }

    @TypeConverter
    fun fromEnergy(value: Energy): String {
        return value.name
    }

    @TypeConverter
    fun toEnergy(value: String): Energy {
        return Energy.valueOf(value)
    }
}