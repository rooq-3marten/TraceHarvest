package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = AppDatabase.VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun traceHarvestDao(): TraceHarvestDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        /**
         * The current database version. If you change a table, raise this number AND add a Migration
         * to [MIGRATIONS] that moves the data from the old version to the new one.
         * Records the agent has not synced yet live in this database, so an update must never wipe it.
         */
        const val VERSION = 9

        /**
         * One entry per version step from 9 onward, for example:
         *   val MIGRATION_9_10 = object : Migration(9, 10) {
         *       override fun migrate(db: SupportSQLiteDatabase) {
         *           db.execSQL("ALTER TABLE farmers ADD COLUMN example TEXT NOT NULL DEFAULT ''")
         *       }
         *   }
         * A test fails if VERSION goes up without a matching entry here.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        /** Builds before version 9 were development builds. They are the only ones allowed to be reset. */
        private val RESETTABLE_OLD_VERSIONS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trace_harvest_db"
                )
                .addMigrations(*MIGRATIONS)
                .fallbackToDestructiveMigrationFrom(true, *RESETTABLE_OLD_VERSIONS)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
