package com.example.comp90018.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecordDaoTest {
    private lateinit var database: TrailwiseDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TrailwiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun sameRecordId_isIsolatedByOwner() = runBlocking {
        val dao = database.activityRecordDao()
        dao.upsert(entity(ownerId = "owner-a", distanceMetres = 1_000))
        dao.upsert(entity(ownerId = "owner-b", distanceMetres = 2_000))

        assertEquals(1_000, dao.findById("owner-a", "shared-id")!!.distanceMetres)
        assertEquals(2_000, dao.findById("owner-b", "shared-id")!!.distanceMetres)
    }

    @Test
    fun upsertSameOwnerAndId_updatesExistingRecord() = runBlocking {
        val dao = database.activityRecordDao()
        dao.upsert(entity(ownerId = "owner-a", distanceMetres = 1_000))
        dao.upsert(entity(ownerId = "owner-a", distanceMetres = 3_000))

        assertEquals(3_000, dao.findById("owner-a", "shared-id")!!.distanceMetres)
    }

    @Test
    fun recordRemainsAvailableAfterDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "record-persistence-test.db"
        context.deleteDatabase(databaseName)

        Room.databaseBuilder(context, TrailwiseDatabase::class.java, databaseName)
            .build()
            .also { first ->
                first.activityRecordDao().upsert(entity("owner-a", 4_000))
                first.close()
            }

        Room.databaseBuilder(context, TrailwiseDatabase::class.java, databaseName)
            .build()
            .also { reopened ->
                assertEquals(
                    4_000,
                    reopened.activityRecordDao().findById("owner-a", "shared-id")!!.distanceMetres
                )
                reopened.close()
            }

        context.deleteDatabase(databaseName)
    }

    private fun entity(ownerId: String, distanceMetres: Long) = ActivityRecordEntity(
        id = "shared-id",
        ownerId = ownerId,
        recordedAtEpochMillis = 1_000,
        activityType = "WALKING",
        durationSeconds = 600,
        distanceMetres = distanceMetres,
        steps = 1_000
    )
}
