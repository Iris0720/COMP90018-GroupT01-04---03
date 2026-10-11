package com.example.comp90018.wear

import com.example.comp90018.watch.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WatchControllerTest {
    @Test fun timeoutAfterExecutionRetriesSameIdExactlyOnce() = runTest {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneCommandDispatcher(gateway)
        val ids = mutableListOf<String>()
        var loseAck = true
        val controller = WatchController(WatchBridge { command ->
            val ack = dispatcher.sendWatchCommand("watch", command)
            if (command.action == WatchAction.START) {
                ids += command.commandId
                if (loseAck) { loseAck = false; return@WatchBridge WatchAcknowledgement.rejected(command, WatchFailure.TIMEOUT, true) }
            }
            ack
        })
        controller.refresh()
        controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(WatchPhase.IDLE, controller.state.session!!.phase)
        assertTrue(controller.state.canRetry)
        assertTrue(controller.state.stale)
        controller.command(WatchAction.START, WatchStartOptions()) // unresolved mutation cannot be replaced
        controller.retry()
        assertEquals(ids.first(), ids.last())
        assertEquals(2, ids.size)
        assertEquals(1, gateway.executions)
        assertEquals(WatchPhase.LIVE, controller.state.session!!.phase)
        assertEquals(AckStatus.DUPLICATE, controller.state.lastStatus)
    }

    @Test fun disconnectDoesNotOptimisticallyPause() = runTest {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneCommandDispatcher(gateway)
        var connected = true
        val controller = WatchController(WatchBridge { command ->
            if (connected) dispatcher.sendWatchCommand("watch", command)
            else WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
        })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        connected = false; controller.command(WatchAction.PAUSE)
        assertEquals(WatchPhase.LIVE, controller.state.session!!.phase)
        assertEquals(WatchPhase.LIVE, gateway.snapshot().phase)
        assertTrue(controller.state.canRetry)
        connected = true; controller.retry()
        assertEquals(WatchPhase.PAUSED, controller.state.session!!.phase)
    }

    @Test fun signedOutClearsPreviousVisibleData() = runTest {
        val gateway = DemoPhoneGateway()
        val d = PhoneCommandDispatcher(gateway)
        val controller = WatchController(WatchBridge { d.sendWatchCommand("watch", it) })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        gateway.signedIn = false
        controller.refresh()
        assertNull(controller.state.session)
        assertFalse(controller.state.canRetry)
        assertEquals(WatchFailure.SIGNED_OUT, controller.state.failure)
    }

    @Test fun mismatchedReplyNeverChangesUiAndKeepsRetry() = runTest {
        val gateway = DemoPhoneGateway()
        val d = PhoneCommandDispatcher(gateway)
        val controller = WatchController(WatchBridge { command ->
            val reply = d.sendWatchCommand("watch", command)
            if (command.action == WatchAction.START) reply.copy(commandId = "other") else reply
        })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(WatchPhase.IDLE, controller.state.session!!.phase)
        assertEquals(WatchFailure.REMOTE_ERROR, controller.state.failure)
        assertTrue(controller.state.canRetry)
    }

    @Test fun accountGenerationChangeDiscardsOldRetry() = runTest {
        val gateway = DemoPhoneGateway()
        val d = PhoneCommandDispatcher(gateway)
        var connected = true
        val controller = WatchController(WatchBridge { command ->
            if (connected) d.sendWatchCommand("watch", command)
            else WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
        })
        controller.refresh()
        connected = false; controller.command(WatchAction.START, WatchStartOptions())
        assertTrue(controller.state.canRetry)
        d.invalidateAccount()
        connected = true; controller.refresh()
        assertFalse(controller.state.canRetry)
        controller.retry()
        assertEquals(0, gateway.executions)
    }

    @Test fun noAccountHandshakeCannotSendMutation() = runTest {
        var requests = 0
        val controller = WatchController(WatchBridge { requests++; WatchAcknowledgement.rejected(it, WatchFailure.SIGNED_OUT) })
        controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(0, requests)
        assertEquals(WatchFailure.NOT_READY, controller.state.failure)
    }

    @Test fun cancellationPropagatesAndPreservesUncertainCommand() = runTest {
        val gateway = DemoPhoneGateway()
        val d = PhoneCommandDispatcher(gateway)
        val controller = WatchController(WatchBridge { command ->
            if (command.action == WatchAction.START) throw java.util.concurrent.CancellationException()
            d.sendWatchCommand("watch", command)
        })
        controller.refresh()
        val attempt = runCatching { controller.command(WatchAction.START, WatchStartOptions()) }
        assertTrue(attempt.exceptionOrNull() is java.util.concurrent.CancellationException)
        assertFalse(controller.state.busy)
        assertTrue(controller.state.canRetry)
        assertEquals(WatchPhase.IDLE, controller.state.session!!.phase)
    }
}
