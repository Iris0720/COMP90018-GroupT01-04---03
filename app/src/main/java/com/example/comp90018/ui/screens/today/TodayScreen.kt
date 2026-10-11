package com.example.comp90018.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.domain.model.ActivityRecord
import com.example.comp90018.domain.model.ActivityType
import com.example.comp90018.domain.repository.HistoryState
import com.example.comp90018.health.HealthCondition
import java.text.DateFormat
import java.util.Date
import com.example.comp90018.ui.components.MetricCard
import com.example.comp90018.ui.components.StatusChip
import com.example.comp90018.ui.theme.*

@Composable
fun TodayScreen(
    historyState: HistoryState,
    condition: HealthCondition,
    onStartActivity: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Good afternoon", style = MaterialTheme.typography.headlineLarge)
        Text("Here is your outdoor-ready snapshot.", color = MaterialTheme.colorScheme.secondary)

        Column(
            Modifier.fillMaxWidth().background(ForestDark, RoundedCornerShape(26.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TODAY'S MOVEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBFE9CE))
                    Text("6,420", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("of 10,000 steps", color = Color.White.copy(alpha = .8f))
                }
                Text("64%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { .642f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF8EE2AB),
                trackColor = Color.White.copy(alpha = .2f)
            )
            Button(
                onClick = onStartActivity,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ForestDark),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Start an activity", fontWeight = FontWeight.Bold) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("42", "active min", Modifier.weight(1f))
            MetricCard("3.6", "km today", Modifier.weight(1f))
        }
        SectionTitle("Conditions")
        var weather by remember { mutableStateOf<MelbourneWeather?>(null) }
        var weatherUnavailable by remember { mutableStateOf(false) }
        var weatherRefreshKey by remember { mutableIntStateOf(0) }
        LaunchedEffect(weatherRefreshKey) {
            weatherUnavailable = false
            weather = try {
                fetchMelbourneWeather()
            } catch (_: Exception) {
                weatherUnavailable = true
                null
            }
        }
        InfoCard(Sky) {
            when {
                weather != null -> {
                    val currentWeather = weather!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${currentWeather.temperatureCelsius}°", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentWeather.summary, fontWeight = FontWeight.Bold)
                            Text(
                                "Melbourne · High ${currentWeather.highCelsius}° · Low ${currentWeather.lowCelsius}° · Updated ${currentWeather.updatedAt}",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text("Weather data: Open-Meteo", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
                weatherUnavailable -> {
                    Text("Melbourne weather is temporarily unavailable.", color = MaterialTheme.colorScheme.secondary)
                    TextButton(onClick = { weatherRefreshKey++ }) { Text("Try again") }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading Melbourne weather…", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        SectionTitle("Wellbeing")
        val isConditionGood = condition == HealthCondition.GOOD
        InfoCard(if (isConditionGood) Mint else Sand) {
            Text("TODAY'S CONDITION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            StatusChip(condition.name, if (isConditionGood) Color(0xFFBFE9CE) else Color(0xFFFFE4A8), ForestDark)
            Text(
                when (condition) {
                    HealthCondition.GOOD -> "No concerns reported. Listen to your body while moving."
                    HealthCondition.CAUTION -> "Consider a lighter activity and seek professional advice if concerned."
                    HealthCondition.UNKNOWN -> "Complete all questions to calculate a condition. Missing information never defaults to Good."
                },
                color = MaterialTheme.colorScheme.secondary
            )
        }
        SectionTitle("Recent activity")
        InfoCard(Color.White) {
            when (historyState) {
                HistoryState.Loading -> Text(
                    "Loading your latest activity…",
                    color = MaterialTheme.colorScheme.secondary
                )
                HistoryState.Empty -> Text(
                    "No saved activities yet. Start an activity to see it here.",
                    color = MaterialTheme.colorScheme.secondary
                )
                is HistoryState.Failure -> Text(
                    "Activity history is unavailable right now.",
                    color = MaterialTheme.colorScheme.secondary
                )
                is HistoryState.Success -> {
                    val latest = historyState.records
                        .filterIsInstance<ActivityRecord>()
                        .maxByOrNull { it.recordedAtEpochMillis }

                    if (latest == null) {
                        Text(
                            "No saved activities yet. Start an activity to see it here.",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(latest.activityType.displayName(), fontWeight = FontWeight.Bold)
                        Text(
                            listOfNotNull(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(latest.recordedAtEpochMillis)),
                                String.format("%.1f km", latest.distanceMetres / 1000.0),
                                "${latest.durationSeconds / 60} min",
                                latest.steps?.let { "$it steps" }
                            ).joinToString(" · "),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
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

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge)

private fun ActivityType.displayName(): String = when (this) {
    ActivityType.WALKING -> "Walk"
    ActivityType.RUNNING -> "Run"
    ActivityType.HIKING -> "Hike"
}
