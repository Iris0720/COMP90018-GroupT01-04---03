package com.example.comp90018.data.repository

import com.example.comp90018.data.local.entity.ActivityRecordEntity
import com.example.comp90018.data.local.entity.HealthCheckInRecordEntity
import com.example.comp90018.domain.model.ActivityRecord
import com.example.comp90018.domain.model.ActivityRecordDraft
import com.example.comp90018.domain.model.ActivityType
import com.example.comp90018.domain.model.Feeling
import com.example.comp90018.domain.model.HealthCheckInDraft
import com.example.comp90018.domain.model.HealthCheckInRecord
import com.example.comp90018.domain.model.HealthCondition
import com.example.comp90018.domain.model.SymptomLevel

internal fun ActivityRecordDraft.toEntity(ownerId: String) = ActivityRecordEntity(
    id = id,
    ownerId = ownerId,
    recordedAtEpochMillis = recordedAtEpochMillis,
    activityType = activityType.name,
    durationSeconds = durationSeconds,
    distanceMetres = distanceMetres,
    steps = steps
)

internal fun ActivityRecordEntity.toDomain() = ActivityRecord(
    id = id,
    ownerId = ownerId,
    recordedAtEpochMillis = recordedAtEpochMillis,
    activityType = ActivityType.valueOf(activityType),
    durationSeconds = durationSeconds,
    distanceMetres = distanceMetres,
    steps = steps
)

internal fun HealthCheckInDraft.toEntity(ownerId: String) = HealthCheckInRecordEntity(
    id = id,
    ownerId = ownerId,
    recordedAtEpochMillis = recordedAtEpochMillis,
    feeling = feeling.name,
    breathingDifficulty = breathingDifficulty.name,
    fatigue = fatigue.name,
    condition = condition.name
)

internal fun HealthCheckInRecordEntity.toDomain() = HealthCheckInRecord(
    id = id,
    ownerId = ownerId,
    recordedAtEpochMillis = recordedAtEpochMillis,
    feeling = Feeling.valueOf(feeling),
    breathingDifficulty = SymptomLevel.valueOf(breathingDifficulty),
    fatigue = SymptomLevel.valueOf(fatigue),
    condition = HealthCondition.valueOf(condition)
)
