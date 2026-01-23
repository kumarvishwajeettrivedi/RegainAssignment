package com.example.regainassignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.regainassignment.data.local.AppEntity

@Database(entities = [AppEntity::class, TodoEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun todoDao(): TodoDao
    
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
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL,
                        scheduledTime INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        isInProgress INTEGER NOT NULL DEFAULT 0,
                        notificationEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
