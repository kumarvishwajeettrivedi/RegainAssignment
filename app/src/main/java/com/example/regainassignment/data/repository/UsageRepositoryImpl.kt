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
        
        // Sync system usage stats - Use queryEvents for precision matching Digital Wellbeing
        val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val now = System.currentTimeMillis()
        
        // Calculate usage for ALL apps using events
        val systemUsageMap = mutableMapOf<String, Long>()
        val lastEventTimeMap = mutableMapOf<String, Long>()
        val isVisibleMap = mutableMapOf<String, Boolean>()
        
        val usageEvents = usageStatsManager?.queryEvents(startTime, now)
        val event = android.app.usage.UsageEvents.Event()
        
        if (usageEvents != null) {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName
                
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEventTimeMap[pkg] = event.timeStamp
                    isVisibleMap[pkg] = true
                } else if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    val lastTime = lastEventTimeMap[pkg] ?: 0L
                    val visible = isVisibleMap[pkg] ?: false
                    
                    if (visible && lastTime > 0) {
                        val currentTotal = systemUsageMap[pkg] ?: 0L
                        systemUsageMap[pkg] = currentTotal + (event.timeStamp - lastTime)
                    }
                    isVisibleMap[pkg] = false
                }
            }
            
            // Handle apps still in foreground
            isVisibleMap.forEach { (pkg, visible) ->
                if (visible) {
                    val lastTime = lastEventTimeMap[pkg] ?: 0L
                    if (lastTime > 0) {
                        val currentTotal = systemUsageMap[pkg] ?: 0L
                        systemUsageMap[pkg] = currentTotal + (now - lastTime)
                    }
                }
            }
        }
        
        activities.forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            
            // Skip own app
            if (packageName == context.packageName) return@forEach
            
            val systemUsage = systemUsageMap[packageName] ?: 0L
            val existingApp = appDao.getApp(packageName)
            
            if (existingApp == null) {
                appDao.insertApp(
                    AppEntity(
                        packageName = packageName,
                        appName = label,
                        totalUsageToday = systemUsage // Initialize with precise system usage
                    )
                )
            } else {
                // CRITICAL FIX: Always overwrite with precise system usage
                appDao.updateUsage(packageName, systemUsage, System.currentTimeMillis())
            }
        }
    }
    
    // New method implementations
    override suspend fun setTemporaryBlock(packageName: String, isBlocked: Boolean) {
        appDao.updateTemporaryBlock(packageName, isBlocked)
    }
    
    override suspend fun setSessionInfo(packageName: String, startTime: Long, duration: Long, expiryTime: Long) {
        // Atomic session start: Set all fields including remaining time
        appDao.updateSessionInfo(packageName, startTime, duration, expiryTime)
        appDao.updateRemainingTime(packageName, duration) // Explicit initialization
        appDao.updateSessionState(packageName, AppEntity.STATE_ACTIVE)
        
        // Initialize in-memory session with 0 usage
        val newSession = SessionState(
            packageName = packageName,
            sessionStart = startTime,
            totalSessionTime = 0L,
            extensionsGranted = 0L,
            lastChecked = System.currentTimeMillis()
        )
        activeSessions[packageName] = newSession
        
        android.util.Log.d("UnScroll", "Started session for $packageName: duration=${duration}ms, remaining=${duration}ms, state=ACTIVE")
    }
    
    override suspend fun updateRemainingTime(packageName: String, remainingTime: Long) {
        appDao.updateRemainingTime(packageName, remainingTime)
    }
    
    override suspend fun clearAllTemporaryBlocks() {
        appDao.clearAllTemporaryBlocks()
    }
    
    override suspend fun getUsageStatsForToday(context: android.content.Context, packageName: String): Long {
        return calculatePreciseUsage(context, packageName)
    }

    /**
     * Calculates precise usage using UsageEvents (Event Stream) instead of buckets.
     * This ensures exact midnight-to-now calculation matching Digital Wellbeing.
     */
    private fun calculatePreciseUsage(context: android.content.Context, packageName: String): Long {
        val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        
        // Get exact midnight
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val startOfToday = calendar.timeInMillis
        val now = System.currentTimeMillis()
        
        // Log query range
        // android.util.Log.d("UsageQuery", "Querying events for $packageName from $startOfToday to $now")
        
        // Use queryEvents for raw data
        val usageEvents = usageStatsManager.queryEvents(startOfToday, now)
        val event = android.app.usage.UsageEvents.Event()
        
        var totalInputTime = 0L
        var lastEventTime = 0L
        var isVisible = false
        
        // Iterate through all events
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            // Filter for our package
            if (event.packageName == packageName) {
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastEventTime = event.timeStamp
                    isVisible = true
                } else if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (isVisible && lastEventTime > 0) {
                        totalInputTime += (event.timeStamp - lastEventTime)
                    }
                    isVisible = false
                }
            }
        }
        
        // If app is currently visible, add time since last open until now
        if (isVisible && lastEventTime > 0) {
            totalInputTime += (now - lastEventTime)
        }
        
        return totalInputTime
    }

    // ==========================================
    // Session Tracking Implementation (Polling)
    // ==========================================

    // In-memory cache for polling efficiency (accessed every 15s)
    private val activeSessions = java.util.concurrent.ConcurrentHashMap<String, SessionState>()

    override suspend fun getCurrentSessionState(packageName: String): SessionState {
        // Return cached state if available
        activeSessions[packageName]?.let { session ->
            if (System.currentTimeMillis() - session.lastChecked < 15 * 60 * 1000) {
                return session
            } else {
                activeSessions.remove(packageName)
            }
        }
        
        val app = appDao.getApp(packageName)
        
        if (app != null) {
            // BLOCKED or expired session
            if (app.sessionState == AppEntity.STATE_BLOCKED) {
                val finishedSession = SessionState(
                    packageName = packageName,
                    sessionStart = app.sessionStartTime,
                    totalSessionTime = app.selectedSessionDuration, // Show full duration used
                    extensionsGranted = 0L,
                    lastChecked = System.currentTimeMillis()
                )
                activeSessions[packageName] = finishedSession
                return finishedSession
            }
            
            // Active/Paused session with valid remaining time
            if ((app.sessionState == AppEntity.STATE_ACTIVE || app.sessionState == AppEntity.STATE_PAUSED) &&
                app.remainingSessionTime > 0 && app.selectedSessionDuration > 0) {
                
                val totalUsed = (app.selectedSessionDuration - app.remainingSessionTime).coerceAtLeast(0L)
                
                val restoredSession = SessionState(
                    packageName = packageName,
                    sessionStart = app.sessionStartTime,
                    totalSessionTime = totalUsed,
                    extensionsGranted = 0L,
                    lastChecked = System.currentTimeMillis()
                )
                activeSessions[packageName] = restoredSession
                return restoredSession
            }
        }
        
        // New session - default state
        val defaultState = SessionState(
            packageName = packageName,
            sessionStart = System.currentTimeMillis(),
            totalSessionTime = 0L,
            extensionsGranted = 0L,
            lastChecked = System.currentTimeMillis()
        )
        
        activeSessions[packageName] = defaultState
        return defaultState
    }

    override suspend fun syncUsageWithSystem(context: android.content.Context, packageName: String) {
        // Precise sync with system events to update totalUsageToday
        val systemUsage = calculatePreciseUsage(context, packageName)
        appDao.updateUsage(packageName, systemUsage, System.currentTimeMillis())
    }

    override suspend fun updateSessionTime(packageName: String) {
        val session = activeSessions[packageName] ?: return
        val app = appDao.getApp(packageName) ?: return
        
        // CRITICAL FIX: Do not update time if we are supposed to be PAUSED
        if (app.sessionState == AppEntity.STATE_PAUSED || app.sessionState == AppEntity.STATE_BLOCKED) {
             activeSessions.remove(packageName)
             return
        }
        
        val now = System.currentTimeMillis()
        val delta = (now - session.lastChecked).coerceAtLeast(0L)
        
        val newTotalSessionTime = session.totalSessionTime + delta
        
        val newState = session.copy(
            totalSessionTime = newTotalSessionTime,
            lastChecked = now
        )
        
        activeSessions[packageName] = newState
        
        // Calculate remaining time for DB persistence
        // Total Allowed = Original Duration + Extensions
        // Remaining = Total Allowed - Total Used
        val allowedTime = app.selectedSessionDuration + session.extensionsGranted
        val remainingTime = (allowedTime - newTotalSessionTime).coerceAtLeast(0L)
        
        // CRITICAL FIX: Only update remaining session time, NOT daily usage
        // Daily usage is synced separately from system via syncUsageWithSystem in Receiver
        appDao.updateRemainingTime(packageName, remainingTime)
    }

    override suspend fun grantExtension(packageName: String, minutes: Int) {
        val app = appDao.getApp(packageName) ?: return
        val session = activeSessions[packageName] ?: getCurrentSessionState(packageName)
        val extensionMs = minutes * 60 * 1000L
        
        val newState = session.copy(
            extensionsGranted = session.extensionsGranted + extensionMs
        )
        
        activeSessions[packageName] = newState
        
        // IMMEDIATE PERSISTENCE FIX:
        // Calculate new remaining time immediately so it's saved even if app is killed
        val allowedTime = app.selectedSessionDuration + newState.extensionsGranted
        val remainingTime = (allowedTime - newState.totalSessionTime).coerceAtLeast(0L)
        
        appDao.updateRemainingTime(packageName, remainingTime)
        appDao.updateSessionState(packageName, AppEntity.STATE_ACTIVE)
        
        android.util.Log.d("UnScroll", "Granted ${minutes}m extension for $packageName. New Remaining: ${remainingTime/1000}s")
    }

    override suspend fun endSession(packageName: String) {
        activeSessions.remove(packageName)
        
        // Critical: Zero out remaining time to prevent resumeSession from reactivating
        appDao.updateSessionState(
            packageName = packageName,
            remainingTime = 0L,
            state = AppEntity.STATE_BLOCKED,
            pausedTime = System.currentTimeMillis()
        )
        
        android.util.Log.d("UnScroll", "Ended session for $packageName (remainingTime set to 0, state -> BLOCKED)")
    }

    override suspend fun updateSessionState(packageName: String, state: Int) {
        appDao.updateSessionState(packageName, state)
        
        // Only clear session on explicit BLOCK or expiration
        if (state == AppEntity.STATE_BLOCKED) {
            activeSessions.remove(packageName)
            android.util.Log.d("UnScroll", "Cleared session cache for $packageName (state -> BLOCKED)")
        }
    }

    override suspend fun pauseSession(packageName: String) {
        val app = appDao.getApp(packageName) ?: return
        
        // Only pause if app has an active or paused session
        if (app.sessionState != AppEntity.STATE_ACTIVE && app.sessionState != AppEntity.STATE_PAUSED) {
            return
        }
        
        // Get session from cache, or fallback to DB-based calculation
        val session = activeSessions[packageName]
        
        val remainingTime = if (session != null) {
            // If in memory, use accurate calculation including extensions
            val existingTotal = session.totalSessionTime
            val allowedTime = app.selectedSessionDuration + session.extensionsGranted
            (allowedTime - existingTotal).coerceAtLeast(0L)
        } else {
            // Fallback: if not in memory, use DB's current remaining time
            app.remainingSessionTime.coerceAtLeast(0L)
        }
        
        // Save to database atomically
        appDao.updateSessionState(
            packageName = packageName,
            remainingTime = remainingTime,
            state = AppEntity.STATE_PAUSED,
            pausedTime = System.currentTimeMillis()
        )
        
        // CRITICAL FIX: Remove from activeSessions to prevent accidental updates by Receiver race conditions
        activeSessions.remove(packageName)
        
        android.util.Log.d("UnScroll", "Paused session for $packageName: ${remainingTime}ms remaining. Removed from memory.")
    }

    override suspend fun resumeSession(packageName: String) {
        val app = appDao.getApp(packageName) ?: return
        
        // BLOCKED means permanently blocked, don't resume
        if (app.sessionState == AppEntity.STATE_BLOCKED) {
            android.util.Log.d("UnScroll", "Resume blocked: $packageName is BLOCKED")
            return
        }
        
        // IDLE means no session started yet - this is fine, receiver will handle it
        if (app.sessionState == AppEntity.STATE_IDLE) {
            android.util.Log.d("UnScroll", "Resume skipped: $packageName is IDLE (session not started)")
            return
        }
        
        // For ACTIVE or PAUSED, check if we have time remaining
        if (app.remainingSessionTime > 0 && app.selectedSessionDuration > 0) {
            val totalUsed = (app.selectedSessionDuration - app.remainingSessionTime).coerceAtLeast(0L)
            
            // Sanity check
            if (totalUsed > app.selectedSessionDuration) {
                android.util.Log.e("UnScroll", "Data corruption: totalUsed ($totalUsed) > duration (${app.selectedSessionDuration})")
                // Reset to prevent infinite loop
                appDao.updateSessionState(packageName, AppEntity.STATE_IDLE)
                return
            }
            
            val session = SessionState(
                packageName = packageName,
                sessionStart = app.sessionStartTime,
                totalSessionTime = totalUsed,
                extensionsGranted = 0L, // Note: Extensions not fully persisted yet in this simple plan
                lastChecked = System.currentTimeMillis()
            )
            
            activeSessions[packageName] = session
            
            // Update state to ACTIVE
            appDao.updateSessionState(
                packageName = packageName,
                remainingTime = app.remainingSessionTime,
                state = AppEntity.STATE_ACTIVE,
                pausedTime = 0L
            )
            
            android.util.Log.d("UnScroll", "Resumed session for $packageName: used=${totalUsed}ms, remaining=${app.remainingSessionTime}ms")
        } else {
            android.util.Log.d("UnScroll", "Cannot resume $packageName: remainingTime=${app.remainingSessionTime}, duration=${app.selectedSessionDuration}")
        }
    }
    
    override fun isSessionInMemory(packageName: String): Boolean {
        return activeSessions.containsKey(packageName)
    }
}
