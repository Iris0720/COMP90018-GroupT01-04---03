package com.example.comp90018.wear

import com.example.comp90018.watch.*
import org.junit.Assert.*
import org.junit.Test

class WatchSetupOptionsTest {
    @Test fun choicesUseSecondsAndMetresWithoutImplicitGoal() {
        assertNull(watchStartOptions(0, 0).goalValue)
        assertEquals(WatchGoalType.OPEN, watchStartOptions(0, 0).goalType)
        assertEquals(900.0, watchStartOptions(0, 1).goalValue)
        assertEquals(1800.0, watchStartOptions(0, 2).goalValue)
        assertEquals(3600.0, watchStartOptions(0, 3).goalValue)
        assertEquals(1000.0, watchStartOptions(0, 4).goalValue)
        assertEquals(5000.0, watchStartOptions(0, 5).goalValue)
        assertEquals(WatchGoalType.DISTANCE_METRES, watchStartOptions(0, 5).goalType)
    }

    @Test fun allChoicesPassRealDispatcherValidation() {
        for (activity in WatchActivityType.entries.indices) for (goal in watchGoalChoices.indices) {
            val dispatcher = PhoneCommandDispatcher(DemoPhoneGateway())
            val handshake = dispatcher.sendWatchCommand("setup-test", WatchCommand(java.util.UUID.randomUUID().toString(), WatchAction.READ_STATE))
            val ack = dispatcher.sendWatchCommand("setup-test", WatchCommand(java.util.UUID.randomUUID().toString(),
                WatchAction.START, java.util.UUID.randomUUID().toString(), watchStartOptions(activity, goal), handshake.accountToken))
            assertTrue("activity=$activity goal=$goal", ack.successful)
            assertEquals(WatchActivityType.entries[activity], ack.state!!.activityType)
        }
    }

    @Test fun invalidRestoredIndicesFallBackSafely() {
        assertEquals(WatchStartOptions(), watchStartOptions(-1, 99))
    }

    @Test fun elapsedTimeDoesNotWrapAfterAnHour() {
        assertEquals("00:00:00", watchElapsedTime(-1))
        assertEquals("00:00:59", watchElapsedTime(59))
        assertEquals("01:01:01", watchElapsedTime(3661))
        assertEquals("27:00:00", watchElapsedTime(27 * 3600))
    }
}
