package com.example.comp90018.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comp90018.permissions.PermissionController
import com.example.comp90018.permissions.PermissionStatus
import com.example.comp90018.permissions.PermissionType
import com.example.comp90018.permissions.SettingsTarget
import com.example.comp90018.ui.theme.Forest
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.Mint
import com.example.comp90018.ui.theme.Sand

private val SandText = Color(0xFF8A5A00)

@Composable
fun ProfileScreen(
    permissions: PermissionController,
    initialName: String = "Alex",
    initialStepGoal: Int = 10_000,
    initialUnits: String = "metric",
    healthNote: String = "Prefer moderate-intensity outdoor sessions.",
    onSave: (name: String, stepGoal: Int, units: String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val mutedText = colors.onSurface.copy(alpha = 0.7f)

    var name by remember { mutableStateOf(initialName) }
    var stepGoalText by remember { mutableStateOf(initialStepGoal.toString()) }
    var units by remember { mutableStateOf(initialUnits) }
    val fieldStyle: TextStyle = type.titleMedium.copy(color = ForestDark)

    BackHandler(onBack = onDismiss)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Profile", style = type.headlineLarge, color = ForestDark)
                Text("Account, preferences and permissions", style = type.bodySmall, color = mutedText)
            }
            Box(
                modifier = Modifier.size(44.dp).background(colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(name.take(1).uppercase(), style = type.titleMedium, color = Forest)
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.padding(horizontal = 26.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ProfileField(label = "Display name", labelColor = mutedText) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    singleLine = true,
                    textStyle = fieldStyle,
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ProfileField(label = "Daily step goal", labelColor = mutedText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = stepGoalText,
                        onValueChange = { input -> stepGoalText = input.filter { it.isDigit() }.take(6) },
                        singleLine = true,
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(" steps", style = fieldStyle)
                }
            }
            ProfileField(label = "Units", labelColor = mutedText) {
                Text(
                    text = if (units.equals("metric", ignoreCase = true)) "Metric" else "Imperial",
                    style = fieldStyle,
                    modifier = Modifier.fillMaxWidth().clickable {
                        units = if (units.equals("metric", ignoreCase = true)) "imperial" else "metric"
                    },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Text("Optional health note", style = type.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("“$healthNote”", style = type.bodyMedium, color = mutedText)
            Spacer(Modifier.height(6.dp))
            Text("Stored locally on this device.", style = type.bodySmall, color = mutedText)
        }

        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Permissions", style = type.titleLarge, color = colors.onSurface)
            PermissionRow(PermissionType.LOCATION, permissions)
            PermissionRow(PermissionType.ACTIVITY_RECOGNITION, permissions)
            PermissionRow(PermissionType.CAMERA, permissions)
            PermissionRow(PermissionType.NOTIFICATIONS, permissions)
            Text(
                "Permissions are requested only when you use the feature that needs them.",
                style = type.bodySmall,
                color = mutedText
            )
        }

        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Account privacy", style = type.titleMedium, color = colors.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your account keeps profile settings and saved health records synced.",
                    style = type.bodyMedium,
                    color = colors.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val goal = stepGoalText.toIntOrNull() ?: initialStepGoal
                onSave(name.trim(), goal, units)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Save changes", style = type.titleMedium)
        }
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Logout", style = type.titleMedium)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back")
        }
    }
}

@Composable
private fun ProfileField(label: String, labelColor: Color, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun PermissionRow(type: PermissionType, permissions: PermissionController) {
    val status = permissions.status(type)
    val statusText = when (status) {
        is PermissionStatus.Granted -> status.detail ?: "Allowed"
        is PermissionStatus.Denied -> "Not allowed"
        is PermissionStatus.Unavailable -> status.reason
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(type.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            if (status is PermissionStatus.Granted) StatusChip(true) else StatusChip(false)
        }
        Text(type.purpose.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.bodySmall)
        Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        when (status) {
            is PermissionStatus.Granted -> if (status.detail != "Not required on this Android version") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { permissions.openSettings(type, status) }) { Text("Change permission") }
                }
            }
            is PermissionStatus.Denied -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    if (status.canAskAgain) permissions.requestPermission(type) {}
                    else permissions.openSettings(type, status)
                }) { Text(if (status.canAskAgain) "Request" else "App settings") }
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

@Composable
private fun StatusChip(allowed: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = if (allowed) Mint else Sand) {
        Text(
            text = if (allowed) "Allowed" else "Not allowed",
            style = MaterialTheme.typography.labelMedium,
            color = if (allowed) ForestDark else SandText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
