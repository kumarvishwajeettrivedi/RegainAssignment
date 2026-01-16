package com.example.regainassignment.data.repository

import com.example.regainassignment.data.local.AppEntity
import kotlinx.coroutines.flow.Flow

/**
 * In-memory session state for fast access during polling
 */
data class SessionState(
    val packageName: String,
    val sessionStart: Long,
    val totalSessionTime: Long, // Milliseconds used in this session
    val extensionsGranted: Long, // Additional time granted (5m, 10m, etc.)
    val lastChecked: Long
)

interface UsageRepository {
    fun getAllApps(): Flow<List<AppEntity>>
    fun getTrackedApps(): Flow<List<AppEntity>>
    suspend fun getApp(packageName: String): AppEntity?
    suspend fun toggleLimit(packageName: String, enabled: Boolean)
    suspend fun setSessionExpiry(packageName: String, expiryTime: Long)
    suspend fun resetDailyUsage()
    
    suspend fun syncUsageWithSystem(context: android.content.Context, packageName: String)
    
    suspend fun updateUsage(packageName: String, timeSpent: Long)
    suspend fun setUsage(packageName: String, totalUsage: Long)
    suspend fun insertApp(app: AppEntity)
    suspend fun refreshApps(context: android.content.Context)
    
    // Enhanced features
    suspend fun setTemporaryBlock(packageName: String, isBlocked: Boolean)
    suspend fun setSessionInfo(packageName: String, startTime: Long, duration: Long, expiryTime: Long)
    suspend fun updateRemainingTime(packageName: String, remainingTime: Long)
    suspend fun clearAllTemporaryBlocks()
    suspend fun getUsageStatsForToday(context: android.content.Context, packageName: String): Long
    
    // Session tracking methods for polling approach
    suspend fun getCurrentSessionState(packageName: String): SessionState
    suspend fun updateSessionTime(packageName: String)
    suspend fun grantExtension(packageName: String, minutes: Int)
    suspend fun endSession(packageName: String)
    suspend fun updateSessionState(packageName: String, state: Int)
    
    // Session persistence
    suspend fun pauseSession(packageName: String)
    suspend fun resumeSession(packageName: String)
    fun isSessionInMemory(packageName: String): Boolean
}
