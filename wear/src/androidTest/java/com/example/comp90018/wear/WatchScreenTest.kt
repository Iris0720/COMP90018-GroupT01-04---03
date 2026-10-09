package com.example.comp90018.wear

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.comp90018.watch.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchScreenTest {
    @get:Rule val compose = createComposeRule()

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
