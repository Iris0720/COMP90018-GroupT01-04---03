package com.example.comp90018.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendRowsSerializationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun activityPayloadUsesDatabaseColumnNames() {
        val payload = json.encodeToString(
            ActivitySessionRow(
                id = "session-1",
                ownerId = "user-1",
                startedAt = "2026-10-09T10:00:00Z",
                finishedAt = null,
                activityType = "walking",
                sessionState = "live",
                goalType = "time",
                goalValue = 1800,
                durationSeconds = 10,
                distanceMetres = 20,
                steps = 25
            )
        )

        assertTrue(payload.contains("\"owner_id\":\"user-1\""))
        assertTrue(payload.contains("\"started_at\":\"2026-10-09T10:00:00Z\""))
        assertTrue(payload.contains("\"duration_seconds\":10"))
        assertFalse(payload.contains("ownerId"))
    }

    @Test
    fun mealPayloadUsesNutritionColumnNames() {
        val payload = json.encodeToString(
            MealEntryRow(
                id = "meal-1",
                ownerId = "user-1",
                recordedAt = "2026-10-09T12:00:00Z",
                name = "Rice bowl",
                serving = "1 bowl",
                calories = 500,
                proteinGrams = 30,
                carbohydrateGrams = 60,
                fatGrams = 12,
                sourceLabel = "Manual entry"
            )
        )

        assertTrue(payload.contains("\"protein_grams\":30"))
        assertTrue(payload.contains("\"carbohydrate_grams\":60"))
        assertTrue(payload.contains("\"source_label\":\"Manual entry\""))
    }
}
