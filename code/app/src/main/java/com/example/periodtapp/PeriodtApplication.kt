package com.example.periodtapp

import android.app.Application
import com.example.periodtapp.db.PeriodtDatabase// FIX: Changed .repository to .data to match your project structure
import com.example.periodtapp.data.PeriodtRepository

class PeriodtApplication : Application() {

    val repository: PeriodtRepository by lazy {
        val database = PeriodtDatabase.getDatabase(this)
        PeriodtRepository(database.periodtDao())
    }
}