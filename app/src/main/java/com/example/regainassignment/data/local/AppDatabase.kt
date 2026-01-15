package com.example.regainassignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.regainassignment.data.local.AppEntity

@Database(entities = [AppEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns with default values
                database.execSQL("ALTER TABLE app_usage ADD COLUMN isTemporarilyBlocked INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_usage ADD COLUMN sessionStartTime INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_usage ADD COLUMN selectedSessionDuration INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_usage ADD COLUMN remainingSessionTime INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
