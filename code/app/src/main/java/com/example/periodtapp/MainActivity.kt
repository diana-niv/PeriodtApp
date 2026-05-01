package com.example.periodtapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.periodtapp.ui.PeriodtApp
import com.example.periodtapp.ui.theme.PeriodtAppTheme

//test
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PeriodtAppTheme {
                PeriodtApp()
            }
        }
    }
}