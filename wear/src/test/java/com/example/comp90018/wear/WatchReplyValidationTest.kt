package com.example.comp90018.wear

import com.example.comp90018.watch.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class WatchReplyValidationTest {
    @Test fun staleRefreshPreventsNewMutationUntilRecovery() = runTest {
        val dispatcher = PhoneCommandDispatcher(DemoPhoneGateway())
        var connected = true
        var starts = 0
        val controller = WatchController(WatchBridge { command ->
            if (command.action == WatchAction.START) starts++
            if (connected) dispatcher.sendWatchCommand("watch", command)
            else WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
        })
        controller.refresh()
        connected = false; controller.refresh()
        assertTrue(controller.state.stale)
        controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(0, starts)
        connected = true; controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(1, starts)
    }

    private fun rejectsStartReply(change: (WatchAcknowledgement) -> WatchAcknowledgement) = runTest {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneCommandDispatcher(gateway)
        var tamper = true
        val controller = WatchController(WatchBridge { command ->
            val ack = dispatcher.sendWatchCommand("watch", command)
            if (command.action == WatchAction.START && tamper) change(ack) else ack
        })
        controller.refresh()
        controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(WatchPhase.IDLE, controller.state.session!!.phase)
        assertEquals(WatchFailure.REMOTE_ERROR, controller.state.failure)
        assertTrue(controller.state.canRetry)
        assertTrue(controller.state.stale)
        tamper = false
        controller.retry()
        assertEquals(WatchPhase.LIVE, controller.state.session!!.phase)
        assertEquals(1, gateway.executions)
        assertFalse(controller.state.canRetry)
    }

    @Test fun wrongSessionReplyPreservesOriginalRetry() = rejectsStartReply {
        it.copy(state = it.state!!.copy(sessionId = UUID.randomUUID().toString()))
    }
    @Test fun impossibleAcceptedPhaseIsRejected() = rejectsStartReply {
        it.copy(state = it.state!!.copy(phase = WatchPhase.COMPLETE))
    }
    @Test fun negativeMetricsAreRejected() = rejectsStartReply {
        it.copy(state = it.state!!.copy(durationSeconds = -1))
    }
    @Test fun nonFiniteDistanceIsRejected() = rejectsStartReply {
        it.copy(state = it.state!!.copy(distanceMetres = Double.NaN))
    }
    @Test fun blankAccountBindingIsRejected() = rejectsStartReply { it.copy(accountToken = " ") }
    @Test fun changedBindingOnMutationIsRejected() = rejectsStartReply { it.copy(accountToken = "other-generation") }
    @Test fun failureAttachedToSuccessfulReplyIsRejected() = rejectsStartReply { it.copy(failure = WatchFailure.SAVE_FAILED) }

    @Test fun nonDemoFinishCannotShowUnsavedSuccess() = runTest {
        val dispatcher = PhoneCommandDispatcher(DemoPhoneGateway())
        val controller = WatchController(WatchBridge { command ->
            val ack = dispatcher.sendWatchCommand("watch", command)
            if (command.action == WatchAction.FINISH) ack.copy(state = ack.state!!.copy(demo = false, saved = false)) else ack
        })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        controller.command(WatchAction.FINISH)
        assertEquals(WatchPhase.LIVE, controller.state.session!!.phase)
        assertEquals(WatchFailure.REMOTE_ERROR, controller.state.failure)
        assertTrue(controller.state.canRetry)
    }

    @Test fun modeResetRequiresFreshHandshakeAndDoesNotAbandonLiveSession() = runTest {
        val dispatcher = PhoneCommandDispatcher(DemoPhoneGateway())
        val controller = WatchController(WatchBridge { dispatcher.sendWatchCommand("watch", it) })
        controller.refresh()
        assertTrue(controller.resetForModeChange())
        assertNull(controller.state.session)
        controller.command(WatchAction.START, WatchStartOptions())
        assertEquals(WatchFailure.NOT_READY, controller.state.failure)
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        assertFalse(controller.resetForModeChange())
        controller.command(WatchAction.PAUSE)
        assertFalse(controller.resetForModeChange())
        controller.command(WatchAction.FINISH)
        assertTrue(controller.resetForModeChange())
    }

    @Test fun modeResetCannotDiscardAnUncertainRequest() = runTest {
        val dispatcher = PhoneCommandDispatcher(DemoPhoneGateway())
        val controller = WatchController(WatchBridge { command ->
            if (command.action == WatchAction.START) WatchAcknowledgement.rejected(command, WatchFailure.TIMEOUT, true)
            else dispatcher.sendWatchCommand("watch", command)
        })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions())
        assertFalse(controller.resetForModeChange())
        assertTrue(controller.state.canRetry)
    }

    @Test fun resetCannotDiscardCompletedButUnsavedRealSession() = runTest {
        val controller = WatchController(WatchBridge { command ->
            WatchAcknowledgement(command.commandId, AckStatus.ACCEPTED,
                WatchSessionState(UUID.randomUUID().toString(), WatchPhase.COMPLETE, WatchActivityType.WALKING), "generation")
        })
        controller.refresh()
        assertFalse(controller.resetForModeChange())
    }

    @Test fun duplicateReplyCanShowLaterConfirmedState() = runTest {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneCommandDispatcher(gateway)
        var lost = true
        val controller = WatchController(WatchBridge { command ->
            val ack = dispatcher.sendWatchCommand("watch", command)
            if (command.action == WatchAction.START && lost) {
                lost = false
                gateway.execute(WatchCommand(UUID.randomUUID().toString(), WatchAction.PAUSE, command.sessionId), "demo-account")
                WatchAcknowledgement.rejected(command, WatchFailure.TIMEOUT, true)
            } else ack
        })
        controller.refresh(); controller.command(WatchAction.START, WatchStartOptions()); controller.retry()
        assertEquals(AckStatus.DUPLICATE, controller.state.lastStatus)
        assertEquals(WatchPhase.PAUSED, controller.state.session!!.phase)
    }
}
