package com.example.comp90018.data.remote

/** Values accepted by the database constraints in the Supabase migrations. */
object BackendValues {
    val activityTypes = setOf("walking", "running", "hiking")
    val sessionStates = setOf("setup", "live", "paused", "complete")
    val goalTypes = setOf("time", "distance", "open")
    val feelings = setOf("good", "okay", "unwell")
    val symptomLevels = setOf("none", "mild", "severe")
    val healthConditions = setOf("good", "caution", "unknown")
    val unitSystems = setOf("metric", "imperial")
}

data class ProfileUpdate(
    val displayName: String,
    val dailyStepTarget: Int,
    val unitSystem: String
)

data class ActivitySessionInput(
    val id: String,
    val startedAt: String,
    val finishedAt: String?,
    val activityType: String,
    val sessionState: String = "complete",
    val goalType: String? = null,
    val goalValue: Long? = null,
    val durationSeconds: Long,
    val distanceMetres: Long,
    val steps: Long?
)

data class RoutePointInput(
    val id: String,
    val sessionId: String,
    val segmentNumber: Int,
    val sequenceNumber: Int,
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMetres: Float? = null,
    val altitudeMetres: Float? = null,
    val speedMetresPerSecond: Float? = null
)

data class HealthCheckInInput(
    val id: String,
    val recordedAt: String,
    val feeling: String,
    val breathingDifficulty: String,
    val fatigue: String,
    val condition: String
)

data class MealEntryInput(
    val id: String,
    val recordedAt: String,
    val name: String,
    val serving: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbohydrateGrams: Int,
    val fatGrams: Int,
    val sourceLabel: String = "Manual entry",
    val photoStoragePath: String? = null
)
