package com.example.comp90018.wear

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.example.comp90018.watch.*
import java.util.Locale

@Composable
fun WatchScreen(
    state: WatchUiState,
    demo: Boolean,
    onRefresh: () -> Unit,
    onCommand: (WatchAction, WatchStartOptions?) -> Unit,
    onRetry: () -> Unit,
    onDemo: () -> Unit,
    onDemoConnection: (Boolean) -> Unit = {},
    onDemoSignIn: (Boolean) -> Unit = {}
) {
    var setup by rememberSaveable { mutableStateOf(false) }
    var activityIndex by rememberSaveable { mutableIntStateOf(0) }
    var goalIndex by rememberSaveable { mutableIntStateOf(0) }
    val session = state.session
    val phase = session?.phase ?: WatchPhase.IDLE
    val enabled = !state.busy && !state.stale && !state.canRetry && session != null
    LaunchedEffect(phase) { if (phase != WatchPhase.IDLE && phase != WatchPhase.COMPLETE) setup = false }

    MaterialTheme(colors = Colors(primary = Color(0xFF8EE2AB))) {
        Scaffold(timeText = { TimeText() }) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().testTag("watch-screen"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Label("TRAILWISE", "watch-title") }
                item { Label(if (demo || session?.demo == true) "DEMO · no GPS / no save" else "Phone companion", "mode") }
                item {
                    Label(when {
                        setup -> "Start activity"
                        phase == WatchPhase.LIVE -> "Live activity"
                        phase == WatchPhase.PAUSED -> "Paused"
                        phase == WatchPhase.COMPLETE -> "Complete"
                        else -> "Home"
                    }, "phase")
                }
                if (state.busy) item { Label("Waiting for acknowledgement…", "loading") }
                state.failure?.let { failure ->
                    item { Label(failureText(failure), "error") }
                }
                if (state.stale && session != null) item { Label("Last confirmed state · may be stale", "stale") }
                if (state.canRetry) item { Action("Retry same command", "retry", !state.busy, onRetry) }

                when {
                    setup -> {
                        item {
                            Action("Type: ${WatchActivityType.entries[activityIndex].name.lowercase()}", "choose-type", enabled) {
                                activityIndex = (activityIndex + 1) % WatchActivityType.entries.size
                            }
                        }
                        item {
                            Action("Goal: ${listOf("Open", "15 min", "1 km")[goalIndex]}", "choose-goal", enabled) {
                                goalIndex = (goalIndex + 1) % 3
                            }
                        }
                        item {
                            Action("Start", "start", enabled) {
                                val options = when (goalIndex) {
                                    1 -> WatchStartOptions(WatchActivityType.entries[activityIndex], WatchGoalType.DURATION_SECONDS, 900.0)
                                    2 -> WatchStartOptions(WatchActivityType.entries[activityIndex], WatchGoalType.DISTANCE_METRES, 1000.0)
                                    else -> WatchStartOptions(WatchActivityType.entries[activityIndex])
                                }
                                onCommand(WatchAction.START, options)
                            }
                        }
                        item { Action("Back", "back", !state.busy) { setup = false } }
                    }
                    phase == WatchPhase.LIVE || phase == WatchPhase.PAUSED -> {
                        item { Label(session?.activityType?.name ?: "Activity", "activity-type") }
                        item { Label(formatMetrics(session!!), "metrics") }
                        item {
                            Action(if (phase == WatchPhase.PAUSED) "Resume" else "Pause", "pause-resume", enabled) {
                                onCommand(if (phase == WatchPhase.PAUSED) WatchAction.RESUME else WatchAction.PAUSE, null)
                            }
                        }
                        item { Action("Finish", "finish", enabled) { onCommand(WatchAction.FINISH, null) } }
                    }
                    phase == WatchPhase.COMPLETE -> {
                        item { Label(formatMetrics(session!!), "metrics") }
                        item { Label(if (session?.saved == true) "Saved on phone" else "Not saved · demo only", "saved") }
                        item { Action("New activity", "new-activity", enabled) { setup = true } }
                    }
                    else -> {
                        item { Action("Set up activity", "setup", enabled) { setup = true } }
                        if (session == null && !demo) item { Action("Try local demo", "demo", !state.busy && !state.canRetry, onDemo) }
                    }
                }
                item { Action("Refresh phone state", "refresh", !state.busy, onRefresh) }
                if (demo) {
                    item { Action("Simulate disconnect", "disconnect", !state.busy) { onDemoConnection(false) } }
                    item { Action("Reconnect demo", "reconnect", !state.busy) { onDemoConnection(true) } }
                    item { Action("Simulate sign out", "signout", !state.busy) { onDemoSignIn(false) } }
                    item { Action("Sign in demo", "signin", !state.busy) { onDemoSignIn(true) } }
                }
            }
        }
    }
}

@Composable private fun Label(text: String, tag: String) {
    Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag(tag))
}

@Composable private fun Action(text: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Chip(onClick = onClick, enabled = enabled, label = { Text(text) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag))
}

private fun formatMetrics(state: WatchSessionState): String = String.format(Locale.US,
    "%02d:%02d · %.0f m\n%s steps", state.durationSeconds / 60, state.durationSeconds % 60,
    state.distanceMetres, state.steps?.toString() ?: "—")

private fun failureText(reason: WatchFailure): String = when (reason) {
    WatchFailure.SIGNED_OUT -> "Sign in on your phone first."
    WatchFailure.ACCOUNT_CHANGED -> "Account changed. Refresh before sending a new command."
    WatchFailure.DISCONNECTED -> "Phone disconnected. Reconnect and refresh or retry."
    WatchFailure.TIMEOUT -> "No acknowledgement. The command may have reached the phone."
    WatchFailure.NOT_READY -> "Open the phone app and connect its activity bridge."
    WatchFailure.SAVE_FAILED -> "Phone save failed. Retry the same command."
    WatchFailure.AMBIGUOUS_PHONE -> "Multiple phones found. Pair only the intended phone."
    WatchFailure.WRONG_SESSION -> "Session changed on phone. Refresh its state."
    else -> "Command rejected. Refresh the phone state."
}
