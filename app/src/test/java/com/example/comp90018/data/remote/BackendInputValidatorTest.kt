package com.example.comp90018.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendInputValidatorTest {
    @Test
    fun validActivityAndRouteAreAccepted() {
        val activity = validActivity()
        val point = validPoint(activity.id)

        assertNull(BackendInputValidator.activity(activity))
        assertNull(BackendInputValidator.routePoint(point, activity.id))
    }

    @Test
    fun activityFinishBeforeStartIsRejected() {
        val error = BackendInputValidator.activity(
            validActivity().copy(finishedAt = "2026-10-09T09:59:59Z")
        )

        assertEquals("Activity finish time cannot be before its start time.", error)
    }

    @Test
    fun routeCannotBeAttachedToAnotherSession() {
        val error = BackendInputValidator.routePoint(validPoint("session-b"), "session-a")

        assertEquals("Route point belongs to a different session.", error)
    }

    @Test
    fun invalidProfileTargetAndUnitAreRejected() {
        assertEquals(
            "Daily step target must be between 0 and 100000.",
            BackendInputValidator.profile(ProfileUpdate("Jingtian", 100_001, "metric"))
        )
        assertEquals(
            "Unit system must be metric or imperial.",
            BackendInputValidator.profile(ProfileUpdate("Jingtian", 10_000, "unknown"))
        )
    }

    @Test
    fun incompleteHealthValuesAreRejected() {
        val input = HealthCheckInInput(
            id = "health-1",
            recordedAt = "2026-10-09T10:00:00Z",
            feeling = "great",
            breathingDifficulty = "none",
            fatigue = "none",
            condition = "good"
        )

        assertEquals("Unsupported feeling value.", BackendInputValidator.health(input))
    }

    @Test
    fun negativeMealNutritionIsRejected() {
        val input = MealEntryInput(
            id = "meal-1",
            recordedAt = "2026-10-09T10:00:00Z",
            name = "Rice bowl",
            serving = "1 bowl",
            calories = 500,
            proteinGrams = -1,
            carbohydrateGrams = 60,
            fatGrams = 12
        )

        assertEquals("Nutrition values cannot be negative.", BackendInputValidator.meal(input))
    }

    private fun validActivity() = ActivitySessionInput(
        id = "session-1",
        startedAt = "2026-10-09T10:00:00Z",
        finishedAt = "2026-10-09T10:30:00Z",
        activityType = "walking",
        durationSeconds = 1_800,
        distanceMetres = 2_000,
        steps = 2_500
    )

    private fun validPoint(sessionId: String) = RoutePointInput(
        id = "point-1",
        sessionId = sessionId,
        segmentNumber = 0,
        sequenceNumber = 0,
        recordedAt = "2026-10-09T10:00:05Z",
        latitude = -37.8136,
        longitude = 144.9631,
        accuracyMetres = 5f
    )
}
