package com.example.comp90018.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusChip(label: String, containerColor: Color, contentColor: Color) {
    Text(
        text = label,
        color = contentColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(containerColor, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
