package com.example.comp90018.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.comp90018.tracking.ActivitySaveState
import com.example.comp90018.domain.model.ActivityRecord
import com.example.comp90018.domain.repository.HistoryState
import java.text.DateFormat
import java.util.Date

@Composable
fun ActivityScreen(
    trackingViewModel: TrackingViewModel = viewModel()
) {
    val uiState = trackingViewModel.uiState

    var showHistory by rememberSaveable {
        mutableStateOf(false)
    }

    if (showHistory) {
        TrainingHistoryScreen(
            historyState = trackingViewModel.historyState,
            deletingRecordIds = trackingViewModel.deletingRecordIds,
            deleteError = trackingViewModel.historyDeleteError,
            onBack = {
                showHistory = false
            },
            onRetry = trackingViewModel::loadHistory,
            onDelete = trackingViewModel::deleteHistoryRecords,
            onDismissDeleteError = trackingViewModel::clearHistoryDeleteError
        )
        return
    }

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

            Button(
                onClick = {
                    trackingViewModel.loadHistory()
                    showHistory = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mint,
                    contentColor = ForestDark
                )
            ) {
                Text(
                    text = "Training History",
                    fontWeight = FontWeight.Bold
                )
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
            onDone = trackingViewModel::reset,
            onViewHistory = {
                trackingViewModel.loadHistory()
                showHistory = true
            }
        )
    }
}

@Composable
private fun TrainingHistoryScreen(
    historyState: HistoryState,
    deletingRecordIds: Set<String>,
    deleteError: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (Set<String>) -> Unit,
    onDismissDeleteError: () -> Unit
) {
    var managingRecords by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedRecordIds by remember {
        mutableStateOf(emptySet<String>())
    }

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var deleteSubmitted by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(deletingRecordIds, deleteError, deleteSubmitted) {
        if (
            deleteSubmitted &&
            deletingRecordIds.isEmpty()
        ) {
            if (deleteError == null) {
                managingRecords = false
                selectedRecordIds = emptySet()
            }
            deleteSubmitted = false
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
            },
            title = {
                Text("Delete selected activities?")
            },
            text = {
                Text(
                    "${selectedRecordIds.size} selected " +
                            "${if (selectedRecordIds.size == 1) "activity" else "activities"} " +
                            "will be permanently removed from your training history."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteSubmitted = true
                        onDelete(selectedRecordIds)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = ForestDark
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title = {
                Text("Unable to delete activity")
            },
            text = {
                Text(deleteError)
            },
            confirmButton = {
                TextButton(onClick = onDismissDeleteError) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // The history list scrolls independently while the return button stays visible.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 16.dp
            )
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Training history",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    TextButton(
                        onClick = {
                            managingRecords = !managingRecords
                            selectedRecordIds = emptySet()
                        },
                        enabled = deletingRecordIds.isEmpty()
                    ) {
                        Text(
                            text = if (managingRecords) "Cancel" else "Manage records",
                            color = ForestDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            when (historyState) {
                HistoryState.Loading -> {
                    item {
                        CircularProgressIndicator()
                    }
                    item {
                        Text("Loading saved activities...")
                    }
                }

                HistoryState.Empty -> {
                    item {
                        Text("No saved activities yet.")
                    }
                }

                is HistoryState.Failure -> {
                    item {
                        Text(
                            text = historyState.error.message
                                ?: "Unable to load training history.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    item {
                        Button(onClick = onRetry) {
                            Text("Try again")
                        }
                    }
                }

                is HistoryState.Success -> {
                    val activities = historyState.records
                        .filterIsInstance<ActivityRecord>()

                    if (activities.isEmpty()) {
                        item {
                            Text("No saved activities yet.")
                        }
                    } else {
                        items(
                            items = activities,
                            key = { activity -> activity.id }
                        ) { activity ->
                            TrainingHistoryCard(
                                activity = activity,
                                selectionEnabled = managingRecords,
                                selected = activity.id in selectedRecordIds,
                                onSelectionChange = { selected ->
                                    selectedRecordIds = if (selected) {
                                        selectedRecordIds + activity.id
                                    } else {
                                        selectedRecordIds - activity.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // This action remains above the app navigation while the list scrolls.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 4.dp
        ) {
            if (managingRecords) {
                Button(
                    onClick = {
                        showDeleteConfirmation = true
                    },
                    enabled = selectedRecordIds.isNotEmpty() &&
                            deletingRecordIds.isEmpty(),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = if (deletingRecordIds.isNotEmpty()) {
                            "Deleting..."
                        } else {
                            "Delete selected (${selectedRecordIds.size})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                PrimaryButton(
                    text = "Back to Activity",
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    ),
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun TrainingHistoryCard(
    activity: ActivityRecord,
    selectionEnabled: Boolean,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val dateText = remember(activity.recordedAtEpochMillis) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(activity.recordedAtEpochMillis))
    }

    val durationText = remember(activity.durationSeconds) {
        val hours = activity.durationSeconds / 3_600
        val minutes = activity.durationSeconds / 60 % 60
        val seconds = activity.durationSeconds % 60

        if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = selectionEnabled) {
                onSelectionChange(!selected)
            },
        colors = CardDefaults.cardColors(
            containerColor = Mint
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.activityType.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectionEnabled) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectionChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = ForestDark,
                            checkmarkColor = Color.White,
                            uncheckedColor = ForestDark
                        )
                    )
                }
            }

            Text(
                text = dateText,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = "$durationText · " +
                        String.format(
                            Locale.getDefault(),
                            "%.2f km",
                            activity.distanceMetres / 1_000.0
                        )
            )

            Text(
                text = activity.steps?.let { "$it steps" }
                    ?: "Steps unavailable"
            )
        }
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
    onDone: () -> Unit,
    onViewHistory: () -> Unit
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
            .verticalScroll(rememberScrollState())
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

        val saveMessage = when (uiState.saveState) {
            ActivitySaveState.NOT_SAVED -> "Session ended · Not saved yet"
            ActivitySaveState.SAVING -> "Saving activity..."
            ActivitySaveState.SAVED -> "Activity saved"
            ActivitySaveState.FAILED ->
                uiState.saveError ?: "Unable to save activity"
        }

        Text(
            text = saveMessage,
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.saveState == ActivitySaveState.FAILED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.secondary
            }
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
            modifier = Modifier.fillMaxWidth(),
            chartHeight = 260.dp
        )

        PrimaryButton(
            text = "Back to Activity",
            onClick = onDone
        )

        OutlinedButton(
            onClick = onViewHistory,
            enabled = uiState.saveState == ActivitySaveState.SAVED,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                if (uiState.saveState == ActivitySaveState.SAVING) {
                    "Saving activity..."
                } else {
                    "View training history"
                }
            )
        }

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
