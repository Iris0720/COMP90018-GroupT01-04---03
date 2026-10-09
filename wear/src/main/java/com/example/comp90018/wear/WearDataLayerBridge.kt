package com.example.comp90018.wear

import android.content.Context
import com.example.comp90018.watch.*
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** sendRequest returns the phone's application ACK, not merely a transport message ID. */
class WearDataLayerBridge(context: Context) : WatchBridge {
    private val context = context.applicationContext
    private var selectedPhone: String? = null

    override suspend fun sendWatchCommand(command: WatchCommand): WatchAcknowledgement = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(Wearable.getCapabilityClient(context)
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE), 5, TimeUnit.SECONDS).nodes
            if (nodes.isEmpty()) return@withContext WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
            // Never broadcast a START or silently move an uncertain retry to another phone.
            val phone = selectedPhone
            val node = if (phone != null) nodes.singleOrNull { it.id == phone } else nodes.singleOrNull()
            if (node == null) return@withContext WatchAcknowledgement.rejected(command,
                if (phone == null) WatchFailure.AMBIGUOUS_PHONE else WatchFailure.DISCONNECTED, true)
            selectedPhone = node.id
            val bytes = Tasks.await(Wearable.getMessageClient(context)
                .sendRequest(node.id, WATCH_COMMAND_PATH, WatchWire.encode(command)), 8, TimeUnit.SECONDS)
            WatchWire.acknowledgement(bytes)
        } catch (_: TimeoutException) {
            WatchAcknowledgement.rejected(command, WatchFailure.TIMEOUT, true)
        } catch (error: java.util.concurrent.CancellationException) {
            throw error
        } catch (_: Exception) {
            WatchAcknowledgement.rejected(command, WatchFailure.DISCONNECTED, true)
        }
    }
}
