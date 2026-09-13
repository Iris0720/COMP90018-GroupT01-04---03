package com.example.comp90018.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.model.WellbeingStatus
import com.example.comp90018.ui.components.PrimaryButton
import com.example.comp90018.ui.components.StatusChip
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.Mint
import com.example.comp90018.ui.theme.Sand
import com.example.comp90018.ui.theme.Sky

@Composable
fun HealthScreen() {
    var status by remember { mutableStateOf(WellbeingStatus.UNKNOWN) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Health", style = MaterialTheme.typography.headlineLarge)
        Text("A quick, non-diagnostic check-in for planning your activity.", color = MaterialTheme.colorScheme.secondary)
        InfoCard(if (status == WellbeingStatus.GOOD) Mint else Sand) {
            Text("TODAY'S CONDITION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            StatusChip(
                label = status.name,
                containerColor = if (status == WellbeingStatus.GOOD) Color(0xFFBFE9CE) else Color(0xFFFFE4A8),
                contentColor = ForestDark
            )
            Text(
                if (status == WellbeingStatus.GOOD) "No concerns reported. Listen to your body while moving."
                else "No check-in yet. Missing information never defaults to Good."
            )
        }
        Text("Health check-in", style = MaterialTheme.typography.titleLarge)
        InfoCard(Color.White) {
            Text("How are you feeling?", fontWeight = FontWeight.Bold)
            Text("Mood · Breathing · Fatigue", color = MaterialTheme.colorScheme.secondary)
            PrimaryButton(if (status == WellbeingStatus.UNKNOWN) "Start check-in" else "Update check-in") {
                status = WellbeingStatus.GOOD
            }
        }
        Text("Meal log", style = MaterialTheme.typography.titleLarge)
        InfoCard(Sky) {
            Text("Add a meal", fontWeight = FontWeight.Bold)
            Text("Use a photo when online or enter meal details manually.", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Take photo") }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Enter manually") }
            }
        }
        Text(
            "This app provides general wellbeing information only. It does not diagnose or treat medical conditions.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun InfoCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(color, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}
