package com.example.comp90018.domain.repository

import com.example.comp90018.domain.model.ActivityRecord
import com.example.comp90018.domain.model.ActivityRecordDraft
import com.example.comp90018.domain.model.ActivityReport
import com.example.comp90018.domain.model.HealthCheckInDraft
import com.example.comp90018.domain.model.HealthCheckInRecord
import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.Record
import kotlinx.coroutines.flow.Flow

interface ActiveOwnerProvider {
    /** Emits null when signed out so account-scoped streams can clear immediately. */
    fun observeActiveOwnerId(): Flow<String?>

    /** Returns the current account id for one-shot reads and writes. */
    suspend fun currentOwnerId(): String?
}

interface RecordRepository {
    suspend fun saveActivity(draft: ActivityRecordDraft): StorageResult<ActivityRecord>

    suspend fun saveHealthCheckIn(draft: HealthCheckInDraft): StorageResult<HealthCheckInRecord>

    suspend fun readRecord(recordId: String): StorageResult<Record>

    fun observeHistory(query: HistoryQuery): Flow<StorageResult<List<Record>>>

    suspend fun getReport(query: HistoryQuery): StorageResult<ActivityReport>
}

sealed interface StorageResult<out T> {
    data class Success<T>(val value: T) : StorageResult<T>

    data class Failure(val error: StorageError) : StorageResult<Nothing>
}

data class StorageError(
    val code: StorageErrorCode,
    val retryable: Boolean,
    val message: String? = null
)

enum class StorageErrorCode {
    SIGNED_OUT,
    INVALID_RECORD,
    NOT_FOUND,
    DATABASE_UNAVAILABLE,
    UNKNOWN
}
