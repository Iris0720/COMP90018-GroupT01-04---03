package com.example.comp90018.watch

/** Explicit demo: timer and synthetic distance/steps, no GPS or database save. */
class DemoPhoneGateway(private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }) : PhoneSessionGateway {
    @Volatile var signedIn = true
        set(value) { synchronized(this) { field = value } }
    @Volatile var connected = true
    var executions = 0
        private set
    private var state = WatchSessionState(demo = true)
    private var accumulatedMillis = 0L
    private var segmentStart = 0L

    override fun currentOwnerId(): String? = if (signedIn) "demo-account" else null

    @Synchronized override fun snapshot(): WatchSessionState {
        val elapsed = accumulatedMillis + if (state.phase == WatchPhase.LIVE) {
            (nowMillis() - segmentStart).coerceAtLeast(0)
        } else 0
        val seconds = elapsed / 1000
        return state.copy(durationSeconds = seconds, distanceMetres = seconds * 1.2, steps = seconds * 2)
    }

    @Synchronized override fun execute(command: WatchCommand, expectedOwnerId: String): WatchSessionState {
        if (currentOwnerId() != expectedOwnerId) throw WatchCommandException(WatchFailure.ACCOUNT_CHANGED)
        if (!connected) throw WatchCommandException(WatchFailure.DISCONNECTED, true)
        when (command.action) {
            WatchAction.START -> {
                if (command.sessionId == state.sessionId) throw WatchCommandException(WatchFailure.INVALID_TRANSITION)
                accumulatedMillis = 0
                segmentStart = nowMillis()
                state = WatchSessionState(command.sessionId, WatchPhase.LIVE, command.options!!.activityType,
                    revision = state.revision + 1, demo = true)
            }
            WatchAction.PAUSE, WatchAction.FINISH -> {
                if (state.phase == WatchPhase.LIVE) accumulatedMillis += (nowMillis() - segmentStart).coerceAtLeast(0)
                state = state.copy(phase = if (command.action == WatchAction.PAUSE) WatchPhase.PAUSED else WatchPhase.COMPLETE,
                    revision = state.revision + 1)
            }
            WatchAction.RESUME -> {
                segmentStart = nowMillis()
                state = state.copy(phase = WatchPhase.LIVE, revision = state.revision + 1)
            }
            WatchAction.READ_STATE -> Unit
        }
        executions++
        return snapshot()
    }
}
