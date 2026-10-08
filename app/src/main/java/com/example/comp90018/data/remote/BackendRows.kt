package com.example.comp90018.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("daily_step_target") val dailyStepTarget: Int,
    @SerialName("unit_system") val unitSystem: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ProfileUpdateRow(
    @SerialName("display_name") val displayName: String,
    @SerialName("daily_step_target") val dailyStepTarget: Int,
    @SerialName("unit_system") val unitSystem: String
)

@Serializable
internal data class ActivitySessionWriteRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String?,
    @SerialName("activity_type") val activityType: String,
    @SerialName("session_state") val sessionState: String,
    @SerialName("goal_type") val goalType: String?,
    @SerialName("goal_value") val goalValue: Long?,
    @SerialName("duration_seconds") val durationSeconds: Long,
    @SerialName("distance_metres") val distanceMetres: Long,
    val steps: Long?
)

@Serializable
internal data class RoutePointWriteRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("segment_number") val segmentNumber: Int,
    @SerialName("sequence_number") val sequenceNumber: Int,
    @SerialName("recorded_at") val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_metres") val accuracyMetres: Float? = null,
    @SerialName("altitude_metres") val altitudeMetres: Float? = null,
    @SerialName("speed_metres_per_second") val speedMetresPerSecond: Float? = null
)

@Serializable
internal data class HealthCheckInWriteRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("recorded_at") val recordedAt: String,
    val feeling: String,
    @SerialName("breathing_difficulty") val breathingDifficulty: String,
    val fatigue: String,
    val condition: String
)

@Serializable
internal data class MealEntryWriteRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("recorded_at") val recordedAt: String,
    val name: String,
    val serving: String,
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Int,
    @SerialName("carbohydrate_grams") val carbohydrateGrams: Int,
    @SerialName("fat_grams") val fatGrams: Int,
    @SerialName("source_label") val sourceLabel: String,
    @SerialName("photo_storage_path") val photoStoragePath: String? = null
)

@Serializable
data class ActivitySessionRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String?,
    @SerialName("activity_type") val activityType: String,
    @SerialName("session_state") val sessionState: String,
    @SerialName("goal_type") val goalType: String?,
    @SerialName("goal_value") val goalValue: Long?,
    @SerialName("duration_seconds") val durationSeconds: Long,
    @SerialName("distance_metres") val distanceMetres: Long,
    val steps: Long?,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RoutePointRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("segment_number") val segmentNumber: Int,
    @SerialName("sequence_number") val sequenceNumber: Int,
    @SerialName("recorded_at") val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_metres") val accuracyMetres: Float? = null,
    @SerialName("altitude_metres") val altitudeMetres: Float? = null,
    @SerialName("speed_metres_per_second") val speedMetresPerSecond: Float? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class HealthCheckInRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("recorded_at") val recordedAt: String,
    val feeling: String,
    @SerialName("breathing_difficulty") val breathingDifficulty: String,
    val fatigue: String,
    val condition: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class MealEntryRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("recorded_at") val recordedAt: String,
    val name: String,
    val serving: String,
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Int,
    @SerialName("carbohydrate_grams") val carbohydrateGrams: Int,
    @SerialName("fat_grams") val fatGrams: Int,
    @SerialName("source_label") val sourceLabel: String,
    @SerialName("photo_storage_path") val photoStoragePath: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)
