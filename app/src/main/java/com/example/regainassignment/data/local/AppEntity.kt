package com.example.regainassignment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val totalUsageToday: Long = 0L, // In milliseconds
    val isLimitEnabled: Boolean = false,
    val sessionExpiryTime: Long = 0L, // Timestamp when current session ends
    val lastInteractTime: Long = 0L, // To track when usage was last updated
    val isTemporarilyBlocked: Boolean = false, // Temporary block state (after "Close App")
    val sessionStartTime: Long = 0L, // When current session started
    val selectedSessionDuration: Long = 0L, // User's selected time in milliseconds
    val remainingSessionTime: Long = 0L // For screen lock pause - time remaining when paused
)
