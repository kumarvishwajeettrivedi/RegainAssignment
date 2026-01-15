package com.example.regainassignment.data.repository

import com.example.regainassignment.data.local.AppDao
import com.example.regainassignment.data.local.AppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val appDao: AppDao
) : UsageRepository {

    override fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    override fun getTrackedApps(): Flow<List<AppEntity>> = appDao.getTrackedApps()

    override suspend fun getApp(packageName: String): AppEntity? = appDao.getApp(packageName)

    override suspend fun toggleLimit(packageName: String, enabled: Boolean) {
        // When enabling limit, reset session time so it blocks immediately if enabled
        if (enabled) {
             appDao.updateLimitStatus(packageName, true)
        } else {
             appDao.updateLimitStatus(packageName, false)
        }
    }

    override suspend fun setSessionExpiry(packageName: String, expiryTime: Long) {
        appDao.updateSessionExpiry(packageName, expiryTime)
    }

    override suspend fun resetDailyUsage() {
        appDao.resetDailyUsage()
        appDao.clearAllTemporaryBlocks() // Also clear temporary blocks at midnight
    }

    override suspend fun updateUsage(packageName: String, timeSpent: Long) {
        val app = appDao.getApp(packageName)
        if (app != null) {
            val validTimeSpent = if (timeSpent > 0) timeSpent else 0
            val newTotal = app.totalUsageToday + validTimeSpent
            appDao.updateUsage(packageName, newTotal, System.currentTimeMillis())
        }
    }
    
    override suspend fun setUsage(packageName: String, totalUsage: Long) {
        // Direct override validation
        if (totalUsage >= 0) {
            appDao.updateUsage(packageName, totalUsage, System.currentTimeMillis())
        }
    }

    override suspend fun insertApp(app: AppEntity) {
        // Only insert if not exists to avoid overwriting usage
        if (appDao.getApp(app.packageName) == null) {
            appDao.insertApp(app)
        }
    }

    override suspend fun refreshApps(context: android.content.Context) {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        // queries all activities that are launchers
        val activities = pm.queryIntentActivities(intent, 0)
        
        activities.forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            // Skip own app
            if (packageName == context.packageName) return@forEach
            
            if (appDao.getApp(packageName) == null) {
                val label = resolveInfo.loadLabel(pm).toString()
                appDao.insertApp(
                    AppEntity(
                        packageName = packageName,
                        appName = label
                    )
                )
            }
        }
    }
    
    // New method implementations
    override suspend fun setTemporaryBlock(packageName: String, isBlocked: Boolean) {
        appDao.updateTemporaryBlock(packageName, isBlocked)
    }
    
    override suspend fun setSessionInfo(packageName: String, startTime: Long, duration: Long, expiryTime: Long) {
        appDao.updateSessionInfo(packageName, startTime, duration, expiryTime)
    }
    
    override suspend fun updateRemainingTime(packageName: String, remainingTime: Long) {
        appDao.updateRemainingTime(packageName, remainingTime)
    }
    
    override suspend fun clearAllTemporaryBlocks() {
        appDao.clearAllTemporaryBlocks()
    }
    
    override suspend fun getUsageStatsForToday(context: android.content.Context, packageName: String): Long {
        val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val calendar = java.util.Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        // Use queryAndAggregate to automatically merge partial buckets
        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )
        
        val appUsage = usageStatsMap[packageName]
        
        // Return system usage if available, otherwise 0
        return appUsage?.totalTimeInForeground ?: 0L
    }
}
