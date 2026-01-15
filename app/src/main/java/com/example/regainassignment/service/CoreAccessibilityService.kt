package com.example.regainassignment.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import com.example.regainassignment.data.repository.UsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class CoreAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var repository: UsageRepository

    @Inject
    lateinit var overlayManager: OverlayManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackingJob: Job? = null
    private var currentPackageName: String? = null
    private var isPaused = false // For screen lock detection
    
    // Notification variables
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "regain_service_channel"
    private val CHANNEL_NAME = "Regain Monitor Service"
    private lateinit var notificationManager: android.app.NotificationManager
    
    // STRICT TRACKING STATE - Source of Truth while active
    private var activeSessionRemaining: Long = 0L 
    private var activeSessionTotalDuration: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    pauseSession()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isPaused = false
                    // Resume if we were tracking the foreground app
                    currentPackageName?.let { handleAppChange(it) }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager.init(this)
        
        // Setup Notification Channel
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Monitoring usage..."))
        
        // Register screen on/off listener
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }
    
    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_LOW // Low importance to prevent sound/popup on every update
        ).apply {
            description = "Maintains usage tracking and blocking features"
            setShowBadge(false)
        }
        notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    // Helper to get a stable builder
    private fun getNotificationBuilder(title: String, content: String): android.app.Notification.Builder {
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setCategory(android.app.Notification.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true) // CRITICAL: Prevents sound/vibration on updates
    }
    
    private fun createNotification(content: String): android.app.Notification {
        return getNotificationBuilder("Regain Monitor", content).build()
    }

    private fun updateNotification(title: String, content: String, progressMax: Int = 0, progressCurrent: Int = 0) {
        val builder = getNotificationBuilder(title, content)
        
        if (progressMax > 0) {
            builder.setProgress(progressMax, progressCurrent, false)
        } else {
             builder.setProgress(0, 0, false)
        }
        
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Ignore self to avoid loop
            if (packageName == this.packageName) return
            
            // Ignore SystemUI to prevent notification flicker when pulling shade
            if (packageName == "com.android.systemui") return
            
            // Ignore Vivo system UI apps (notification shade, always-on-display, launcher)
            // These trigger when checking notifications but aren't "real" app usage
            if (packageName == "com.vivo.upslide") return  // Notification shade
            if (packageName == "com.vivo.nightpearl") return  // Always-on display
            if (packageName == "android") return  // Generic system/launcher
            
            handleAppChange(packageName)
        }
    }

    private fun handleAppChange(newPackage: String) {
        android.util.Log.d("Regain", "handleAppChange: $newPackage (current: $currentPackageName, tracking: ${trackingJob?.isActive})")
        
        if (currentPackageName == newPackage && trackingJob?.isActive == true) {
            android.util.Log.d("Regain", "Skipping - already tracking $newPackage")
            return
        }
        
        // Stop tracking previous app completely
        stopTracking(saveState = true)
        
        currentPackageName = newPackage
        
        // Check if we need to track/block new one
        serviceScope.launch {
            // First, sync with system usage stats
            val systemUsage = repository.getUsageStatsForToday(this@CoreAccessibilityService, newPackage)
            
            // Update local DB with system usage to ensure they match
            if (systemUsage > 0) {
                repository.setUsage(newPackage, systemUsage) 
            }
            
            var app = repository.getApp(newPackage)
            android.util.Log.d("Regain", "App info for $newPackage: limitEnabled=${app?.isLimitEnabled}, remaining=${app?.remainingSessionTime}, blocked=${app?.isTemporarilyBlocked}")
            
            // AUTOMATIC TRACKING
            if (app == null) {
                val pm = packageManager
                val appName = try {
                    val info = pm.getApplicationInfo(newPackage, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    newPackage
                }
                
                app = com.example.regainassignment.data.local.AppEntity(
                    packageName = newPackage,
                    appName = appName,
                    totalUsageToday = systemUsage, // Use fetched usage
                    isLimitEnabled = false
                )
                repository.insertApp(app!!)
                android.util.Log.d("Regain", "Inserted new app: $appName")
            } else {
                 // Update the existing app object with fresh usage
                 app = app!!.copy(totalUsageToday = if (systemUsage > app!!.totalUsageToday) systemUsage else app!!.totalUsageToday)
            }
            
            // Now we definitely have an app object
            if (app!!.isLimitEnabled) {
                android.util.Log.d("Regain", "App has limit enabled")
                // STRICT FLOW LOGIC
                
                // 1. Check if we have remaining time (Active Session)
                if (app!!.remainingSessionTime > 0) {
                    android.util.Log.d("Regain", "Resuming session with ${app!!.remainingSessionTime}ms remaining")
                    // RESUME SESSION
                    startTracking(
                        packageName = app!!.packageName,
                        initialUsage = app!!.totalUsageToday,
                        isLimited = true,
                        remainingTime = app!!.remainingSessionTime,
                        sessionDuration = app!!.selectedSessionDuration
                    )
                } else {
                    android.util.Log.d("Regain", "No active session - need user input")
                    // 2. No active session. Check if blocked or just new session needed.
                    
                    if (app!!.isTemporarilyBlocked) {
                         android.util.Log.d("Regain", "Showing gatekeeper (temporarily blocked)")
                         showTemporaryBlockPrompt(app!!.appName, app!!.packageName, app!!.totalUsageToday)
                    } else {
                        android.util.Log.d("Regain", "Showing time selection")
                        // Just ask for time (Standard Flow)
                        checkLimitAndBlock(
                            packageName = app!!.packageName,
                            appName = app!!.appName,
                            currentUsage = app!!.totalUsageToday
                        )
                    }
                }
            } else {
                 android.util.Log.d("Regain", "Limit OFF - tracking usage only")
                 // Limit OFF but still track usage
                startTracking(app!!.packageName, app!!.totalUsageToday, isLimited = false)
            }
        }
    }
    
    private fun showTemporaryBlockPrompt(appName: String, packageName: String, currentUsage: Long) {
        overlayManager.hideTimer()
        overlayManager.showTemporaryBlockOverlay(
            appName = appName,
            dailyUsage = formatTime(currentUsage),
            onTimeSelected = { minutes ->
                // User chose to Open Anyway -> Start NEW session
                val duration = minutes * 60 * 1000L
                
                serviceScope.launch {
                    repository.setTemporaryBlock(packageName, false)
                    repository.setSessionInfo(packageName, System.currentTimeMillis(), duration, 0) // Expiry irrelevant
                    repository.updateRemainingTime(packageName, duration) // Set full credit
                    
                    startTracking(packageName, currentUsage, true, duration, duration)
                }
            },
            onClose = {
                // User chose Close -> Go Home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        )
    }

    private fun checkLimitAndBlock(
        packageName: String,
        appName: String,
        currentUsage: Long
    ) {
        // Show time selection
        overlayManager.hideTimer()
        overlayManager.showBlocker(
            dailyUsage = formatTime(currentUsage),
            appName = appName,
            onTimeSelected = { minutes ->
                val duration = minutes * 60 * 1000L
                
                serviceScope.launch {
                    repository.setSessionInfo(packageName, System.currentTimeMillis(), duration, 0)
                    repository.updateRemainingTime(packageName, duration) // Full credit
                    
                    startTracking(packageName, currentUsage, true, duration, duration)
                }
            }
        )
    }

    private fun startTracking(
        packageName: String,
        initialUsage: Long,
        isLimited: Boolean,
        remainingTime: Long = 0,
        sessionDuration: Long = 0
    ) {
        android.util.Log.d("Regain", "startTracking: $packageName, isLimited=$isLimited, remaining=$remainingTime, duration=$sessionDuration")
        
        trackingJob?.cancel()
        
        // CRITICAL: Clear any existing overlays immediately
        overlayManager.hideTimer()
        overlayManager.hideBlocker()
        
        var currentUsage = initialUsage
        var accumulatedTime = 0L // Batch DB writes
        
        // Set local state
        activeSessionRemaining = remainingTime
        activeSessionTotalDuration = sessionDuration
        
        // Initial notification update
        val appName = if (packageName.contains(".")) packageName.substringAfterLast(".").capitalize() else packageName
        
        if (isLimited) {
            android.util.Log.d("Regain", "Starting LIMITED session for $appName, remaining=$remainingTime, duration=$sessionDuration")
            val progressMax = (activeSessionTotalDuration / 1000).toInt()
            val progressCurrent = ((activeSessionTotalDuration - activeSessionRemaining) / 1000).toInt()
            updateNotification(
                title = "$appName: ${formatTimeLeft(activeSessionRemaining)} left",
                content = "Session Limit Active",
                progressMax = progressMax,
                progressCurrent = progressCurrent
            )
        } else {
             android.util.Log.d("Regain", "Starting UNLIMITED tracking for $appName")
             updateNotification(title = "Regain Monitor", content = "Tracking $appName: ${formatTime(currentUsage)}")
        }

        trackingJob = serviceScope.launch {
            val tickRate = 1000L
            android.util.Log.d("Regain", "Starting tracking job for $packageName. Limited: $isLimited, Remaining: $activeSessionRemaining")
            
            while (isActive && !isPaused) {
                delay(tickRate) 
                
                // 1. Update Usage
                currentUsage += tickRate
                accumulatedTime += tickRate
                
                // 2. Decrement remaining time if limited
                if (isLimited) {
                    activeSessionRemaining -= tickRate
                    android.util.Log.d("Regain", "Tick: $packageName, Remaining: $activeSessionRemaining")
                }
                
                // Batch update usage to DB every 5 seconds
                if (accumulatedTime >= 5000) {
                    repository.updateUsage(packageName, accumulatedTime)
                    if (isLimited) {
                        repository.updateRemainingTime(packageName, activeSessionRemaining)
                    }
                    accumulatedTime = 0L
                    
                    if (!isLimited) {
                         updateNotification("Regain Monitor", "Tracking $appName: ${formatTime(currentUsage)}")
                    }
                }
                
                if (isLimited) {
                    // Update Notification
                    val elapsed = activeSessionTotalDuration - activeSessionRemaining
                    
                    val progressMax = if (activeSessionTotalDuration > 0) (activeSessionTotalDuration / 1000).toInt() else 100
                    val progressCurrent = (elapsed / 1000).toInt()
                    
                    // Update TITLE for visibility
                    updateNotification(
                        title = "$appName: ${formatTimeLeft(activeSessionRemaining)} left",
                        content = "Usage: ${formatTime(currentUsage)}",
                        progressMax = progressMax,
                        progressCurrent = progressCurrent
                    )

                    if (activeSessionRemaining <= 0) {
                        android.util.Log.d("Regain", "Time is up for $packageName")
                        // ... rest of termination logic
                        if (accumulatedTime > 0) {
                            repository.updateUsage(packageName, accumulatedTime)
                            repository.updateRemainingTime(packageName, 0)
                        }
                        
                        overlayManager.hideTimer()
                        val app = repository.getApp(packageName)
                        val fullAppName = app?.appName ?: packageName
                        
                        try {
                            android.util.Log.d("Regain", "Showing TimeUp overlay for $fullAppName")
                            overlayManager.showTimeUp(
                                appName = fullAppName,
                                usageMinutes = formatTime(currentUsage),
                                onClose = {
                                    android.util.Log.d("Regain", "TimeUp: User chose Close")
                                    // User chose Close -> Block + Home
                                    serviceScope.launch {
                                        repository.setTemporaryBlock(packageName, true)
                                        repository.updateRemainingTime(packageName, 0)
                                    }
                                    performGlobalAction(GLOBAL_ACTION_HOME)
                                    updateNotification("Regain Monitor", "Monitoring usage...")
                                    stopTracking(false)
                                },
                                onOpenAnyway = {
                                    android.util.Log.d("Regain", "TimeUp: User chose Open Anyway")
                                    // User chose Open Anyway -> Show Selection
                                    checkLimitAndBlock(packageName, fullAppName, currentUsage)
                                }
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("Regain", "Error showing TimeUp overlay", e)
                            e.printStackTrace()
                        }
                        break 
                    }
                }
            }
        }
    }
    
    private fun pauseSession() {
        val appName = currentPackageName?.let { if (it.contains(".")) it.substringAfterLast(".").capitalize() else it } ?: "App"
        val remaining = if (activeSessionRemaining > 0) formatTimeLeft(activeSessionRemaining) else ""
        
        isPaused = true
        stopTracking(saveState = true)
        
        if (remaining.isNotEmpty()) {
             updateNotification("$appName Paused", "$remaining left stored")
        } else {
             updateNotification("Regain Paused", "Screen Off / Background")
        }
    }

    private fun stopTracking(saveState: Boolean) {
        // Capture state before nulling
        val pkg = currentPackageName
        val remaining = activeSessionRemaining
        
        android.util.Log.d("Regain", "stopTracking called for $pkg, saveState=$saveState, remaining=$remaining")
        
        trackingJob?.cancel()
        trackingJob = null
        
        if (saveState) {
            if (pkg != null && remaining > 0) {
                 // Save exact remaining time on exit/pause - use runBlocking to ensure it completes
                 runBlocking {
                     repository.updateRemainingTime(pkg, remaining)
                     android.util.Log.d("Regain", "Saved remaining time for $pkg: $remaining")
                 }
            }
        }
        
        // CRITICAL FIX: Don't hide overlays or reset notification here
        // Let the new tracking session handle it, otherwise we get flicker
        // Only cleanup if we're truly stopping (not switching apps)
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    private fun formatTimeLeft(millis: Long): String {
        val m = if (millis < 0) 0 else millis
        val seconds = (m / 1000) % 60
        val minutes = (m / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onInterrupt() {
        stopTracking(saveState = true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceScope.cancel()
    }
}
