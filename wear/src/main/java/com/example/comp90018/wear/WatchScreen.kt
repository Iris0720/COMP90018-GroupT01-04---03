package com.example.comp90018.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDemoSignIn: (Boolean) -> Unit = {},
    onExitDemo: () -> Unit = {}
) {
    var setup by rememberSaveable { mutableStateOf(false) }
    var activityIndex by rememberSaveable { mutableIntStateOf(0) }
    var goalIndex by rememberSaveable { mutableIntStateOf(0) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var demoTools by rememberSaveable { mutableStateOf(false) }
    val session = state.session
    val phase = session?.phase ?: WatchPhase.IDLE
    val enabled = !state.busy && !state.stale && !state.canRetry && session != null
    LaunchedEffect(demo) { setup = false; confirmFinish = false; demoTools = false }
    LaunchedEffect(phase) { if (phase != WatchPhase.IDLE && phase != WatchPhase.COMPLETE) setup = false }
    LaunchedEffect(phase, demo) { if (phase !in setOf(WatchPhase.LIVE, WatchPhase.PAUSED)) confirmFinish = false }

    MaterialTheme(colors = Colors(primary = Mint, onPrimary = Forest,
        background = Forest, surface = Surface, onSurface = Color(0xFFE9F2ED))) {
        Scaffold(timeText = { TimeText() }) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().testTag("watch-screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 38.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TrailMark()
                        Text("TRAILWISE", color = Mint, fontSize = 11.sp,
                            letterSpacing = 2.sp, modifier = Modifier.testTag("watch-title"))
                    }
                }
                item { ModeBadge(if (demo || session?.demo == true) "Demo · synthetic / not saved" else "Phone companion") }
                item {
                    Label(when {
                        setup -> "Start activity"
                        phase == WatchPhase.LIVE -> "Live activity"
                        phase == WatchPhase.PAUSED -> "Paused"
                        phase == WatchPhase.COMPLETE -> "Complete"
                        else -> "Home"
                    }, "phase", prominent = true)
                }
                if (state.busy) item { Label("Connecting… please wait", "loading") }
                state.failure?.let { failure ->
                    item { Label(failureText(failure), "error") }
                }
                if (state.stale && session != null) item { Label("Showing the last update", "stale") }
                if (state.canRetry) item { Action("Try again", "retry", !state.busy, onRetry) }

                when {
                    setup -> {
                        item {
                            Action("${activityName(WatchActivityType.entries.getOrNull(activityIndex))} · change", "choose-type", enabled) {
                                activityIndex = (activityIndex + 1) % WatchActivityType.entries.size
                            }
                        }
                        item {
                            Action("Goal: ${watchGoalChoices.getOrElse(goalIndex) { watchGoalChoices.first() }.label}", "choose-goal", enabled) {
                                goalIndex = (goalIndex + 1) % watchGoalChoices.size
                            }
                        }
                        item {
                            Action("Start", "start", enabled) {
                                onCommand(WatchAction.START, watchStartOptions(activityIndex, goalIndex))
                            }
                        }
                        item { Action("Back", "back", !state.busy) { setup = false } }
                    }
                    session != null && (phase == WatchPhase.LIVE || phase == WatchPhase.PAUSED) -> {
                        item { Label(activityName(session.activityType), "activity-type") }
                        item { MetricsCard(session) }
                        if (confirmFinish) {
                            item { Label("Finish this activity?", "finish-question") }
                            item { Label(if (demo || session.demo) "Demo results will not be saved."
                                else "Wait for the phone to confirm the save.", "finish-note") }
                            item { Action("Confirm finish", "confirm-finish", enabled) { onCommand(WatchAction.FINISH, null) } }
                            item { Action("Keep activity", "cancel-finish", !state.busy) { confirmFinish = false } }
                        } else {
                            item {
                                Action(if (phase == WatchPhase.PAUSED) "Resume" else "Pause", "pause-resume", enabled) {
                                    onCommand(if (phase == WatchPhase.PAUSED) WatchAction.RESUME else WatchAction.PAUSE, null)
                                }
                            }
                            item { Action("Finish", "finish", enabled) { confirmFinish = true } }
                        }
                    }
                    session != null && phase == WatchPhase.COMPLETE -> {
                        item { MetricsCard(session) }
                        item { Label(when {
                            session.saved -> "Saved on phone"
                            demo || session.demo -> "Not saved · demo only"
                            else -> "Not saved on phone"
                        }, "saved") }
                        if (!session.saved && !demo && !session.demo) {
                            item { Action("Retry finish/save", "retry-save", enabled) { onCommand(WatchAction.FINISH, null) } }
                        } else item { Action("New activity", "new-activity", enabled) { setup = true } }
                    }
                    else -> {
                        item { Label(if (session == null) "Connect your phone to head out."
                            else "A little movement, a better day.", "home-note") }
                        item { Action("Start activity", "setup", enabled) { setup = true } }
                        if (session == null && !demo) item { Action("Explore demo", "demo", !state.busy && !state.canRetry, onDemo) }
                    }
                }
                item { Action(if (demo) "Refresh demo" else "Refresh connection", "refresh", !state.busy, onRefresh) }
                if (demo) {
                    item { Action("Use phone companion", "exit-demo", !state.busy && !state.canRetry &&
                        phase !in setOf(WatchPhase.LIVE, WatchPhase.PAUSED), onExitDemo) }
                    item { Action(if (demoTools) "Hide demo tools" else "Demo tools", "demo-tools", !state.busy) { demoTools = !demoTools } }
                    if (demoTools) {
                        item { Label("Test controls · demo only", "demo-tools-note") }
                        item { Action("Simulate disconnect", "disconnect", !state.busy) { onDemoConnection(false) } }
                        item { Action("Reconnect demo", "reconnect", !state.busy) { onDemoConnection(true) } }
                        item { Action("Simulate sign out", "signout", !state.busy) { onDemoSignIn(false) } }
                        item { Action("Sign in demo", "signin", !state.busy) { onDemoSignIn(true) } }
                    }
                }
            }
        }
    }
}

private val Forest = Color(0xFF0C1712)
private val Surface = Color(0xFF1B2B22)
private val Mint = Color(0xFFAAF2BC)
private val Muted = Color(0xFFA8B8AD)

@Composable private fun Label(text: String, tag: String, prominent: Boolean = false) {
    Text(text, textAlign = TextAlign.Center, color = if (prominent) MaterialTheme.colors.onSurface else Muted,
        fontSize = if (prominent) 20.sp else 12.sp,
        fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth().testTag(tag))
}

@Composable private fun Action(text: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    val primary = tag in setOf("start", "setup", "demo", "pause-resume", "new-activity", "confirm-finish", "retry", "retry-save")
    Chip(onClick = onClick, enabled = enabled, label = { Text(text) },
        colors = ChipDefaults.chipColors(backgroundColor = if (primary) Mint else Surface,
            contentColor = if (primary) Forest else MaterialTheme.colors.onSurface),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag))
}

@Composable private fun ModeBadge(text: String) {
    Text(text, fontSize = 10.sp, color = Mint, textAlign = TextAlign.Center,
        modifier = Modifier.background(Surface, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp).testTag("mode"))
}

@Composable private fun TrailMark() {
    // Native vector drawing: no bitmap assets, network fetches or decorative metrics.
    Canvas(Modifier.size(38.dp).padding(4.dp)) {
        drawCircle(Mint.copy(alpha = 0.1f))
        val trail = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.74f)
            lineTo(size.width * 0.47f, size.height * 0.24f)
            lineTo(size.width * 0.67f, size.height * 0.53f)
            lineTo(size.width * 0.78f, size.height * 0.4f)
            lineTo(size.width * 0.92f, size.height * 0.74f)
        }
        drawPath(trail, Mint, style = Stroke(width = 2.dp.toPx()))
        drawLine(Mint.copy(alpha = 0.5f), Offset(size.width * 0.2f, size.height * 0.83f),
            Offset(size.width * 0.85f, size.height * 0.83f), strokeWidth = 1.dp.toPx())
    }
}

@Composable private fun MetricsCard(state: WatchSessionState) {
    Column(modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(22.dp))
        .padding(horizontal = 10.dp, vertical = 14.dp).testTag("metrics"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("ACTIVE TIME", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(watchElapsedTime(state.durationSeconds), color = Mint, fontSize = 26.sp,
            fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric(String.format(Locale.US, "%.2f", state.distanceMetres / 1000), "km")
            Metric(state.steps?.toString() ?: "—", "steps")
        }
    }
}

@Composable private fun Metric(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colors.onSurface, fontSize = 17.sp)
        Text(unit, color = Muted, fontSize = 10.sp)
    }
}

private fun activityName(type: WatchActivityType?): String = when (type) {
    WatchActivityType.WALKING -> "Walking"
    WatchActivityType.RUNNING -> "Running"
    WatchActivityType.HIKING -> "Hiking"
    null -> "Activity"
}

private fun failureText(reason: WatchFailure): String = when (reason) {
    WatchFailure.SIGNED_OUT -> "Sign in on your phone first."
    WatchFailure.ACCOUNT_CHANGED -> "Your account changed. Refresh to continue."
    WatchFailure.DISCONNECTED -> "Phone not connected. Reconnect, then refresh."
    WatchFailure.TIMEOUT -> "Still waiting for your phone. Try again to check."
    WatchFailure.NOT_READY -> "Phone controls aren't ready yet. You can explore the demo."
    WatchFailure.SAVE_FAILED -> "Couldn't save on your phone. Try again."
    WatchFailure.AMBIGUOUS_PHONE -> "Multiple phones found. Pair only the intended phone."
    WatchFailure.WRONG_SESSION -> "Activity changed on your phone. Refresh to continue."
    else -> "Couldn't update. Refresh or try again."
}
