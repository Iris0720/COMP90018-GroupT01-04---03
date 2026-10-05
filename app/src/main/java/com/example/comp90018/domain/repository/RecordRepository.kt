package com.example.comp90018.domain.repository

import com.example.comp90018.domain.model.ActivityReport
import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.Record
import com.example.comp90018.domain.model.RecordDraft
import kotlinx.coroutines.flow.Flow

interface ActiveOwnerProvider {
    /** Emits null when signed out so account-scoped streams can clear immediately. */
    fun observeActiveOwnerId(): Flow<String?>

    /** Returns the current account id for one-shot reads and writes. */
    suspend fun currentOwnerId(): String?
}

interface RecordRepository {
    suspend fun saveRecord(draft: RecordDraft): StorageResult<Record>

    suspend fun readRecord(recordId: String): StorageResult<Record>

    fun observeHistory(query: HistoryQuery): Flow<HistoryState>

    suspend fun getReport(query: HistoryQuery): StorageResult<ActivityReport>
}

sealed interface HistoryState {
    data object Loading : HistoryState

    data object Empty : HistoryState

    data class Success(val records: List<Record>) : HistoryState

    data class Failure(val error: StorageError) : HistoryState
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
