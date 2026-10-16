package com.example.comp90018.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchRefreshPolicyTest {
    @Test fun localDemoRefreshesEverySecond() {
        assertEquals(1_000L, watchRefreshIntervalMillis(true))
    }

    @Test fun phoneCompanionKeepsThreeSecondPolling() {
        assertEquals(3_000L, watchRefreshIntervalMillis(false))
    }
}
