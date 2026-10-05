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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comp90018.tracking.ActivityTrackingState
import com.example.comp90018.tracking.TrackingViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.roundToInt
import android.os.Build
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.comp90018.tracking.GpxExporter

@Composable
fun ActivityScreen(
    trackingViewModel: TrackingViewModel = viewModel()
) {
    val uiState = trackingViewModel.uiState

    val context = LocalContext.current

    val targetType = when {
        uiState.target.endsWith(" min") -> "Time"
        uiState.target.endsWith(" km") -> "Distance"
        else -> "Open"
    }

    var distanceInput by rememberSaveable {
        mutableStateOf(
            if (uiState.target.endsWith(" km")) {
                uiState.target.removeSuffix(" km")
            } else {
                "5"
            }
        )
    }

    val normalizedDistance = distanceInput.trim().replace(',', '.')
    val distanceValue = normalizedDistance.toDoubleOrNull()

// Allow 0.1–1000 km, with up to two decimal places.
    val distanceValid =
        normalizedDistance.matches(
            Regex("""\d+(\.\d{1,2})?""")
        ) &&
                distanceValue != null &&
                distanceValue.isFinite() &&
                distanceValue in 0.1..1000.0

    val canStart = targetType != "Distance" || distanceValid

    val displayedTarget = if (targetType == "Distance") {
        if (distanceValid) "$normalizedDistance km"
        else "Enter a valid distance"
    } else {
        uiState.target
    }

    // Start after the user responds to the physical activity request.
    // If denied, the timer and permitted GPS tracking still work.
    val stepPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            trackingViewModel.start()
        }

    fun requestStepsOrStart() {
        val needsStepPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) != PackageManager.PERMISSION_GRANTED

        if (needsStepPermission) {
            stepPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            trackingViewModel.start()
        }
    }

    // Once the location request finishes, check the step permission.
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            requestStepsOrStart()
        }

    fun startActivity() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            requestStepsOrStart()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    when (uiState.phase) {
        SessionState.SETUP -> Page {
            Text(
                "Ready to move?",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                "Choose an activity and a simple target.",
                color = MaterialTheme.colorScheme.secondary
            )

            SectionTitle("Activity type")

            ChoiceRow(
                options = listOf("Walking", "Running", "Hiking"),
                selected = uiState.activity,
                onSelect = trackingViewModel::selectActivity
            )

            SectionTitle("Target type")

            ChoiceRow(
                options = listOf("Time", "Distance", "Open"),
                selected = targetType,
                onSelect = { type ->
                    when (type) {
                        "Time" -> {
                            if (targetType != "Time") {
                                trackingViewModel.selectTarget("30 min")
                            }
                        }

                        "Distance" -> {
                            if (targetType != "Distance") {
                                trackingViewModel.selectTarget(
                                    if (distanceValid) {
                                        "$normalizedDistance km"
                                    } else {
                                        "5 km"
                                    }
                                )
                            }
                        }

                        "Open" -> {
                            trackingViewModel.selectTarget("Open")
                        }
                    }
                }
            )

            when (targetType) {
                "Time" -> {
                    TargetDropdown(
                        label = "Time goal",
                        options = listOf(
                            "15 min",
                            "30 min",
                            "45 min",
                            "60 min"
                        ),
                        selected = uiState.target,
                        onSelect = trackingViewModel::selectTarget
                    )
                }

                "Distance" -> {
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = {
                            distanceInput = it
                        },
                        label = {
                            Text("Distance goal")
                        },
                        suffix = {
                            Text("km")
                        },
                        placeholder = {
                            Text("For example, 2.5")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        isError = !distanceValid,
                        supportingText = {
                            Text(
                                if (distanceValid) {
                                    "Enter 0.1–1000 km"
                                } else {
                                    "Use 0.1–1000 km, with up to 2 decimal places"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "Open" -> {
                    Text(
                        text = "Track without a time or distance target.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            InfoCard(Mint) {
                Text(
                    "Ready to move?",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "${uiState.activity} · $displayedTarget",
                    color = ForestDark
                )

                Text("Timer and GPS tracking are ready.")
            }

            Button(
                onClick = {
                    if (canStart) {
                        // Commit the input before requesting permissions.
                        if (targetType == "Distance") {
                            trackingViewModel.selectTarget(
                                "$normalizedDistance km"
                            )
                        }

                        startActivity()
                    }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestDark,
                    contentColor = Color.White
                )
            ) {
                Text("Start ${uiState.activity}")
            }

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Training history — coming soon")
            }
        }

        SessionState.LIVE,
        SessionState.PAUSED -> LiveActivity(
            uiState = uiState,
            onPauseResume = {
                if (uiState.phase == SessionState.PAUSED) {
                    trackingViewModel.resume()
                } else {
                    trackingViewModel.pause()
                }
            },
            onFinish = trackingViewModel::finish
        )

        SessionState.COMPLETE -> SummaryScreen(
            uiState = uiState,
            onDone = trackingViewModel::reset
        )
    }
}

@Composable
private fun LiveActivity(
    uiState: ActivityTrackingState,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit
) {
    val paused = uiState.phase == SessionState.PAUSED

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // Allow scrolling on shorter screens.
        val compactHeight = this.maxHeight < 600.dp

        val contentModifier =
            if (compactHeight) {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxSize()
            }

        Column(
            modifier = contentModifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.activity,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = if (paused) "Paused" else "In progress",
                    color = Forest,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = if (paused) {
                    "Tracking paused"
                } else {
                    uiState.gpsStatus
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (paused) Sand else Mint,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.formattedTime,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = uiState.target,
                    color = ForestDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.hasFixedGoal) {
                LinearProgressIndicator(
                    progress = {
                        uiState.goalProgress
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = ForestDark,
                    trackColor = Mint
                )
            }

            Text(
                text = uiState.goalProgressText,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.goalReached) {
                    ForestDark
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    formatDistance(uiState.distanceMetres),
                    "km",
                    Modifier.weight(1f)
                )

                MetricCard(
                    formatPace(uiState.averagePaceSecondsPerKm),
                    "avg min/km",
                    Modifier.weight(1f)
                )

                MetricCard(
                    if (uiState.hasStepReading) {
                        uiState.sessionSteps.toString()
                    } else {
                        "—"
                    },
                    "steps",
                    Modifier.weight(1f)
                )
            }

            if (!uiState.hasStepReading) {
                Text(
                    text = uiState.stepStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (compactHeight) {
                RoutePreview(
                    segments = uiState.routeSegments,
                    chartHeight = 160.dp
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val routeHeight =
                        (this.maxHeight - 80.dp).coerceAtLeast(60.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        RoutePreview(
                            segments = uiState.routeSegments,
                            chartHeight = routeHeight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (paused) "Resume" else "Pause")
                }

                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Finish")
                }
            }
        }
    }
}

@Composable
private fun SummaryScreen(
    uiState: ActivityTrackingState,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var exportError by remember {
        mutableStateOf<String?>(null)
    }

    val summary = uiState.finishedSummary

    val hasRoute = summary
        ?.routeSegments
        ?.any { segment ->
            segment.isNotEmpty()
        } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Activity complete",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "${uiState.activity} · ${uiState.target}",
            color = ForestDark,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Session ended · Not saved yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                uiState.formattedTime,
                "active duration",
                Modifier.weight(1f)
            )

            MetricCard(
                formatDistance(uiState.distanceMetres),
                "kilometres",
                Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                formatPace(uiState.averagePaceSecondsPerKm),
                "avg min/km",
                Modifier.weight(1f)
            )

            MetricCard(
                if (uiState.hasStepReading) {
                    uiState.sessionSteps.toString()
                } else {
                    "—"
                },
                "recorded steps",
                Modifier.weight(1f)
            )
        }

        RoutePreview(
            segments = uiState.routeSegments,
            completed = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            fillAvailableHeight = true
        )

        OutlinedButton(
            onClick = {
                val finishedSummary = summary

                if (finishedSummary == null) {
                    exportError = "Activity summary is unavailable."
                } else {
                    val result = GpxExporter.exportAndShare(
                        context = context,
                        summary = finishedSummary
                    )

                    exportError = result.exceptionOrNull()?.message
                }
            },
            enabled = hasRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Save or share route")
        }

        if (!hasRoute) {
            Text(
                text = "No GPS route is available to export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        exportError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        PrimaryButton(
            text = "Back to Activity",
            onClick = onDone
        )
    }
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


private fun formatDistance(distanceMetres: Float): String {
    val distanceKilometres = distanceMetres / 1_000f

    return String.format(
        Locale.getDefault(),
        "%.2f",
        distanceKilometres
    )
}

private fun formatPace(paceSecondsPerKm: Double?): String {
    if (paceSecondsPerKm == null ||
        !paceSecondsPerKm.isFinite() ||
        paceSecondsPerKm <= 0.0
    ) {
        return "—"
    }

    val totalSeconds = paceSecondsPerKm.roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(
        Locale.getDefault(),
        "%d:%02d",
        minutes,
        seconds
    )
}
@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(label)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = {
                    Text(label)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}