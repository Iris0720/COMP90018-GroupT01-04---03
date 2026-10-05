package com.example.comp90018.data.repository

import com.example.comp90018.data.local.dao.ActivityRecordDao
import com.example.comp90018.data.local.dao.HealthCheckInDao
import com.example.comp90018.domain.model.ActivityRecordDraft
import com.example.comp90018.domain.model.ActivityReport
import com.example.comp90018.domain.model.HealthCheckInDraft
import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.PeriodComparison
import com.example.comp90018.domain.model.Record
import com.example.comp90018.domain.model.RecordDraft
import com.example.comp90018.domain.repository.ActiveOwnerProvider
import com.example.comp90018.domain.repository.HistoryState
import com.example.comp90018.domain.repository.RecordRepository
import com.example.comp90018.domain.repository.StorageError
import com.example.comp90018.domain.repository.StorageErrorCode
import com.example.comp90018.domain.repository.StorageResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class RoomRecordRepository(
    private val activityDao: ActivityRecordDao,
    private val healthDao: HealthCheckInDao,
    private val ownerProvider: ActiveOwnerProvider
) : RecordRepository {
    override suspend fun saveRecord(draft: RecordDraft): StorageResult<Record> = withOwner { ownerId ->
        when (draft) {
            is ActivityRecordDraft -> {
                val entity = draft.toEntity(ownerId)
                activityDao.upsert(entity)
                entity.toDomain()
            }
            is HealthCheckInDraft -> {
                val entity = draft.toEntity(ownerId)
                healthDao.upsert(entity)
                entity.toDomain()
            }
        }
    }

    override suspend fun readRecord(recordId: String): StorageResult<Record> {
        if (recordId.isBlank()) return invalidRecord("Record id must not be blank")

        return withOwner { ownerId ->
            activityDao.findById(ownerId, recordId)?.toDomain()
                ?: healthDao.findById(ownerId, recordId)?.toDomain()
                ?: throw RecordNotFoundException()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHistory(query: HistoryQuery): Flow<HistoryState> = flow {
        emit(HistoryState.Loading)
        val window = runCatching { HistoryWindowCalculator.calculate(query) }
            .getOrElse {
                emit(HistoryState.Failure(invalidRecordError(it.message)))
                return@flow
            }

        emitAll(ownerProvider.observeActiveOwnerId().flatMapLatest { ownerId ->
            if (ownerId.isNullOrBlank()) {
                flowOf(HistoryState.Failure(signedOutError()))
            } else {
                combine(
                    activityDao.observeBetween(ownerId, window.startEpochMillis, window.endEpochMillis),
                    healthDao.observeBetween(ownerId, window.startEpochMillis, window.endEpochMillis)
                ) { activities, healthCheckIns ->
                    val records = (activities.map { it.toDomain() } + healthCheckIns.map { it.toDomain() })
                        .sortedByDescending { it.recordedAtEpochMillis }
                    if (records.isEmpty()) HistoryState.Empty else HistoryState.Success(records)
                }
            }
        })
    }.catch { error ->
        emit(
            HistoryState.Failure(
                StorageError(
                    StorageErrorCode.DATABASE_UNAVAILABLE,
                    retryable = true,
                    message = error.message
                )
            )
        )
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

    private fun signedOut() = StorageResult.Failure(signedOutError())

    private fun signedOutError() = StorageError(StorageErrorCode.SIGNED_OUT, retryable = false)

    private fun invalidRecord(message: String?) = StorageResult.Failure(invalidRecordError(message))

    private fun invalidRecordError(message: String?) =
        StorageError(StorageErrorCode.INVALID_RECORD, retryable = false, message)

    private class RecordNotFoundException : Exception()
}
