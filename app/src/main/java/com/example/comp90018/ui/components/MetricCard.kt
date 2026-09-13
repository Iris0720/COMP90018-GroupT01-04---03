package com.example.comp90018.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
    }
}
