package com.example.comp90018.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Guest user", fontWeight = FontWeight.Bold)
                Text("Daily step target  10,000")
                Text("Units  Metric")
                HorizontalDivider()
                Text("Permissions", fontWeight = FontWeight.Bold)
                Text("Location  Not requested")
                Text("Activity recognition  Not requested")
                Text("Camera  Not requested")
                Text("Your activity and health data stays on this device by default.", fontSize = 12.sp)
            }
        }
    )
}
