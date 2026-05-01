package com.example.periodtapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.periodtapp.R
import com.example.periodtapp.ui.theme.BurgundySplash
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }
    Box(Modifier.fillMaxSize().background(BurgundySplash), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.periodt_logo),
                contentDescription = "Periodt Logo",
                modifier = Modifier.size(100.dp),
                tint = Color.Unspecified
            )
            Text("Periodt.", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
        }
    }
}
