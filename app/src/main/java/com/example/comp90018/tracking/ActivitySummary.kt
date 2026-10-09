package com.example.comp90018.tracking

data class ActivitySummary(
    val sessionId: String,
    val activityType: String,
    val target: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val activeDurationMillis: Long,
    val distanceMetres: Float,
    val averagePaceSecondsPerKm: Double?,
    val recordedSteps: Long?,
    val routeSegments: List<List<TrackPoint>>
)
