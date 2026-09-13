package com.example.comp90018.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.ui.components.MetricCard
import com.example.comp90018.ui.components.StatusChip
import com.example.comp90018.ui.theme.*

@Composable
fun TodayScreen(onStartActivity: () -> Unit) {
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
        InfoCard(Sky) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("18°", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Clear and mild", fontWeight = FontWeight.Bold)
                    Text("Great for a walk · Updated 12 min ago", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        SectionTitle("Wellbeing")
        InfoCard(Sand) {
            StatusChip("UNKNOWN", Color(0xFFFFE4A8), Ink)
            Text("Complete a quick check-in before a longer activity.", color = MaterialTheme.colorScheme.secondary)
        }
        SectionTitle("Recent activity")
        InfoCard(Color.White) {
            Text("Morning walk", fontWeight = FontWeight.Bold)
            Text("Yesterday · 3.2 km · 34 min · 4,180 steps", color = MaterialTheme.colorScheme.secondary)
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
