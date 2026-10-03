package com.example.comp90018.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {
    @Upsert
    suspend fun upsert(record: ActivityRecordEntity)

    @Query("SELECT * FROM activity_records WHERE id = :recordId AND ownerId = :ownerId LIMIT 1")
    suspend fun findById(ownerId: String, recordId: String): ActivityRecordEntity?

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
