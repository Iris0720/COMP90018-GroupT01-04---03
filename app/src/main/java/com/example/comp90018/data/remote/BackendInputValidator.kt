package com.example.comp90018.data.remote

import java.time.Instant

internal object BackendInputValidator {
    fun profile(input: ProfileUpdate): String? = when {
        input.displayName.length > 80 -> "Display name must be at most 80 characters."
        input.dailyStepTarget !in 0..100_000 -> "Daily step target must be between 0 and 100000."
        input.unitSystem !in BackendValues.unitSystems -> "Unit system must be metric or imperial."
        else -> null
    }

    fun activity(input: ActivitySessionInput): String? = when {
        !isUuid(input.id) -> "Activity id must be a UUID."
        !isTimestamp(input.startedAt) -> "Activity start time must be an ISO-8601 timestamp."
        input.finishedAt != null && !isTimestamp(input.finishedAt) ->
            "Activity finish time must be an ISO-8601 timestamp."
        input.finishedAt != null && Instant.parse(input.finishedAt).isBefore(Instant.parse(input.startedAt)) ->
            "Activity finish time cannot be before its start time."
        input.activityType !in BackendValues.activityTypes -> "Unsupported activity type."
        input.sessionState !in BackendValues.sessionStates -> "Unsupported session state."
        input.goalType != null && input.goalType !in BackendValues.goalTypes -> "Unsupported goal type."
        input.goalValue != null && input.goalValue < 0 -> "Goal value cannot be negative."
        input.durationSeconds < 0 -> "Duration cannot be negative."
        input.distanceMetres < 0 -> "Distance cannot be negative."
        input.steps != null && input.steps < 0 -> "Steps cannot be negative."
        else -> null
    }

    fun routePoint(input: RoutePointInput, expectedSessionId: String): String? = when {
        !isUuid(input.id) -> "Route point id must be a UUID."
        input.sessionId != expectedSessionId -> "Route point belongs to a different session."
        !isUuid(input.sessionId) -> "Session id must be a UUID."
        input.segmentNumber < 0 -> "Route segment number cannot be negative."
        input.sequenceNumber < 0 -> "Route sequence number cannot be negative."
        !isTimestamp(input.recordedAt) -> "Route time must be an ISO-8601 timestamp."
        input.latitude !in -90.0..90.0 -> "Latitude is outside -90..90."
        input.longitude !in -180.0..180.0 -> "Longitude is outside -180..180."
        listOfNotNull(input.accuracyMetres, input.altitudeMetres, input.speedMetresPerSecond)
            .any { !it.isFinite() } -> "Sensor values must be finite."
        input.accuracyMetres != null && input.accuracyMetres < 0 -> "Accuracy cannot be negative."
        input.speedMetresPerSecond != null && input.speedMetresPerSecond < 0 -> "Speed cannot be negative."
        else -> null
    }

    fun health(input: HealthCheckInInput): String? = when {
        !isUuid(input.id) -> "Health check-in id must be a UUID."
        !isTimestamp(input.recordedAt) -> "Check-in time must be an ISO-8601 timestamp."
        input.feeling !in BackendValues.feelings -> "Unsupported feeling value."
        input.breathingDifficulty !in BackendValues.symptomLevels -> "Unsupported breathing value."
        input.fatigue !in BackendValues.symptomLevels -> "Unsupported fatigue value."
        input.condition !in BackendValues.healthConditions -> "Unsupported condition value."
        else -> null
    }

    fun meal(input: MealEntryInput): String? = when {
        !isUuid(input.id) -> "Meal id must be a UUID."
        !isTimestamp(input.recordedAt) -> "Meal time must be an ISO-8601 timestamp."
        input.name.isBlank() || input.name.length > 160 -> "Meal name must contain 1 to 160 characters."
        input.serving.isBlank() || input.serving.length > 160 -> "Serving must contain 1 to 160 characters."
        input.sourceLabel.isBlank() -> "Meal source label is required."
        listOf(
            input.calories,
            input.proteinGrams,
            input.carbohydrateGrams,
            input.fatGrams
        ).any { it < 0 } -> "Nutrition values cannot be negative."
        else -> null
    }

    private fun isTimestamp(value: String): Boolean = runCatching { Instant.parse(value) }.isSuccess

    fun isUuid(value: String): Boolean = UUID_PATTERN.matches(value)
    private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
}
