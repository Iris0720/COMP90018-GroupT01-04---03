package com.example.comp90018.watch

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** One dispatcher per authoritative phone controller, shared by every watch node.
 * No optimistic state updates: an acknowledgement is returned only after execute completes.
 * The in-process journal covers retransmissions. On process restart accountToken changes,
 * so an old outstanding mutation is rejected instead of blindly executed again.
 */
class PhoneCommandDispatcher(private val gateway: PhoneSessionGateway) {
    private var owner: String? = null
    private var accountToken: String? = null
    private val authGeneration = AtomicLong()
    private var observedGeneration = -1L
    private data class Entry(val command: WatchCommand, val acknowledgement: WatchAcknowledgement)
    private val journal = linkedMapOf<String, Entry>()

    /** Host must call this on EVERY auth transition, including same-owner logout/login. */
    fun invalidateAccount() { authGeneration.incrementAndGet() }

    @Synchronized
    fun sendWatchCommand(sourceNode: String, command: WatchCommand): WatchAcknowledgement {
        if (sourceNode.isBlank() || !valid(command)) {
            return WatchAcknowledgement.rejected(command, WatchFailure.INVALID_COMMAND)
        }
        val generation = authGeneration.get()
        val activeOwner = gateway.currentOwnerId()?.takeIf(String::isNotBlank)
        if (activeOwner != owner || generation != observedGeneration) {
            owner = activeOwner
            observedGeneration = generation
            accountToken = activeOwner?.let { UUID.randomUUID().toString() }
            journal.clear()
        }
        if (activeOwner == null) return WatchAcknowledgement.rejected(command, WatchFailure.SIGNED_OUT)

        if (command.action == WatchAction.READ_STATE) {
            val snapshot = gateway.snapshot()
            if (authGeneration.get() != generation || gateway.currentOwnerId() != activeOwner) {
                return WatchAcknowledgement.rejected(command, WatchFailure.ACCOUNT_CHANGED)
            }
            return WatchAcknowledgement(command.commandId, AckStatus.ACCEPTED, snapshot, accountToken)
        }
        if (command.accountToken != accountToken) {
            return WatchAcknowledgement.rejected(command, WatchFailure.ACCOUNT_CHANGED)
        }
        // The account token and source node prevent stale/cross-device command reuse.
        val key = "$sourceNode/${command.commandId}"
        journal[key]?.let { entry ->
            return if (entry.command == command) {
                val snapshot = gateway.snapshot()
                if (authGeneration.get() != generation || gateway.currentOwnerId() != activeOwner) {
                    WatchAcknowledgement.rejected(command, WatchFailure.ACCOUNT_CHANGED)
                } else entry.acknowledgement.copy(status = AckStatus.DUPLICATE, state = snapshot)
            } else WatchAcknowledgement.rejected(command, WatchFailure.COMMAND_ID_REUSED)
        }
        return try {
            val before = gateway.snapshot()
            if (command.action != WatchAction.START && command.sessionId != before.sessionId) {
                return WatchAcknowledgement.rejected(command, WatchFailure.WRONG_SESSION)
            }
            val allowed = when (command.action) {
                WatchAction.START -> before.phase in setOf(WatchPhase.IDLE, WatchPhase.COMPLETE)
                WatchAction.PAUSE -> before.phase == WatchPhase.LIVE
                WatchAction.RESUME -> before.phase == WatchPhase.PAUSED
                WatchAction.FINISH -> before.phase in setOf(WatchPhase.LIVE, WatchPhase.PAUSED) ||
                    (before.phase == WatchPhase.COMPLETE && !before.demo)
                WatchAction.READ_STATE -> false
            }
            if (!allowed) return WatchAcknowledgement.rejected(command, WatchFailure.INVALID_TRANSITION)
            // Persistence may have completed on the phone after the previous reply
            // failed. Confirm that exact session without finishing it a second time.
            val state = if (command.action == WatchAction.FINISH && before.phase == WatchPhase.COMPLETE && before.saved) {
                before
            } else gateway.execute(command, activeOwner)
            if (authGeneration.get() != generation || gateway.currentOwnerId() != activeOwner) {
                return WatchAcknowledgement.rejected(command, WatchFailure.ACCOUNT_CHANGED)
            }
            val expectedPhase = when (command.action) {
                WatchAction.START, WatchAction.RESUME -> WatchPhase.LIVE
                WatchAction.PAUSE -> WatchPhase.PAUSED
                WatchAction.FINISH -> WatchPhase.COMPLETE
                WatchAction.READ_STATE -> WatchPhase.IDLE
            }
            if (state.sessionId != command.sessionId || state.phase != expectedPhase) {
                return WatchAcknowledgement.rejected(command, WatchFailure.REMOTE_ERROR, true)
            }
            if (command.action == WatchAction.FINISH && !state.demo && !state.saved) {
                return WatchAcknowledgement.rejected(command, WatchFailure.SAVE_FAILED, true)
            }
            val ack = WatchAcknowledgement(command.commandId, AckStatus.ACCEPTED, state, accountToken)
            journal[key] = Entry(command, ack)
            // Never evict accepted IDs and accidentally replay an old START. Start a new
            // account generation after a full journal; new requests first refresh state.
            if (journal.size >= 256) {
                accountToken = UUID.randomUUID().toString()
                journal.clear()
            }
            ack
        } catch (error: WatchCommandException) {
            WatchAcknowledgement.rejected(command, error.reason, error.retryable)
        }
    }

    private fun valid(command: WatchCommand): Boolean {
        if (command.version != 1 || !isUuid(command.commandId)) return false
        if (command.action == WatchAction.READ_STATE) return command.sessionId == null && command.options == null
        if (!isUuid(command.sessionId)) return false
        if (command.action != WatchAction.START) return command.options == null
        val options = command.options ?: return false
        return when (options.goalType) {
            WatchGoalType.OPEN -> options.goalValue == null
            WatchGoalType.DURATION_SECONDS -> options.goalValue?.let { it.isFinite() && it in 1.0..86400.0 } == true
            WatchGoalType.DISTANCE_METRES -> options.goalValue?.let { it.isFinite() && it in 1.0..1_000_000.0 } == true
        }
    }

    private fun isUuid(value: String?): Boolean = value != null &&
        value.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
}
