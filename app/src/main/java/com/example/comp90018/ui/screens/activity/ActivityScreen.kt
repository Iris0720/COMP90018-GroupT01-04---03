package com.example.comp90018.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.model.SessionState
import com.example.comp90018.ui.components.MetricCard
import com.example.comp90018.ui.components.PrimaryButton
import com.example.comp90018.ui.theme.Forest
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.Mint
import com.example.comp90018.ui.theme.Sand

@Composable
fun ActivityScreen() {
    var state by remember { mutableStateOf(SessionState.SETUP) }
    var activity by remember { mutableStateOf("Walking") }
    var target by remember { mutableStateOf("30 min") }

    when (state) {
        SessionState.SETUP -> Page {
            Text("Move outside", style = MaterialTheme.typography.headlineLarge)
            Text("Choose an activity and a simple target. Tracking works offline.", color = MaterialTheme.colorScheme.secondary)
            SectionTitle("Activity type")
            ChoiceRow(listOf("Walking", "Running", "Hiking"), activity) { activity = it }
            SectionTitle("Target")
            ChoiceRow(listOf("30 min", "5 km", "Open"), target) { target = it }
            InfoCard(Mint) {
                Text("Ready to move?", style = MaterialTheme.typography.titleLarge)
                Text("$activity · $target", color = ForestDark)
                Text("GPS, pace and step placeholders are ready to connect to a tracking ViewModel.")
            }
            PrimaryButton("Start $activity") { state = SessionState.LIVE }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("View training history") }
        }
        SessionState.LIVE, SessionState.PAUSED -> LiveActivity(
            activity = activity,
            paused = state == SessionState.PAUSED,
            onPause = { state = if (state == SessionState.PAUSED) SessionState.LIVE else SessionState.PAUSED },
            onFinish = { state = SessionState.COMPLETE }
        )
        SessionState.COMPLETE -> SummaryScreen { state = SessionState.SETUP }
    }
}

@Composable
private fun LiveActivity(activity: String, paused: Boolean, onPause: () -> Unit, onFinish: () -> Unit) = Page {
    Text(if (paused) "Activity paused" else activity, style = MaterialTheme.typography.headlineLarge)
    Text(if (paused) "Take your time. Your session is safe." else "GPS signal ready · Tracking locally", color = MaterialTheme.colorScheme.secondary)
    Box(
        Modifier.fillMaxWidth().height(190.dp).background(if (paused) Sand else Mint, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("00:24:18", fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(if (paused) "PAUSED" else "IN PROGRESS", color = Forest, fontWeight = FontWeight.Bold)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("2.14", "km", Modifier.weight(1f))
        MetricCard("7:42", "pace /km", Modifier.weight(1f))
        MetricCard("3,012", "steps", Modifier.weight(1f))
    }
    PrimaryButton(if (paused) "Resume activity" else "Pause activity", onClick = onPause)
    OutlinedButton(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) { Text("Finish") }
}

@Composable
private fun SummaryScreen(onDone: () -> Unit) = Page {
    Text("Activity complete", style = MaterialTheme.typography.headlineLarge)
    InfoCard(Mint) {
        Text("Great work", style = MaterialTheme.typography.titleLarge)
        Text("Your demo session is ready for persistence wiring.")
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("28:16", "duration", Modifier.weight(1f))
        MetricCard("3.72", "kilometres", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("7:36", "avg pace", Modifier.weight(1f))
        MetricCard("4,614", "steps", Modifier.weight(1f))
    }
    PrimaryButton("Back to Activity", onClick = onDone)
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            )
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun InfoCard(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(color, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge)
