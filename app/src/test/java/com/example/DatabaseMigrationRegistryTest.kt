package com.example

import com.example.data.local.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test

/** Raising the database version without a migration would wipe agents' unsynced records on update. */
class DatabaseMigrationRegistryTest {

    @Test
    fun `every database version from 9 has a migration to the next`() {
        for (from in 9 until AppDatabase.VERSION) {
            val covered = AppDatabase.MIGRATIONS.any { it.startVersion == from && it.endVersion == from + 1 }
            assertTrue("Missing migration $from -> ${from + 1}. Add it to AppDatabase.MIGRATIONS.", covered)
        }
    }
}
