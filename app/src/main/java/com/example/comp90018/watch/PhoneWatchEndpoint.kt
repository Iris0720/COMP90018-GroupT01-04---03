package com.example.comp90018.watch

/** Fail closed until the real phone session owner installs its gateway.
 * No default guest/demo gateway is enabled in the production app.
 */
object PhoneWatchEndpoint {
    @Volatile private var dispatcher: PhoneCommandDispatcher? = null

    @Synchronized fun install(gateway: PhoneSessionGateway): PhoneCommandDispatcher =
        PhoneCommandDispatcher(gateway).also { dispatcher = it }

    @Synchronized fun uninstall(installed: PhoneCommandDispatcher) {
        if (dispatcher === installed) dispatcher = null
    }

    /** Call on each authentication event before accepting more commands. */
    fun invalidateAccount() { dispatcher?.invalidateAccount() }

    fun sendWatchCommand(sourceNode: String, command: WatchCommand): WatchAcknowledgement = try {
        dispatcher?.sendWatchCommand(sourceNode, command)
            ?: WatchAcknowledgement.rejected(command, WatchFailure.NOT_READY)
    } catch (_: Exception) {
        WatchAcknowledgement.rejected(command, WatchFailure.REMOTE_ERROR, true)
    }
}
