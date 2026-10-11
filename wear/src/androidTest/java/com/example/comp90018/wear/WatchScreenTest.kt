package com.example.comp90018.wear

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.comp90018.watch.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun demoToolsAreCollapsedAndCanBeOpenedWithoutIssuingACommand() {
        val state = WatchUiState(WatchSessionState(demo = true))
        var commands = 0
        var connected: Boolean? = null
        compose.setContent { WatchScreen(state, true, {}, { _, _ -> commands++ }, {}, {},
            onDemoConnection = { connected = it }) }
        // Scroll to the section before checking lazy items, so this verifies
        // collapse state rather than mere off-screen item disposal.
        scrollTo("demo-tools")
        compose.onNodeWithTag("disconnect").assertDoesNotExist()
        tap("demo-tools"); tap("disconnect")
        assertEquals(false, connected)
        assertEquals(0, commands)
        tap("demo-tools")
        compose.onNodeWithTag("disconnect").assertDoesNotExist()
    }

    @Test fun metricCardShowsAcknowledgedTimeDistanceAndUnknownSteps() {
        val state = WatchUiState(WatchSessionState("session", WatchPhase.LIVE,
            WatchActivityType.RUNNING, durationSeconds = 3661, distanceMetres = 1250.0, demo = true))
        compose.setContent { WatchScreen(state, true, {}, { _, _ -> }, {}, {}) }
        scrollTo("metrics")
        compose.onNodeWithText("01:01:01").assertExists()
        compose.onNodeWithText("1.25").assertExists()
        compose.onNodeWithText("—").assertExists()
    }

    @Test fun realViewModelCanFinishDemoAndReturnToPhoneMode() {
        val vm = WatchViewModel(ApplicationProvider.getApplicationContext<Application>())
        val store = ViewModelStore().apply { put("watch-test", vm) }
        try {
            compose.setContent {
                val state by vm.state.collectAsState()
                val demo by vm.demo.collectAsState()
                WatchScreen(state, demo, vm::refresh, vm::send, vm::retry, vm::enableDemo,
                    vm::demoConnection, vm::demoSignIn, vm::exitDemo)
            }
            tap("demo")
            compose.waitUntil(10_000) { vm.state.value.session != null && !vm.state.value.busy }
            tap("setup"); tap("start")
            compose.waitUntil(10_000) { vm.state.value.session?.phase == WatchPhase.LIVE }
            tap("finish"); tap("confirm-finish")
            compose.waitUntil(10_000) { vm.state.value.session?.phase == WatchPhase.COMPLETE }
            tap("exit-demo")
            compose.waitUntil(10_000) { !vm.demo.value && !vm.state.value.busy }
            scrollTo("mode")
            compose.onNodeWithTag("mode").assertTextEquals("Phone companion")
            assertEquals(false, vm.demo.value)
            // Re-entering demo must not be stuck on the previous setup screen.
            scrollTo("demo")
            compose.onNodeWithTag("demo").assertIsEnabled()
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun homeStartLivePausedCompleteFlowUsesAcknowledgements() {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneCommandDispatcher(gateway)
        var state by mutableStateOf(WatchUiState())
        val controller = WatchController(WatchBridge { dispatcher.sendWatchCommand("ui-test", it) }) { state = it }
        runBlocking { controller.refresh() }
        compose.setContent {
            WatchScreen(state, true,
                { runBlocking { controller.refresh() } },
                { action, options -> runBlocking { controller.command(action, options) } },
                { runBlocking { controller.retry() } }, {})
        }
        assertPhase("Home")
        tap("setup")
        assertPhase("Start activity")
        tap("start")
        assertPhase("Live activity")
        tap("pause-resume")
        assertPhase("Paused")
        tap("pause-resume")
        assertPhase("Live activity")
        tap("finish")
        assertPhase("Live activity")
        tap("confirm-finish")
        assertPhase("Complete")
        scrollTo("saved")
        compose.onNodeWithTag("saved").assertTextEquals("Not saved · demo only")
        assertEquals(4, gateway.executions)
    }

    @Test fun disconnectedStateDisablesMutationAndOffersRetry() {
        val state = WatchUiState(WatchSessionState("session", WatchPhase.LIVE, demo = true),
            stale = true, failure = WatchFailure.DISCONNECTED, canRetry = true)
        compose.setContent { WatchScreen(state, true, {}, { _, _ -> }, {}, {}) }
        scrollTo("pause-resume")
        compose.onNodeWithTag("pause-resume").assertIsNotEnabled()
        scrollTo("retry")
        compose.onNodeWithTag("retry").assertIsEnabled()
    }

    @Test fun cancelFinishKeepsActivityAndLiveDemoCannotSwitchModes() {
        val state = WatchUiState(WatchSessionState("session", WatchPhase.LIVE, WatchActivityType.WALKING, demo = true))
        var commands = 0
        compose.setContent { WatchScreen(state, true, {}, { _, _ -> commands++ }, {}, {}) }
        tap("finish")
        scrollTo("finish-note")
        compose.onNodeWithTag("finish-note").assertTextEquals("Demo results will not be saved.")
        tap("cancel-finish")
        scrollTo("pause-resume")
        compose.onNodeWithTag("pause-resume").assertIsEnabled()
        scrollTo("exit-demo")
        compose.onNodeWithTag("exit-demo").assertIsNotEnabled()
        assertEquals(0, commands)
    }

    @Test fun completedDemoCanReturnToPhoneCompanion() {
        var demo by mutableStateOf(true)
        var state by mutableStateOf(WatchUiState(WatchSessionState("session", WatchPhase.COMPLETE,
            WatchActivityType.WALKING, demo = true)))
        compose.setContent { WatchScreen(state, demo, {}, { _, _ -> }, {}, {}, onExitDemo = {
            demo = false; state = WatchUiState(failure = WatchFailure.DISCONNECTED)
        }) }
        tap("exit-demo")
        scrollTo("mode")
        compose.onNodeWithTag("mode").assertTextEquals("Phone companion")
        scrollTo("demo")
        compose.onNodeWithTag("demo").assertIsEnabled()
    }

    @Test fun pendingCommandBlocksModeSwitchAndMutations() {
        val state = WatchUiState(WatchSessionState(demo = true), stale = true, canRetry = true,
            failure = WatchFailure.TIMEOUT)
        compose.setContent { WatchScreen(state, true, {}, { _, _ -> }, {}, {}) }
        scrollTo("exit-demo")
        compose.onNodeWithTag("exit-demo").assertIsNotEnabled()
        scrollTo("setup")
        compose.onNodeWithTag("setup").assertIsNotEnabled()
    }

    @Test fun unsavedPhoneCompletionIsNotMislabeledAsDemo() {
        val state = WatchUiState(WatchSessionState("session", WatchPhase.COMPLETE, WatchActivityType.WALKING))
        compose.setContent { WatchScreen(state, false, {}, { _, _ -> }, {}, {}) }
        scrollTo("saved")
        compose.onNodeWithTag("saved").assertTextEquals("Not saved on phone")
        scrollTo("retry-save")
        compose.onNodeWithTag("retry-save").assertIsEnabled()
        compose.onNodeWithTag("new-activity").assertDoesNotExist()
    }

    @Test fun setupChoosesRunningAndThirtyMinuteGoal() {
        var chosen: WatchStartOptions? = null
        val state = WatchUiState(WatchSessionState(demo = true))
        compose.setContent { WatchScreen(state, true, {}, { _, options -> chosen = options }, {}, {}) }
        tap("setup"); tap("choose-type"); tap("choose-goal"); tap("choose-goal"); tap("start")
        assertEquals(WatchActivityType.RUNNING, chosen!!.activityType)
        assertEquals(WatchGoalType.DURATION_SECONDS, chosen!!.goalType)
        assertEquals(1800.0, chosen!!.goalValue)
    }

    private fun tap(tag: String) {
        scrollTo(tag)
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun assertPhase(text: String) {
        scrollTo("phase")
        compose.onNodeWithTag("phase").assertTextEquals(text)
    }

    private fun scrollTo(tag: String) {
        // Wear ScalingLazyColumn's test tag belongs to an outer rotary Box.
        // Scroll the nested actual LazyColumn rather than that non-scrollable Box.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
    }
}
