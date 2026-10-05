package com.example.comp90018.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.permissions.PermissionController
import com.example.comp90018.permissions.PermissionStatus
import com.example.comp90018.permissions.PermissionType
import com.example.comp90018.permissions.SettingsTarget

@Composable
fun ProfileScreen(permissions: PermissionController, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Profile") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Guest user", fontWeight = FontWeight.Bold)
                Text("Daily step target  10,000")
                Text("Units  Metric")
                HorizontalDivider()
                Text("Permissions", fontWeight = FontWeight.Bold)
                PermissionRow(PermissionType.LOCATION, permissions)
                PermissionRow(PermissionType.ACTIVITY_RECOGNITION, permissions)
                PermissionRow(PermissionType.CAMERA, permissions)
                Text(
                    "Permissions are requested only when you use the feature that needs them. Activity and health data stays on this device by default.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}

@Composable
private fun PermissionRow(type: PermissionType, permissions: PermissionController) {
    val status = permissions.status(type)
    val stateLabel = when (status) {
        is PermissionStatus.Granted -> status.detail ?: "Allowed"
        is PermissionStatus.Denied -> "Not allowed"
        is PermissionStatus.Unavailable -> status.reason
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(type.title, fontWeight = FontWeight.Medium)
        Text(type.purpose.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
        Text(stateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        when (status) {
            is PermissionStatus.Granted -> Unit
            is PermissionStatus.Denied -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    if (status.canAskAgain) permissions.requestPermission(type) {}
                    else permissions.openSettings(type, status)
                }) {
                    Text(if (status.canAskAgain) "Request" else "App settings")
                }
            }
            is PermissionStatus.Unavailable -> if (status.settingsTarget != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { permissions.openSettings(type, status) }) {
                        Text(if (status.settingsTarget == SettingsTarget.LOCATION) "Location settings" else "Settings")
                    }
                }
            }
        }
    }
}
