package com.example.comp90018.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.comp90018.data.local.dao.ActivityRecordDao
import com.example.comp90018.data.local.dao.HealthCheckInDao
import com.example.comp90018.data.local.entity.ActivityRecordEntity
import com.example.comp90018.data.local.entity.HealthCheckInRecordEntity

@Database(
    entities = [ActivityRecordEntity::class, HealthCheckInRecordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TrailwiseDatabase : RoomDatabase() {
    abstract fun activityRecordDao(): ActivityRecordDao

    abstract fun healthCheckInDao(): HealthCheckInDao

    companion object {
        @Volatile
        private var instance: TrailwiseDatabase? = null

        fun getInstance(context: Context): TrailwiseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrailwiseDatabase::class.java,
                    "trailwise.db"
                ).build().also { instance = it }
            }
    }
}
