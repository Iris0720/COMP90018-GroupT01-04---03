package com.example.comp90018.domain.model

/** A persisted record that always belongs to one authenticated account. */
sealed interface Record {
    val id: String
    val ownerId: String
    val recordedAtEpochMillis: Long
}

/** Owner-free input accepted by Package 4. The repository supplies the active owner. */
sealed interface RecordDraft {
    val id: String
    val recordedAtEpochMillis: Long
}

enum class ActivityType {
    WALKING,
    RUNNING,
    HIKING
}

data class ActivityRecordDraft(
    override val id: String,
    override val recordedAtEpochMillis: Long,
    val activityType: ActivityType,
    val durationSeconds: Long,
    val distanceMetres: Long,
    val steps: Long?
) : RecordDraft {
    init {
        validateActivityFields(
            id = id,
            recordedAtEpochMillis = recordedAtEpochMillis,
            durationSeconds = durationSeconds,
            distanceMetres = distanceMetres,
            steps = steps
        )
    }
}

data class ActivityRecord(
    override val id: String,
    override val ownerId: String,
    override val recordedAtEpochMillis: Long,
    val activityType: ActivityType,
    val durationSeconds: Long,
    val distanceMetres: Long,
    val steps: Long?
) : Record {
    init {
        require(ownerId.isNotBlank()) { "Owner id must not be blank" }
        validateActivityFields(id, recordedAtEpochMillis, durationSeconds, distanceMetres, steps)
    }

    val paceSecondsPerKilometre: Double?
        get() = if (distanceMetres == 0L) null else durationSeconds * 1_000.0 / distanceMetres
}

enum class Feeling {
    GOOD,
    OKAY,
    UNWELL
}

enum class SymptomLevel {
    NONE,
    MILD,
    SEVERE
}

enum class HealthCondition {
    GOOD,
    CAUTION,
    UNKNOWN
}

data class HealthCheckInDraft(
    override val id: String,
    override val recordedAtEpochMillis: Long,
    val feeling: Feeling,
    val breathingDifficulty: SymptomLevel,
    val fatigue: SymptomLevel,
    val condition: HealthCondition
) : RecordDraft {
    init {
        validateRecordIdentity(id, recordedAtEpochMillis)
    }
}

data class HealthCheckInRecord(
    override val id: String,
    override val ownerId: String,
    override val recordedAtEpochMillis: Long,
    val feeling: Feeling,
    val breathingDifficulty: SymptomLevel,
    val fatigue: SymptomLevel,
    val condition: HealthCondition
) : Record {
    init {
        require(ownerId.isNotBlank()) { "Owner id must not be blank" }
        validateRecordIdentity(id, recordedAtEpochMillis)
    }
}

private fun validateActivityFields(
    id: String,
    recordedAtEpochMillis: Long,
    durationSeconds: Long,
    distanceMetres: Long,
    steps: Long?
) {
    validateRecordIdentity(id, recordedAtEpochMillis)
    require(durationSeconds >= 0) { "Duration must not be negative" }
    require(distanceMetres >= 0) { "Distance must not be negative" }
    require(steps == null || steps >= 0) { "Steps must not be negative" }
}

private fun validateRecordIdentity(id: String, recordedAtEpochMillis: Long) {
    require(id.isNotBlank()) { "Record id must not be blank" }
    require(recordedAtEpochMillis >= 0) { "Recorded time must not be negative" }
}
