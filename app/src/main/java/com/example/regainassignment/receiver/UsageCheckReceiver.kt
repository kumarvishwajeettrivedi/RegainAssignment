package com.example.regainassignment.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.regainassignment.RegainApplication
import com.example.regainassignment.data.local.AppEntity
import com.example.regainassignment.service.UsageMonitorService
import com.example.regainassignment.ui.block.SoftBlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered every 15 seconds by AlarmManager.
 * 
 * Workflow:
 * 1. Check which app is currently in foreground (UsageStatsManager)
 * 2. Update session time in database
 * 3. Check if session/daily limits exceeded
 * 4. Show soft-blocking dialog if needed
 * 5. Reschedule next check
 * 
 * Battery Impact: Each check takes <100ms
 */
class UsageCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val CHECK_INTERVAL_MS = 2_000L // 2 seconds for real-time updates
        private const val QUERY_WINDOW_MS = 1000L * 60 * 60 * 2 // Look back 2 hours
        
        // SharedPreferences keys
        private const val PREFS_NAME = "regain_prefs"
        private const val KEY_LAST_FOREGROUND_APP = "last_foreground_app"
        private const val KEY_DIALOG_SHOWING = "dialog_showing"
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("Regain", "UsageCheckReceiver: onReceive triggered")
        // Use coroutine for async database operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAndHandleCurrentApp(context)
            } catch (e: Exception) {
                android.util.Log.e("Regain", "Error in UsageCheckReceiver", e)
                e.printStackTrace()
            } finally {
                // Always reschedule next check
                rescheduleNextCheck(context)
            }
        }
    }

    /**
     * Main logic: Check current app and handle limits
     */
    private suspend fun checkAndHandleCurrentApp(context: Context) {
        // Get UsageStatsManager to query foreground app
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) 
            as? UsageStatsManager ?: return
        
        // Optimization: Check very recent foreground changes (last 2 seconds)
        // This reduces delay when app is just opened
        val nowTime = System.currentTimeMillis()
        val recentStartTime = nowTime - 2000L // Last 2 seconds
        
        // First try immediate check with shorter window
        var currentApp = getCurrentForegroundApp(usageStatsManager, recentStartTime, nowTime)
        
        // If no recent change, expand search window
        if (currentApp == null) {
            val endTime = nowTime
            val startTime = endTime - QUERY_WINDOW_MS
            currentApp = getCurrentForegroundApp(usageStatsManager, startTime, endTime)
        }
        
        android.util.Log.d("Regain", "Current Foreground App (before screen check): $currentApp")
        
        // Get repository from Application class
        val repository = (context.applicationContext as RegainApplication).repository
        
        // CRITICAL FIX: If screen is off/locked, pause the current app FIRST before treating as no app
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        if (powerManager?.isInteractive == false) {
            android.util.Log.d("Regain", "Screen is OFF/LOCKED. Pausing current app and stopping monitoring.")
            
            // Pause the current foreground app if any
            currentApp?.let { pkg ->
                val app = repository.getApp(pkg)
                if (app != null && app.isLimitEnabled && app.sessionState == AppEntity.STATE_ACTIVE) {
                    repository.updateSessionTime(pkg) // Final update
                    repository.pauseSession(pkg)
                    android.util.Log.d("Regain", "Paused $pkg due to screen lock")
                }
            }
            
            // Now treat as no app visible
            currentApp = null
        }
        
        android.util.Log.d("Regain", "Current Foreground App (final): $currentApp")
        
        // Handle app switching (save previous session if needed)
        updatePreviousSession(context, currentApp, repository)
        
        // Check if current app has limits enabled
        currentApp?.let { packageName ->
            val app = repository.getApp(packageName)
            
            // If app is not found, or limits not enabled -> Cancel timer
            if (app == null || !app.isLimitEnabled) {
                UsageMonitorService.cancelNotificationTimer()
                return@let
            }
            
            // CRITICAL FIX: If app is BLOCKED, show dialog but don't process further
            // This prevents infinite loop of showing dialog every 2 seconds
            if (app.sessionState == AppEntity.STATE_BLOCKED) {
                android.util.Log.d("Regain", "$packageName is BLOCKED. Showing block dialog only.")
                handleLimitedApp(context, app, repository)
                return@let
            }
            
            if (app.isLimitEnabled) {
                // Only resume if NOT in IDLE state AND not already in memory
                // Calling resumeSession when already active resets lastChecked and breaks time tracking
                if (app.sessionState != AppEntity.STATE_IDLE && !repository.isSessionInMemory(packageName)) {
                    repository.resumeSession(packageName)
                }
                
                // Refresh app state after potential resume
                val updatedApp = repository.getApp(packageName) ?: return@let
                
                handleLimitedApp(context, updatedApp, repository)
            }
        } ?: run {
            // No current app (e.g. home screen) -> Cancel timer and reset notification
            UsageMonitorService.cancelNotificationTimer()
            UsageMonitorService.updateNotification("Regain Active", "Monitoring usage...", 0, 0)
        }
    }

    private suspend fun handleLimitedApp(
        context: Context,
        app: AppEntity,
        repository: com.example.regainassignment.data.repository.UsageRepository
    ) {
        // Prevent multiple dialogs stacking is handled by checking 'dialog_showing' pref,
        // BUT for 'Ask on Open' we might need to be aggressive.
        // We will respect the flag to avoid loops.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DIALOG_SHOWING, false)) return

        when (app.sessionState) {
            AppEntity.STATE_IDLE -> {
                // Case 1: App opened, session not started -> Ask for time
                showSoftBlockDialog(context, app, "ASK_TIME")
            }
            AppEntity.STATE_ACTIVE -> {
                // Case 2: Session running -> Update time and check expiry
                repository.updateSessionTime(app.packageName)
                
                val sessionState = repository.getCurrentSessionState(app.packageName)
                val allowedTime = app.selectedSessionDuration + sessionState.extensionsGranted
                
                // Update Notification with rich data
                val remaining = allowedTime - sessionState.totalSessionTime
                val remainingFormatted = com.example.regainassignment.util.formatTime(remaining)
                val totalFormatted = com.example.regainassignment.util.formatTime(app.totalUsageToday)
                
                // Show daily usage prominently in title
                val title = "${app.appName} - Used $totalFormatted today"
                val text = "Session time left: $remainingFormatted"
                
                android.util.Log.d("Regain", "Notification update: $title | $text")
                
                UsageMonitorService.updateNotification(
                    title, 
                    text,
                    (allowedTime / 1000).toInt(), 
                    ((allowedTime - remaining) / 1000).toInt()
                )
                
                if (sessionState.totalSessionTime >= allowedTime && remaining <= 0) {
                    // Only end if we actually have a valid session with time elapsed
                    if (app.selectedSessionDuration > 0 && sessionState.totalSessionTime > 0) {
                        repository.endSession(app.packageName)
                        showSoftBlockDialog(context, app, "TIME_UP")
                    }
                }
            }
            AppEntity.STATE_BLOCKED -> {
                // Case 3: App is temporarily blocked -> Show "Blocked" screen
                showSoftBlockDialog(context, app, "BLOCKED")
            }
        }
    }

    /**
     * Query UsageStatsManager to find which app is currently in foreground.
     * 
     * How it works:
     * - UsageStatsManager records MOVE_TO_FOREGROUND and MOVE_TO_BACKGROUND events
     * - We query the last 2 hours of events
     * - We iterate to find the LATEST event
     * - If the last event is MOVE_TO_FOREGROUND, that app is currently open.
     * - If the last event is MOVE_TO_BACKGROUND, the user is likely on the home screen/launcher.
     * 
     * Returns: Package name of foreground app if active, or null.
     */
    private fun getCurrentForegroundApp(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long
    ): String? {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        var lastEventType = -1
        var lastPackageName: String? = null
        val event = UsageEvents.Event()
        
        // Iterate through all events to find the last one
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            // We care about Foreground/Background transitions
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || 
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                lastEventType = event.eventType
                lastPackageName = event.packageName
            }
        }
        
        if (lastEventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            return lastPackageName
        }
        
        return null
    }

    /**
     * Handle app switching: End session for previous app
     */
    private suspend fun updatePreviousSession(
        context: Context,
        currentPkg: String?,
        repository: com.example.regainassignment.data.repository.UsageRepository
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousApp = prefs.getString(KEY_LAST_FOREGROUND_APP, null)
        
        // If user switched apps
        if (previousApp != null && previousApp != currentPkg) {
            // Clear dialog flag when switching apps to prevent stuck dialogs
            prefs.edit().putBoolean(KEY_DIALOG_SHOWING, false).apply()
            
            // Critical: Give previous app ONE FINAL time update before pausing
            // This ensures we count all usage up to the moment of switching
            val prevAppEntity = repository.getApp(previousApp)
            if (prevAppEntity != null && prevAppEntity.sessionState == AppEntity.STATE_ACTIVE) {
                repository.updateSessionTime(previousApp)
                android.util.Log.d("Regain", "Final time update for $previousApp before pausing")
            }
            
            // If previous app was BLOCKED, reset to IDLE for next launch
            if (prevAppEntity != null && prevAppEntity.sessionState == AppEntity.STATE_BLOCKED) {
                repository.updateSessionState(previousApp, AppEntity.STATE_IDLE)
                android.util.Log.d("Regain", "Reset $previousApp from BLOCKED to IDLE")
            } else if (prevAppEntity != null) {
                 // Pause session for any other state (ACTIVE, etc)
                 repository.pauseSession(previousApp)
            }
            
            // For ACTIVE sessions, we keep the state as is
            // The session will resume when user returns to the app
        }
        
        // Save current app as "last foreground app"
        prefs.edit().putString(KEY_LAST_FOREGROUND_APP, currentPkg).apply()
    }

    /**
     * Check if session or daily limit exceeded, show dialog if needed
     * (Deprecated/Removed in favor of handleLimitedApp)
     */
     // Removed checkAndShowBlockDialog

    /**
     * Launch soft-blocking dialog activity
     */
    private fun showSoftBlockDialog(
        context: Context,
        app: AppEntity,
        limitType: String // ASK_TIME, TIME_UP, BLOCKED
    ) {
        // Mark dialog as showing to prevent duplicates
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DIALOG_SHOWING, true).apply()
        
        val intent = Intent(context, SoftBlockActivity::class.java).apply {
            putExtra("package_name", app.packageName)
            putExtra("app_name", app.appName)
            putExtra("limit_type", limitType)
            putExtra("usage_time", app.totalUsageToday)
            
            // Required flags to launch activity from BroadcastReceiver
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        
        context.startActivity(intent)
    }

    /**
     * Schedule next check in 15 seconds using AlarmManager
     */
    private fun rescheduleNextCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UsageCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + CHECK_INTERVAL_MS
        
        // Use setExactAndAllowWhileIdle for Doze mode compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
