package com.example.comp90018.data.repository

import com.example.comp90018.data.local.dao.ActivityRecordDao
import com.example.comp90018.data.local.dao.HealthCheckInDao
import com.example.comp90018.domain.model.ActivityRecord
import com.example.comp90018.domain.model.ActivityRecordDraft
import com.example.comp90018.domain.model.ActivityReport
import com.example.comp90018.domain.model.HealthCheckInDraft
import com.example.comp90018.domain.model.HealthCheckInRecord
import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.PeriodComparison
import com.example.comp90018.domain.model.Record
import com.example.comp90018.domain.repository.ActiveOwnerProvider
import com.example.comp90018.domain.repository.RecordRepository
import com.example.comp90018.domain.repository.StorageError
import com.example.comp90018.domain.repository.StorageErrorCode
import com.example.comp90018.domain.repository.StorageResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomRecordRepository(
    private val activityDao: ActivityRecordDao,
    private val healthDao: HealthCheckInDao,
    private val ownerProvider: ActiveOwnerProvider
) : RecordRepository {
    override suspend fun saveActivity(draft: ActivityRecordDraft): StorageResult<ActivityRecord> =
        withOwner { ownerId ->
            val entity = draft.toEntity(ownerId)
            activityDao.upsert(entity)
            entity.toDomain()
        }

    override suspend fun saveHealthCheckIn(
        draft: HealthCheckInDraft
    ): StorageResult<HealthCheckInRecord> = withOwner { ownerId ->
        val entity = draft.toEntity(ownerId)
        healthDao.upsert(entity)
        entity.toDomain()
    }

    override suspend fun readRecord(recordId: String): StorageResult<Record> {
        if (recordId.isBlank()) return invalidRecord("Record id must not be blank")

        return withOwner { ownerId ->
            activityDao.findById(ownerId, recordId)?.toDomain()
                ?: healthDao.findById(ownerId, recordId)?.toDomain()
                ?: throw RecordNotFoundException()
        }
    }

    override fun observeHistory(query: HistoryQuery): Flow<StorageResult<List<Record>>> {
        val window = runCatching { HistoryWindowCalculator.calculate(query) }
            .getOrElse { return flowOf(invalidRecord(it.message)) }

        return ownerProvider.observeActiveOwnerId().flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(signedOut())
            } else {
                combine(
                    activityDao.observeBetween(ownerId, window.startEpochMillis, window.endEpochMillis),
                    healthDao.observeBetween(ownerId, window.startEpochMillis, window.endEpochMillis)
                ) { activities, healthCheckIns ->
                    val records = (activities.map { it.toDomain() } + healthCheckIns.map { it.toDomain() })
                        .sortedByDescending { it.recordedAtEpochMillis }
                    StorageResult.Success(records)
                }
            }
        }.catch { error ->
            emit(
                StorageResult.Failure(
                    StorageError(
                        StorageErrorCode.DATABASE_UNAVAILABLE,
                        retryable = true,
                        message = error.message
                    )
                )
            )
        }
    }

    override suspend fun getReport(query: HistoryQuery): StorageResult<ActivityReport> {
        val window = runCatching { HistoryWindowCalculator.calculate(query) }
            .getOrElse { return invalidRecord(it.message) }

        return withOwner { ownerId ->
            val activities = activityDao.readBetween(ownerId, window.startEpochMillis, window.endEpochMillis)
                .map { it.toDomain() }
            val healthCheckIns = healthDao.readBetween(ownerId, window.startEpochMillis, window.endEpochMillis)
                .map { it.toDomain() }
            val previousDistance = if (window.previousStartEpochMillis == window.previousEndEpochMillis) {
                0L
            } else {
                activityDao.readBetween(
                    ownerId,
                    window.previousStartEpochMillis,
                    window.previousEndEpochMillis
                ).sumOf { it.distanceMetres }
            }
            val currentDistance = activities.sumOf { it.distanceMetres }
            val comparison = if (previousDistance == 0L) {
                PeriodComparison.NoComparison
            } else {
                PeriodComparison.PercentageChange(
                    (currentDistance - previousDistance) * 100.0 / previousDistance
                )
            }
            val records = (activities + healthCheckIns).sortedByDescending { it.recordedAtEpochMillis }

            ActivityReport(
                query = query,
                activityCount = activities.size,
                totalDurationSeconds = activities.sumOf { it.durationSeconds },
                totalDistanceMetres = currentDistance,
                healthCheckInCount = healthCheckIns.size,
                distanceComparison = comparison,
                records = records
            )
        }
    }

    private suspend fun <T> withOwner(block: suspend (String) -> T): StorageResult<T> {
        val ownerId = ownerProvider.currentOwnerId() ?: return signedOut()
        if (ownerId.isBlank()) return signedOut()

        return try {
            StorageResult.Success(block(ownerId))
        } catch (_: RecordNotFoundException) {
            StorageResult.Failure(StorageError(StorageErrorCode.NOT_FOUND, retryable = false))
        } catch (error: IllegalArgumentException) {
            invalidRecord(error.message)
        } catch (error: Exception) {
            StorageResult.Failure(
                StorageError(StorageErrorCode.DATABASE_UNAVAILABLE, retryable = true, error.message)
            )
        }
    }

    private fun signedOut() = StorageResult.Failure(
        StorageError(StorageErrorCode.SIGNED_OUT, retryable = false)
    )

    private fun invalidRecord(message: String?) = StorageResult.Failure(
        StorageError(StorageErrorCode.INVALID_RECORD, retryable = false, message)
    )

    private class RecordNotFoundException : Exception()
}
