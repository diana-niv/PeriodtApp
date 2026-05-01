package com.example.periodtapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CyclesEntity::class,
        CycleLogsEntity::class,
        MoodLogsEntity::class,
        SymptomLogsEntity::class,
        ActivityLogsEntity::class,
        CravingLogsEntity::class,
        SexTypeLogsEntity::class,
        EnergyLogsEntity::class
    ],
    version = 3
)
@TypeConverters(Converters::class)
abstract class PeriodtDatabase : RoomDatabase() {

    abstract fun periodtDao(): PeriodtDao

    companion object {
        @Volatile
        private var Instance: PeriodtDatabase? = null

        fun getDatabase(context: Context): PeriodtDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room
                    .databaseBuilder(context, PeriodtDatabase::class.java, "periodt_database")
                    .fallbackToDestructiveMigration()
                    .build()
                Instance = instance
                return instance
            }
        }
    }
}