package com.example.comp90018.data.repository

import com.example.comp90018.data.local.dao.ActivityRecordDao
import com.example.comp90018.data.local.dao.HealthCheckInDao
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import com.example.comp90018.data.local.entity.HealthCheckInRecordEntity
import com.example.comp90018.domain.model.ActivityRecordDraft
import com.example.comp90018.domain.model.ActivityType
import com.example.comp90018.domain.model.Feeling
import com.example.comp90018.domain.model.HealthCheckInDraft
import com.example.comp90018.domain.model.HealthCheckInRecord
import com.example.comp90018.domain.model.HealthCondition
import com.example.comp90018.domain.model.HistoryQuery
import com.example.comp90018.domain.model.PeriodComparison
import com.example.comp90018.domain.model.ReportPeriod
import com.example.comp90018.domain.model.SymptomLevel
import com.example.comp90018.domain.repository.ActiveOwnerProvider
import com.example.comp90018.domain.repository.HistoryState
import com.example.comp90018.domain.repository.StorageErrorCode
import com.example.comp90018.domain.repository.StorageResult
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomRecordRepositoryTest {
    private val activityDao = FakeActivityDao()
    private val healthDao = FakeHealthDao()
    private val ownerProvider = FakeOwnerProvider("owner-a")
    private val repository = RoomRecordRepository(activityDao, healthDao, ownerProvider)

    @Test
    fun save_injectsActiveOwnerAndKeepsSameIdSeparateAcrossAccounts() = runTest {
        val draft = activityDraft("shared-id", 1_000)

        repository.saveRecord(draft)
        ownerProvider.owner.value = "owner-b"
        repository.saveRecord(draft.copy(distanceMetres = 2_000))

        assertEquals(1_000, activityDao.findById("owner-a", "shared-id")!!.distanceMetres)
        assertEquals(2_000, activityDao.findById("owner-b", "shared-id")!!.distanceMetres)
    }

    @Test
    fun retryingSameSave_updatesInsteadOfDuplicating() = runTest {
        val draft = activityDraft("activity-1", 1_000)

        repository.saveRecord(draft)
        repository.saveRecord(draft.copy(distanceMetres = 3_000))

        assertEquals(1, activityDao.records.size)
        assertEquals(3_000, activityDao.records.single().distanceMetres)
    }

    @Test
    fun saveHealthCheckIn_canBeReadBackAsSameType() = runTest {
        val draft = HealthCheckInDraft(
            id = "health-1",
            recordedAtEpochMillis = 2_000,
            feeling = Feeling.OKAY,
            breathingDifficulty = SymptomLevel.MILD,
            fatigue = SymptomLevel.NONE,
            condition = HealthCondition.CAUTION
        )

        val saved = repository.saveRecord(draft) as StorageResult.Success
        val read = repository.readRecord("health-1") as StorageResult.Success

        assertTrue(saved.value is HealthCheckInRecord)
        assertEquals(saved.value, read.value)
    }

    @Test
    fun recordOwnedByAnotherAccount_isNotFound() = runTest {
        repository.saveRecord(activityDraft("private-record", 1_000))
        ownerProvider.owner.value = "owner-b"

        val result = repository.readRecord("private-record")

        assertEquals(
            StorageErrorCode.NOT_FOUND,
            (result as StorageResult.Failure).error.code
        )
    }

    @Test
    fun readWhileSignedOut_returnsSignedOut() = runTest {
        ownerProvider.owner.value = null

        val result = repository.readRecord("activity-1")

        assertEquals(
            StorageErrorCode.SIGNED_OUT,
            (result as StorageResult.Failure).error.code
        )
    }

    @Test
    fun reportWithNoPreviousDistance_returnsNoComparison() = runTest {
        repository.saveRecord(activityDraft("activity-1", 2_500))

        val result = repository.getReport(
            HistoryQuery(ReportPeriod.ALL, 10_000, "UTC")
        ) as StorageResult.Success

        assertEquals(2_500, result.value.totalDistanceMetres)
        assertTrue(result.value.distanceComparison is PeriodComparison.NoComparison)
    }

    @Test
    fun reportCombinesActivityHealthAndPreviousPeriodComparison() = runTest {
        val current = utcMillis(2024, Calendar.JANUARY, 15)
        val previous = utcMillis(2024, Calendar.JANUARY, 8)
        repository.saveRecord(activityDraft("previous", 1_000).copy(recordedAtEpochMillis = previous))
        repository.saveRecord(activityDraft("current", 2_000).copy(recordedAtEpochMillis = current))
        repository.saveRecord(
            HealthCheckInDraft(
                id = "health-current",
                recordedAtEpochMillis = current,
                feeling = Feeling.GOOD,
                breathingDifficulty = SymptomLevel.NONE,
                fatigue = SymptomLevel.NONE,
                condition = HealthCondition.GOOD
            )
        )

        val result = repository.getReport(
            HistoryQuery(ReportPeriod.WEEK, current, "UTC")
        ) as StorageResult.Success

        assertEquals(1, result.value.activityCount)
        assertEquals(1, result.value.healthCheckInCount)
        assertEquals(2, result.value.records.size)
        assertEquals(
            100.0,
            (result.value.distanceComparison as PeriodComparison.PercentageChange).percentage,
            0.001
        )
    }

    @Test
    fun historyEmitsLoadingSuccessThenSignedOutWithoutLeakingRecords() = runTest {
        repository.saveRecord(activityDraft("activity-1", 1_000))
        val states = mutableListOf<HistoryState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(
                HistoryQuery(ReportPeriod.ALL, 10_000, "UTC")
            ).take(3).toList(states)
        }

        ownerProvider.owner.value = null

        assertTrue(states[0] is HistoryState.Loading)
        assertTrue(states[1] is HistoryState.Success)
        assertEquals(1, (states[1] as HistoryState.Success).records.size)
        assertEquals(
            StorageErrorCode.SIGNED_OUT,
            (states[2] as HistoryState.Failure).error.code
        )
    }

    @Test
    fun emptyHistory_hasExplicitEmptyState() = runTest {
        val states = repository.observeHistory(
            HistoryQuery(ReportPeriod.ALL, 10_000, "UTC")
        ).take(2).toList()

        assertTrue(states[0] is HistoryState.Loading)
        assertTrue(states[1] is HistoryState.Empty)
    }

    private fun activityDraft(id: String, distanceMetres: Long) = ActivityRecordDraft(
        id = id,
        recordedAtEpochMillis = 1_000,
        activityType = ActivityType.WALKING,
        durationSeconds = 600,
        distanceMetres = distanceMetres,
        steps = 1_000
    )

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}

private class FakeOwnerProvider(initialOwnerId: String?) : ActiveOwnerProvider {
    val owner = MutableStateFlow(initialOwnerId)

    override fun observeActiveOwnerId(): Flow<String?> = owner

    override suspend fun currentOwnerId(): String? = owner.value
}

private class FakeActivityDao : ActivityRecordDao {
    val records = mutableListOf<ActivityRecordEntity>()

    override suspend fun upsert(record: ActivityRecordEntity) {
        records.removeAll { it.ownerId == record.ownerId && it.id == record.id }
        records += record
    }

    override suspend fun findById(ownerId: String, recordId: String) =
        records.find { it.ownerId == ownerId && it.id == recordId }

    override fun observeBetween(ownerId: String, startEpochMillis: Long, endEpochMillis: Long) =
        flowOf(read(ownerId, startEpochMillis, endEpochMillis))

    override suspend fun readBetween(ownerId: String, startEpochMillis: Long, endEpochMillis: Long) =
        read(ownerId, startEpochMillis, endEpochMillis)

    private fun read(ownerId: String, start: Long, end: Long) = records.filter {
        it.ownerId == ownerId && it.recordedAtEpochMillis >= start && it.recordedAtEpochMillis < end
    }.sortedByDescending { it.recordedAtEpochMillis }
}

private class FakeHealthDao : HealthCheckInDao {
    private val records = mutableListOf<HealthCheckInRecordEntity>()

    override suspend fun upsert(record: HealthCheckInRecordEntity) {
        records.removeAll { it.ownerId == record.ownerId && it.id == record.id }
        records += record
    }

    override suspend fun findById(ownerId: String, recordId: String) =
        records.find { it.ownerId == ownerId && it.id == recordId }

    override fun observeBetween(ownerId: String, startEpochMillis: Long, endEpochMillis: Long) =
        flowOf(read(ownerId, startEpochMillis, endEpochMillis))

    override suspend fun readBetween(ownerId: String, startEpochMillis: Long, endEpochMillis: Long) =
        read(ownerId, startEpochMillis, endEpochMillis)

    private fun read(ownerId: String, start: Long, end: Long) = records.filter {
        it.ownerId == ownerId && it.recordedAtEpochMillis >= start && it.recordedAtEpochMillis < end
    }.sortedByDescending { it.recordedAtEpochMillis }
}
