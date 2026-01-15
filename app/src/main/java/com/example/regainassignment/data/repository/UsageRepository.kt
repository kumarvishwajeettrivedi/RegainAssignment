package com.example.regainassignment.data.repository

import com.example.regainassignment.data.local.AppEntity
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun getAllApps(): Flow<List<AppEntity>>
    fun getTrackedApps(): Flow<List<AppEntity>>
    suspend fun getApp(packageName: String): AppEntity?
    suspend fun toggleLimit(packageName: String, enabled: Boolean)
    suspend fun setSessionExpiry(packageName: String, expiryTime: Long)
    suspend fun resetDailyUsage()
    suspend fun updateUsage(packageName: String, timeSpent: Long)
    suspend fun setUsage(packageName: String, totalUsage: Long)
    suspend fun insertApp(app: AppEntity)
    suspend fun refreshApps(context: android.content.Context)
    
    // New methods for enhanced features
    suspend fun setTemporaryBlock(packageName: String, isBlocked: Boolean)
    suspend fun setSessionInfo(packageName: String, startTime: Long, duration: Long, expiryTime: Long)
    suspend fun updateRemainingTime(packageName: String, remainingTime: Long)
    suspend fun clearAllTemporaryBlocks()
    suspend fun getUsageStatsForToday(context: android.content.Context, packageName: String): Long
}
