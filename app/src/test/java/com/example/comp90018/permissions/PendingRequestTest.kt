package com.example.comp90018.permissions

import org.junit.Assert.*
import org.junit.Test

class PendingRequestTest {
    @Test fun completionCanStartAnotherPermissionWithoutLosingIt() {
        val pending = PendingRequest<String, Boolean>()
        var started = false
        pending.begin("location") {
            pending.begin("steps") { started = true }
        }
        pending.complete(true)
        assertEquals("steps", pending.type)
        pending.complete(false)
        assertTrue(started) // Optional permission denial still continues the activity.
        assertNull(pending.type)
    }

    @Test fun cancellationCompletesOnceAndReleasesRequest() {
        val pending = PendingRequest<String, Boolean>()
        var calls = 0
        pending.begin("steps") { result -> assertFalse(result); calls++ }
        pending.complete(false)
        pending.complete(false)
        assertEquals(1, calls)
        assertTrue(pending.begin("camera") {})
    }

    @Test fun simultaneousRequestDoesNotOverwriteOriginalCallback() {
        val pending = PendingRequest<String, Boolean>()
        var originalCalled = false
        pending.begin("camera") { originalCalled = true }
        assertFalse(pending.begin("notifications") { fail("Must not replace camera request") })
        pending.complete(true)
        assertTrue(originalCalled)
    }
}
