package com.example.comp90018.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionStatusCard(
    permission: PermissionType,
    status: PermissionStatus,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = when (status) {
        is PermissionStatus.Granted -> return
        is PermissionStatus.Denied -> "${permission.title} is denied. ${permission.purpose.replaceFirstChar { it.uppercase() }}."
        is PermissionStatus.Unavailable -> status.reason
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(detail, color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status is PermissionStatus.Denied && status.canAskAgain) {
                    OutlinedButton(onClick = onRetry) { Text("Try again") }
                }
                if ((status is PermissionStatus.Denied && !status.canAskAgain) ||
                    (status is PermissionStatus.Unavailable && status.settingsTarget != null)
                ) {
                    TextButton(onClick = onSettings) {
                        Text(if (status is PermissionStatus.Unavailable) "Open location settings" else "Open app settings")
                    }
                }
            }
        }
    }
}
