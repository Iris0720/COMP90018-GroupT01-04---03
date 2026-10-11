package com.example.comp90018.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Upsert
    suspend fun upsert(record: ActivityRecordEntity)

    @Upsert
    suspend fun upsertAll(records: List<ActivityRecordEntity>)

    @Query("DELETE FROM activity_records WHERE ownerId = :ownerId")
    suspend fun deleteAllForOwner(ownerId: String)

    @Query("SELECT * FROM activity_records WHERE ownerId = :ownerId")
    suspend fun readAllForOwner(ownerId: String): List<ActivityRecordEntity>

    @Transaction
    suspend fun replaceForOwner(ownerId: String, records: List<ActivityRecordEntity>) {
        deleteAllForOwner(ownerId)
        if (records.isNotEmpty()) upsertAll(records)
    }

    @Query("SELECT * FROM activity_records WHERE id = :recordId AND ownerId = :ownerId LIMIT 1")
    suspend fun findById(ownerId: String, recordId: String): ActivityRecordEntity?

    @Query("DELETE FROM activity_records WHERE id = :recordId AND ownerId = :ownerId")
    suspend fun deleteById(ownerId: String, recordId: String): Int

    @Query(
        """
        SELECT * FROM activity_records
        WHERE ownerId = :ownerId
          AND recordedAtEpochMillis >= :startEpochMillis
          AND recordedAtEpochMillis < :endEpochMillis
        ORDER BY recordedAtEpochMillis DESC
        """
    )
    fun observeBetween(
        ownerId: String,
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Flow<List<ActivityRecordEntity>>

    @Query(
        """
        SELECT * FROM activity_records
        WHERE ownerId = :ownerId
          AND recordedAtEpochMillis >= :startEpochMillis
          AND recordedAtEpochMillis < :endEpochMillis
        ORDER BY recordedAtEpochMillis DESC
        """
    )
    suspend fun readBetween(
        ownerId: String,
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<ActivityRecordEntity>
}
