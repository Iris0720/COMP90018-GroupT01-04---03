package com.example.comp90018.watch

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PhoneWatchEndpointTest {
    @Test fun realServiceRpcCodecReturnsAcknowledgementAndRejectsDuplicates() {
        val gateway = DemoPhoneGateway()
        val dispatcher = PhoneWatchEndpoint.install(gateway)
        val service = PhoneWatchRpcHandler()
        try {
            val read = WatchCommand(UUID.randomUUID().toString(), WatchAction.READ_STATE)
            val reply = com.google.android.gms.tasks.Tasks.await(service.request("test-node", WATCH_COMMAND_PATH, WatchWire.encode(read)))
            val state = WatchWire.acknowledgement(reply)
            assertEquals(WatchPhase.IDLE, state.state!!.phase)
            val start = WatchCommand(UUID.randomUUID().toString(), WatchAction.START, UUID.randomUUID().toString(),
                WatchStartOptions(), state.accountToken)
            fun execute() = WatchWire.acknowledgement(com.google.android.gms.tasks.Tasks.await(
                service.request("test-node", WATCH_COMMAND_PATH, WatchWire.encode(start))))
            assertEquals(AckStatus.ACCEPTED, execute().status)
            assertEquals(AckStatus.DUPLICATE, execute().status)
            assertEquals(1, gateway.executions)
            gateway.signedIn = false
            assertEquals(WatchFailure.SIGNED_OUT, WatchWire.acknowledgement(com.google.android.gms.tasks.Tasks.await(
                service.request("test-node", WATCH_COMMAND_PATH, WatchWire.encode(read)))).failure)
        } finally {
            PhoneWatchEndpoint.uninstall(dispatcher)
            service.close()
        }
    }

    @Test fun unconfiguredPhoneFailsClosed() {
        val command = WatchCommand(UUID.randomUUID().toString(), WatchAction.READ_STATE)
        assertEquals(WatchFailure.NOT_READY, PhoneWatchEndpoint.sendWatchCommand("test-node", command).failure)
    }
}
