package com.example.comp90018.watch

import java.util.UUID
import java.util.concurrent.CancellationException

data class WatchUiState(
    val session: WatchSessionState? = null,
    val busy: Boolean = false,
    val stale: Boolean = false,
    val failure: WatchFailure? = null,
    val canRetry: Boolean = false,
    val lastStatus: AckStatus? = null
)

/** UI changes only after a correlated reply. Failed/uncertain mutations retain their IDs. */
class WatchController(
    private val bridge: WatchBridge,
    private val onChange: (WatchUiState) -> Unit = {}
) {
    var state = WatchUiState()
        private set
    private var accountToken: String? = null
    private var pending: WatchCommand? = null

    /** Never abandon a live session or a mutation whose delivery is still uncertain. */
    fun resetForModeChange(): Boolean {
        if (state.busy || pending != null || state.session?.phase in setOf(WatchPhase.LIVE, WatchPhase.PAUSED)) return false
        if (state.session?.let { !it.demo && it.phase == WatchPhase.COMPLETE && !it.saved } == true) return false
        accountToken = null
        update(WatchUiState())
        return true
    }

    suspend fun refresh() = run(WatchCommand(UUID.randomUUID().toString(), WatchAction.READ_STATE))

    suspend fun command(action: WatchAction, options: WatchStartOptions? = null) {
        if (action == WatchAction.READ_STATE) { refresh(); return }
        if (state.busy || pending != null) return
        val token = accountToken
        if (token == null) {
            update(state.copy(failure = WatchFailure.NOT_READY))
            return
        }
        if (state.stale) return
        val request = WatchCommand(UUID.randomUUID().toString(), action,
            if (action == WatchAction.START) UUID.randomUUID().toString() else state.session?.sessionId,
            options, token)
        run(request)
    }

    suspend fun retry() { pending?.let { run(it) } }

    private suspend fun run(command: WatchCommand) {
        if (state.busy) return
        update(state.copy(busy = true, failure = null))
        try {
            val ack = bridge.sendWatchCommand(command)
            if (ack.commandId != command.commandId || ack.version != 1 ||
                (ack.successful && !validSuccessfulReply(command, ack))) {
                uncertain(command, WatchFailure.REMOTE_ERROR)
                return
            }
            if (ack.successful) {
                if (accountToken != null && accountToken != ack.accountToken) pending = null
                accountToken = ack.accountToken
                if (command.action != WatchAction.READ_STATE) pending = null
                update(WatchUiState(ack.state, canRetry = pending != null, lastStatus = ack.status))
            } else {
                val reason = ack.failure ?: WatchFailure.REMOTE_ERROR
                if (reason == WatchFailure.SIGNED_OUT || reason == WatchFailure.ACCOUNT_CHANGED) {
                    accountToken = null
                    pending = null
                    update(WatchUiState(failure = reason))
                } else {
                    if (command.action != WatchAction.READ_STATE) pending = if (ack.retryable) command else null
                    update(state.copy(busy = false, stale = true, failure = reason,
                        canRetry = pending != null, lastStatus = AckStatus.REJECTED))
                }
            }
        } catch (error: CancellationException) {
            uncertain(command, WatchFailure.TIMEOUT)
            throw error
        } catch (_: Exception) {
            uncertain(command, WatchFailure.REMOTE_ERROR)
        } finally {
            if (state.busy) update(state.copy(busy = false))
        }
    }

    private fun uncertain(command: WatchCommand, failure: WatchFailure) {
        if (command.action != WatchAction.READ_STATE) pending = command
        update(state.copy(busy = false, stale = true, failure = failure, canRetry = pending != null))
    }

    private fun validSuccessfulReply(command: WatchCommand, ack: WatchAcknowledgement): Boolean {
        val session = ack.state ?: return false
        if (ack.accountToken.isNullOrBlank() || ack.failure != null) return false
        if (session.durationSeconds < 0 || !session.distanceMetres.isFinite() || session.distanceMetres < 0 ||
            session.steps?.let { it < 0 } == true || session.revision < 0) return false
        if (session.phase == WatchPhase.IDLE) {
            if (session.sessionId != null || session.saved) return false
        } else if (session.sessionId?.matches(SESSION_ID) != true || session.activityType == null) return false
        if (session.saved && session.phase != WatchPhase.COMPLETE) return false
        if (command.action == WatchAction.READ_STATE) return true
        if (session.sessionId != command.sessionId || ack.accountToken != command.accountToken) return false
        // A cached duplicate may report a later phone phase; an initial ACCEPTED reply may not.
        if (ack.status == AckStatus.ACCEPTED) {
            val expected = when (command.action) {
                WatchAction.START, WatchAction.RESUME -> WatchPhase.LIVE
                WatchAction.PAUSE -> WatchPhase.PAUSED
                WatchAction.FINISH -> WatchPhase.COMPLETE
                WatchAction.READ_STATE -> return true
            }
            if (session.phase != expected) return false
        }
        if (command.action == WatchAction.FINISH && !session.demo &&
            (session.phase != WatchPhase.COMPLETE || !session.saved)) return false
        return true
    }

    private companion object {
        val SESSION_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }

    private fun update(value: WatchUiState) { state = value; onChange(value) }
}
