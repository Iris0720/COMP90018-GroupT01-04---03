package com.example.comp90018.watch

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Executors

class PhoneCommandDispatcherTest {
    private var time = 0L
    private val gateway = DemoPhoneGateway { time }
    private val dispatcher = PhoneCommandDispatcher(gateway)
    private fun id() = UUID.randomUUID().toString()
    private fun read() = dispatcher.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE))
    private fun start(token: String? = read().accountToken) = WatchCommand(id(), WatchAction.START, id(), WatchStartOptions(), token)
    private fun send(command: WatchCommand) = dispatcher.sendWatchCommand("watch", command)
    private fun next(action: WatchAction, start: WatchCommand) = WatchCommand(id(), action, start.sessionId, accountToken = start.accountToken)

    @Test fun lifecycleExcludesPausedTimeAndNeverClaimsDemoSaved() {
        val start = start()
        assertEquals(WatchPhase.LIVE, send(start).state!!.phase)
        time = 10_000
        assertEquals(10L, send(next(WatchAction.PAUSE, start)).state!!.durationSeconds)
        time = 110_000
        assertEquals(10L, read().state!!.durationSeconds)
        assertEquals(WatchPhase.LIVE, send(next(WatchAction.RESUME, start)).state!!.phase)
        time = 115_000
        val finish = next(WatchAction.FINISH, start)
        val result = send(finish)
        assertEquals(WatchPhase.COMPLETE, result.state!!.phase)
        assertEquals(15L, result.state.durationSeconds)
        assertFalse(result.state.saved)
        assertEquals(AckStatus.DUPLICATE, send(finish).status)
        assertEquals(4, gateway.executions)
    }

    @Test fun duplicateStartExecutesOnce() {
        val command = start()
        val first = send(command)
        val second = send(command)
        assertEquals(AckStatus.ACCEPTED, first.status)
        assertEquals(AckStatus.DUPLICATE, second.status)
        assertEquals(first.state!!.sessionId, second.state!!.sessionId)
        assertEquals(1, gateway.executions)
    }

    @Test fun concurrentDuplicateStartExecutesOnce() {
        val command = start()
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (1..8).map { executor.submit<WatchAcknowledgement> { send(command) } }.map { it.get() }
            assertEquals(1, results.count { it.status == AckStatus.ACCEPTED })
            assertEquals(7, results.count { it.status == AckStatus.DUPLICATE })
            assertEquals(1, gateway.executions)
        } finally { executor.shutdownNow() }
    }

    @Test fun commandIdReusedWithDifferentPayloadRejected() {
        val command = start()
        send(command)
        assertEquals(WatchFailure.COMMAND_ID_REUSED,
            send(command.copy(action = WatchAction.PAUSE, options = null)).failure)
    }

    @Test fun duplicateOldPauseReturnsCurrentPhoneState() {
        val start = start(); send(start)
        val pause = next(WatchAction.PAUSE, start); send(pause)
        send(next(WatchAction.RESUME, start))
        val replay = send(pause)
        assertEquals(AckStatus.DUPLICATE, replay.status)
        assertEquals(WatchPhase.LIVE, replay.state!!.phase)
        assertEquals(3, gateway.executions)
    }

    @Test fun signedOutReturnsNoSessionOrAccountBinding() {
        gateway.signedIn = false
        val response = read()
        assertEquals(WatchFailure.SIGNED_OUT, response.failure)
        assertNull(response.state)
        assertNull(response.accountToken)
        assertEquals(0, gateway.executions)
    }

    @Test fun sameAccountLogoutLoginInvalidatesOutstandingCommand() {
        val staleCommand = start()
        dispatcher.invalidateAccount()
        assertEquals(WatchFailure.ACCOUNT_CHANGED, send(staleCommand).failure)
        assertNotEquals(staleCommand.accountToken, read().accountToken)
        assertEquals(0, gateway.executions)
    }

    @Test fun processRestartRejectsOldCommandEvenWhenSameOwner() {
        val command = start()
        val restarted = PhoneCommandDispatcher(gateway)
        assertEquals(WatchFailure.ACCOUNT_CHANGED, restarted.sendWatchCommand("watch", command).failure)
    }

    @Test fun wrongSessionCannotPauseActiveActivity() {
        val command = start(); send(command)
        assertEquals(WatchFailure.WRONG_SESSION,
            send(next(WatchAction.PAUSE, command).copy(sessionId = id())).failure)
        assertEquals(WatchPhase.LIVE, read().state!!.phase)
    }

    @Test fun invalidTransitionCannotResumeLiveSession() {
        val command = start(); send(command)
        assertEquals(WatchFailure.INVALID_TRANSITION, send(next(WatchAction.RESUME, command)).failure)
        assertEquals(1, gateway.executions)
    }

    @Test fun invalidGoalsAndIdentifiersAreRejected() {
        val command = start()
        listOf(command.copy(commandId = "invalid"), command.copy(sessionId = null),
            command.copy(version = 2), command.copy(options = null),
            command.copy(options = WatchStartOptions(goalType = WatchGoalType.DURATION_SECONDS, goalValue = Double.NaN)),
            command.copy(options = WatchStartOptions(goalType = WatchGoalType.DISTANCE_METRES, goalValue = -1.0)),
            command.copy(options = WatchStartOptions(goalValue = 10.0))).forEach {
            assertEquals(WatchFailure.INVALID_COMMAND, send(it).failure)
        }
        assertEquals(0, gateway.executions)
    }

    @Test fun saveFailureCanRetrySameCommandWithoutFalseSuccess() {
        var fail = true
        val failing = object : PhoneSessionGateway by gateway {
            override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
                if (command.action == WatchAction.FINISH && fail) {
                    fail = false
                    throw WatchCommandException(WatchFailure.SAVE_FAILED, true)
                }
                return gateway.execute(command, expectedOwnerId)
            }
        }
        val d = PhoneCommandDispatcher(failing)
        val token = d.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE)).accountToken
        val start = start(token); d.sendWatchCommand("watch", start)
        val finish = next(WatchAction.FINISH, start)
        val first = d.sendWatchCommand("watch", finish)
        assertEquals(WatchFailure.SAVE_FAILED, first.failure)
        assertTrue(first.retryable)
        assertEquals(WatchPhase.LIVE, gateway.snapshot().phase)
        assertEquals(AckStatus.ACCEPTED, d.sendWatchCommand("watch", finish).status)
        assertEquals(AckStatus.DUPLICATE, d.sendWatchCommand("watch", finish).status)
        assertEquals(2, gateway.executions)
    }

    @Test fun boundedJournalRotatesBindingInsteadOfReexecutingEvictedCommands() {
        val token = read().accountToken
        val first = start(token); send(first)
        send(next(WatchAction.FINISH, first))
        repeat(127) {
            val command = start(token); send(command); send(next(WatchAction.FINISH, command))
        }
        assertNotEquals(token, read().accountToken)
        assertEquals(WatchFailure.ACCOUNT_CHANGED, send(first).failure)
        assertEquals(256, gateway.executions)
    }

    @Test fun wireRoundTripPreservesCommandsAndAcknowledgements() {
        val command = start()
        assertEquals(command, WatchWire.command(WatchWire.encode(command)))
        val ack = send(command)
        assertEquals(ack, WatchWire.acknowledgement(WatchWire.encode(ack)))
    }

    @Test fun realFinishCannotClaimSuccessWithoutPersistentSaveAndCanRetry() {
        var persist = false
        var allowSave = false
        val real = object : PhoneSessionGateway by gateway {
            override fun snapshot() = gateway.snapshot().copy(demo = false, saved = persist)
            override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
                if (gateway.snapshot().phase != WatchPhase.COMPLETE) gateway.execute(command, expectedOwnerId)
                if (command.action == WatchAction.FINISH && allowSave) persist = true
                return snapshot()
            }
        }
        val d = PhoneCommandDispatcher(real)
        val token = d.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE)).accountToken
        val command = start(token)
        d.sendWatchCommand("watch", command)
        val finish = next(WatchAction.FINISH, command)
        assertEquals(WatchFailure.SAVE_FAILED, d.sendWatchCommand("watch", finish).failure)
        assertFalse(real.snapshot().saved)
        allowSave = true
        val retry = d.sendWatchCommand("watch", finish)
        assertEquals(AckStatus.ACCEPTED, retry.status)
        assertTrue(retry.state!!.saved)
        assertEquals(AckStatus.DUPLICATE, d.sendWatchCommand("watch", finish).status)
        assertEquals(2, gateway.executions)
    }

    @Test fun wireRejectsOversizedMalformedAndUnknownFields() {
        listOf(ByteArray(WatchWire.MAX_BYTES + 1), byteArrayOf(), "not json".toByteArray(),
            "{\"commandId\":\"x\",\"action\":\"DELETE\"}".toByteArray()).forEach { data ->
            assertTrue(runCatching { WatchWire.command(data) }.isFailure)
        }
    }

    @Test fun finishConfirmsAlreadySavedExactSessionWithoutExecutingAgain() {
        var saved = false
        val real = object : PhoneSessionGateway by gateway {
            override fun snapshot() = gateway.snapshot().copy(demo = false, saved = saved)
            override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
                gateway.execute(command, expectedOwnerId)
                return snapshot()
            }
        }
        val d = PhoneCommandDispatcher(real)
        val token = d.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE)).accountToken
        val command = start(token); d.sendWatchCommand("watch", command)
        val finish = next(WatchAction.FINISH, command)
        assertEquals(WatchFailure.SAVE_FAILED, d.sendWatchCommand("watch", finish).failure)
        saved = true
        assertEquals(AckStatus.ACCEPTED, d.sendWatchCommand("watch", finish).status)
        assertEquals(2, gateway.executions)
    }

    @Test fun authChangeBeforeMutationIsRejectedByTrustedOwnerCheck() {
        val changing = object : PhoneSessionGateway by gateway {
            override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
                gateway.signedIn = false
                return gateway.execute(command, expectedOwnerId)
            }
        }
        val d = PhoneCommandDispatcher(changing)
        val token = d.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE)).accountToken
        assertEquals(WatchFailure.ACCOUNT_CHANGED, d.sendWatchCommand("watch", start(token)).failure)
        assertEquals(0, gateway.executions)
    }

    @Test fun invalidationDoesNotBlockBehindCommandWaitingForMainThread() {
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val blocking = object : PhoneSessionGateway by gateway {
            override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
                started.countDown()
                check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                return gateway.execute(command, expectedOwnerId)
            }
        }
        val d = PhoneCommandDispatcher(blocking)
        val token = d.sendWatchCommand("watch", WatchCommand(id(), WatchAction.READ_STATE)).accountToken
        val executor = Executors.newFixedThreadPool(2)
        try {
            val command = executor.submit<WatchAcknowledgement> { d.sendWatchCommand("watch", start(token)) }
            assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS))
            executor.submit { d.invalidateAccount() }.get(1, java.util.concurrent.TimeUnit.SECONDS)
            release.countDown()
            val result = command.get(2, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(WatchFailure.ACCOUNT_CHANGED, result.failure)
            assertNull(result.state)
        } finally { release.countDown(); executor.shutdownNow() }
    }
}
