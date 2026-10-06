package com.example.comp90018.ui.screens.profile

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.comp90018.ui.theme.Forest
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.Mint
import com.example.comp90018.ui.theme.Sand
import androidx.activity.compose.BackHandler
import androidx.compose.material3.OutlinedButton

// The theme has no dark amber for text on Sand, so this one is local.
private val SandText = Color(0xFF8A5A00)

@Composable
fun ProfileScreen(
    initialName: String = "Alex",
    initialStepGoal: Int = 10_000,
    initialUnits: String = "Metric",
    healthNote: String = "Prefer moderate-intensity outdoor sessions.",
    locationAllowed: Boolean = true,
    cameraAllowed: Boolean = true,
    activityRecognitionAllowed: Boolean = true,
    notificationsAllowed: Boolean = false,
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

    BackHandler {
        onDismiss()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "Profile",
                    style = type.headlineLarge,
                    color = ForestDark,
                )
                Text(
                    text = "Local preferences and permissions",
                    style = type.bodySmall,
                    color = mutedText,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = type.titleMedium,
                    color = Forest,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Editable fields
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
                        onValueChange = { input ->
                            stepGoalText = input.filter { it.isDigit() }.take(6)
                        },
                        singleLine = true,
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(text = " steps", style = fieldStyle)
                }
            }

            ProfileField(label = "Units", labelColor = mutedText) {
                // Tap to toggle between Metric and Imperial
                Text(
                    text = units,
                    style = fieldStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { units = if (units == "Metric") "Imperial" else "Metric" },
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Optional health note
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(
                text = "Optional health note",
                style = type.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "\u201C$healthNote\u201D",
                style = type.bodyMedium,
                color = mutedText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Stored locally on this device.",
                style = type.bodySmall,
                color = mutedText,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Permissions
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Permissions",
                style = type.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            PermissionRow("Location", locationAllowed)
            PermissionRow("Camera", cameraAllowed)
            PermissionRow("Activity recognition", activityRecognitionAllowed)
            PermissionRow("Notifications", notificationsAllowed)
        }

        Spacer(Modifier.height(24.dp))

        // Privacy card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Local-first privacy",
                    style = type.titleMedium,
                    color = colors.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Precise routes, meals and health notes stay on the device for the MVP.",
                    style = type.bodyMedium,
                    color = colors.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save button (uses the theme's primary / onPrimary by default)
        Button(
            onClick = {
                onSave(name.trim(), stepGoalText.toIntOrNull() ?: initialStepGoal, units)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Save changes", style = type.titleMedium)
        }

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Logout", style = type.titleMedium)
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    labelColor: Color,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun PermissionRow(label: String, allowed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StatusChip(allowed)
    }
}

@Composable
private fun StatusChip(allowed: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (allowed) Mint else Sand,
    ) {
        Text(
            text = if (allowed) "Allowed" else "Not allowed",
            style = MaterialTheme.typography.labelMedium,
            color = if (allowed) ForestDark else SandText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}