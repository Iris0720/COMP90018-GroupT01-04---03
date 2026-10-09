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

    suspend fun refresh() = run(WatchCommand(UUID.randomUUID().toString(), WatchAction.READ_STATE))

    suspend fun command(action: WatchAction, options: WatchStartOptions? = null) {
        if (state.busy || pending != null) return
        val token = accountToken
        if (token == null) {
            update(state.copy(failure = WatchFailure.NOT_READY))
            return
        }
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
                (ack.successful && (ack.state == null || ack.accountToken == null))) {
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

    private fun update(value: WatchUiState) { state = value; onChange(value) }
}
