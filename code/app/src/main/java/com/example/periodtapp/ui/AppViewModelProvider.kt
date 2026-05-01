package com.example.periodtapp.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.periodtapp.PeriodtApplication
import com.example.periodtapp.ui.ViewModels.CalendarViewModel
import com.example.periodtapp.ui.ViewModels.EditPeriodViewModel
import com.example.periodtapp.ui.ViewModels.LogViewModel


object AppViewModelProvider {

    val Factory = viewModelFactory {

        initializer {
            CalendarViewModel(periodtApplication().repository)
        }

        initializer {
            EditPeriodViewModel(periodtApplication().repository)
        }

        initializer {
            LogViewModel(periodtApplication().repository)
        }

    }
}

fun CreationExtras.periodtApplication(): PeriodtApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PeriodtApplication)
