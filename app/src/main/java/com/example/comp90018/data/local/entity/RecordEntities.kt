package com.example.comp90018.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "activity_records",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(value = ["ownerId", "recordedAtEpochMillis"])
    ]
)
data class ActivityRecordEntity(
    val id: String,
    val ownerId: String,
    val recordedAtEpochMillis: Long,
    val activityType: String,
    val durationSeconds: Long,
    val distanceMetres: Long,
    val steps: Long?
)

@Entity(
    tableName = "health_check_in_records",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(value = ["ownerId", "recordedAtEpochMillis"])
    ]
)
data class HealthCheckInRecordEntity(
    val id: String,
    val ownerId: String,
    val recordedAtEpochMillis: Long,
    val feeling: String,
    val breathingDifficulty: String,
    val fatigue: String,
    val condition: String
)
