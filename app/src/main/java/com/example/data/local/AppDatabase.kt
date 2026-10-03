package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.PendingSyncDao
import com.example.data.local.dao.TraceHarvestDao
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.HarvestBatchEntity
import com.example.data.local.entity.PendingSync
import com.example.data.local.entity.PracticeLogEntity
import com.example.data.local.entity.SmsLogEntity

@Database(
    entities = [
        FarmerEntity::class,
        PracticeLogEntity::class,
        HarvestBatchEntity::class,
        SmsLogEntity::class,
        PendingSync::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun traceHarvestDao(): TraceHarvestDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trace_harvest_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
