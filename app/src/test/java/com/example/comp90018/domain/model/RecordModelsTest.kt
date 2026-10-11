package com.example.comp90018.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordModelsTest {
    @Test
    fun zeroDistance_hasNoPace() {
        val record = activityRecord(distanceMetres = 0)

        assertNull(record.paceSecondsPerKilometre)
    }

    @Test
    fun pace_usesSecondsPerKilometre() {
        val record = activityRecord(durationSeconds = 600, distanceMetres = 2_000)

        assertEquals(300.0, record.paceSecondsPerKilometre!!, 0.0)
    }

    @Test
    fun negativeDistance_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            activityRecord(distanceMetres = -1)
        }
    }

    private fun activityRecord(
        durationSeconds: Long = 600,
        distanceMetres: Long = 1_000
    ) = ActivityRecord(
        id = "activity-1",
        ownerId = "owner-1",
        recordedAtEpochMillis = 1_000,
        activityType = ActivityType.WALKING,
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        steps = 1_200
    )
}
