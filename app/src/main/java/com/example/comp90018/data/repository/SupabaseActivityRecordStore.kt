package com.example.comp90018.data.repository

import android.content.Context
import com.example.comp90018.data.SupabaseProvider
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Stores activity records in Supabase so the same account can use multiple devices.
class SupabaseActivityRecordStore(
    context: Context,
    private val client: SupabaseClient = SupabaseProvider.client
) {
    private val migrationPreferences = context.applicationContext.getSharedPreferences(
        MIGRATION_PREFERENCES,
        Context.MODE_PRIVATE
    )

    suspend fun save(record: ActivityRecordEntity) {
        client.from(TABLE_NAME).upsert(record.toCloudRow()) {
            onConflict = "id"
        }
    }

    suspend fun readAll(ownerId: String): List<ActivityRecordEntity> {
        return client.from(TABLE_NAME).select {
            filter { eq("owner_id", ownerId) }
            order("started_at", Order.DESCENDING)
        }.decodeList<ActivityRecordCloudRow>().map(ActivityRecordCloudRow::toEntity)
    }

    suspend fun synchronize(
        ownerId: String,
        localRecords: List<ActivityRecordEntity>
    ): List<ActivityRecordEntity> {
        val migrationKey = "$MIGRATION_KEY_PREFIX$ownerId"
        if (!migrationPreferences.getBoolean(migrationKey, false)) {
            localRecords.forEach { save(it) }
            migrationPreferences.edit().putBoolean(migrationKey, true).apply()
        }
        return readAll(ownerId)
    }

    suspend fun delete(ownerId: String, recordId: String) {
        client.from(TABLE_NAME).delete {
            filter {
                eq("owner_id", ownerId)
                eq("id", recordId)
            }
        }
    }

    private companion object {
        const val TABLE_NAME = "activity_sessions"
        const val MIGRATION_PREFERENCES = "activity_cloud_migration"
        const val MIGRATION_KEY_PREFIX = "room_imported_"
    }
}

@Serializable
private data class ActivityRecordCloudRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("activity_type") val activityType: String,
    @SerialName("session_state") val sessionState: String = "complete",
    @SerialName("duration_seconds") val durationSeconds: Long,
    @SerialName("distance_metres") val distanceMetres: Long,
    val steps: Long? = null,
    @SerialName("finished_at") val finishedAt: String? = null
) {
    fun toEntity() = ActivityRecordEntity(
        id = id,
        ownerId = ownerId,
        recordedAtEpochMillis = Instant.parse(finishedAt ?: startedAt).toEpochMilli(),
        activityType = activityType.uppercase(),
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        steps = steps
    )
}

private fun ActivityRecordEntity.toCloudRow() = ActivityRecordCloudRow(
    id = id,
    ownerId = ownerId,
    startedAt = Instant.ofEpochMilli(
        recordedAtEpochMillis - durationSeconds * 1_000L
    ).toString(),
    activityType = activityType.lowercase(),
    sessionState = "complete",
    durationSeconds = durationSeconds,
    distanceMetres = distanceMetres,
    steps = steps,
    finishedAt = Instant.ofEpochMilli(recordedAtEpochMillis).toString()
)
