package com.example.comp90018.watch

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors

class PhoneWatchListenerService : WearableListenerService() {
    private val rpc = PhoneWatchRpcHandler()
    override fun onRequest(nodeId: String, path: String, data: ByteArray): Task<ByteArray> = rpc.request(nodeId, path, data)

    override fun onDestroy() {
        rpc.close()
        super.onDestroy()
    }
}

/** Same RPC handler used by the manifest service and device tests. */
class PhoneWatchRpcHandler : AutoCloseable {
    // One worker serializes RPC execution. Never run database/controller work on main.
    private val worker = Executors.newSingleThreadExecutor()

    fun request(nodeId: String, path: String, data: ByteArray): Task<ByteArray> {
        val response = TaskCompletionSource<ByteArray>()
        worker.execute {
            try {
                require(path == WATCH_COMMAND_PATH)
                val command = WatchWire.command(data)
                response.setResult(WatchWire.encode(PhoneWatchEndpoint.sendWatchCommand(nodeId, command)))
            } catch (error: Exception) {
                response.setException(error)
            }
        }
        return response.task
    }

    override fun close() {
        worker.shutdown()
    }
}
