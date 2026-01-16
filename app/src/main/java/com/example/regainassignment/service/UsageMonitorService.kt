package com.example.regainassignment.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.regainassignment.data.repository.UsageRepository
import com.example.regainassignment.receiver.UsageCheckReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs continuously to monitor app usage.
 */
@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject
    lateinit var repository: UsageRepository

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var usageStatsManager: UsageStatsManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var timerJob: Job? = null
    private var monitorJob: Job? = null
    
    // Track which package we are currently timing to avoid zombies
    private var currentTimerPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, UsageCheckReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        getSharedPreferences("regain_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dialog_showing", false)
            .apply()
        
        // scheduleNextCheck() // Removed
        startMonitoringLoop()
    }
    
    // Updates internal timer job for smooth UI updates
    private fun updateTimer(packageName: String, titleRaw: String, text: String, maxProgress: Int, startProgress: Int) {
        val appName = titleRaw.substringBefore(":")
        
        // Anti-Jumping logic:
        // If we are already timing THIS app, avoid restarting the coroutine to prevent visual glitches.
        if (timerJob?.isActive == true && currentTimerPackage == packageName) {
             // We assume the local timer is accurate enough for short durations.
             // Only restart if we wanted to enforce a sync? 
             // For now, steady state behavior is better.
             return
        }

        timerJob?.cancel()
        currentTimerPackage = packageName
        
        if (maxProgress <= 0) {
            updateNotificationUI(titleRaw, text, 0, 0)
            return
        }
        
        var currentSec = startProgress
        val maxSec = maxProgress
        
        timerJob = scope.launch {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            
            while (isActive && currentSec < maxSec) {
                // 1. Strict Screen Lock Check
                if (!powerManager.isInteractive) {
                    android.util.Log.d("Regain", "Screen locked, pausing visual timer")
                    resetToDefaultNotification()
                    break
                }
            
                // 2. Strict Foreground Check & Immediate Switch Detection
                val currentPkg = getCurrentForegroundPkg()
                
                if (currentPkg != null) {
                    // Check for Launcher
                    if (currentPkg.contains("launcher") || currentPkg.contains("nexuslauncher")) {
                         android.util.Log.d("Regain", "User on Home Screen, pausing timer visual")
                         resetToDefaultNotification()
                         break
                    }
                    
                    // Check for App Switch (Zombie Timer Fix + Fast Response)
                    if (currentPkg != packageName) {
                        android.util.Log.d("Regain", "App switched from $packageName to $currentPkg. Stopping timer.")
                        resetToDefaultNotification()
                        
                        // KEY FIX: Immediately trigger Receiver to handle the new app!
                        // This reduces the delay for the bottom sheet/blocking overlay.
                        sendBroadcast(Intent(this@UsageMonitorService, UsageCheckReceiver::class.java))
                        break
                    }
                }
                
                val remainingSec = maxSec - currentSec 
                val formattedTime = formatTime(remainingSec * 1000L) // this gives mm:ss
                
                val newTitle = "$appName: $formattedTime left"
                
                android.util.Log.d("Regain", "Timer Update: $packageName - $formattedTime left")
                updateNotificationUI(newTitle, text, maxSec, currentSec)
                
                delay(1000)
                currentSec++
            }
            if (currentSec >= maxSec) {
                android.util.Log.d("Regain", "Timer Finished locally for $packageName")
                // Let the Receiver handle the actual blocking logic
            }
        }
    }
    
    private fun getCurrentForegroundPkg(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000L 
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var lastEvent: UsageEvents.Event? = null
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastEvent = event
            }
        }
        return lastEvent?.packageName
    }
    
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun updateNotificationUI(title: String, text: String, max: Int, current: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
        
        if (max > 0) {
            builder.setProgress(max, current, false)
        }
        
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun resetToDefaultNotification() {
        currentTimerPackage = null
        updateNotificationUI("Regain is active", "Monitoring usage...", 0, 0)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors your app usage in the background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Regain is active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun startMonitoringLoop() {
        // Cancel any existing loop to be safe
        monitorJob?.cancel()
        
        monitorJob = scope.launch {
            android.util.Log.d("Regain", "Starting reliable monitoring loop (Interval: ${CHECK_INTERVAL_MS}ms)")
            
            while (isActive) {
                try {
                    // Trigger the check immediately
                    val intent = Intent(this@UsageMonitorService, UsageCheckReceiver::class.java)
                    sendBroadcast(intent)
                    
                    // Wait for the next interval
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    android.util.Log.e("Regain", "Error in monitoring loop", e)
                    // Wait a bit before retrying to avoid crash loops
                    delay(5000)
                }
            }
        }
    }

    // Removed scheduleNextCheck as we now use startMonitoringLoop

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        job.cancel() // Cancel all coroutines
        alarmManager.cancel(pendingIntent)
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        currentTimerPackage = null
        android.util.Log.d("Regain", "Timer cancelled")
        resetToDefaultNotification()
    }

    companion object {
        const val CHANNEL_ID = "regain_usage_channel_v2"
        const val NOTIFICATION_ID = 101
        const val CHECK_INTERVAL_MS = 2000L // 2 seconds for real-time updates

        @Volatile
        var instance: UsageMonitorService? = null

        fun updateNotification(packageName: String, title: String, text: String, max: Int, progress: Int) {
            instance?.updateTimer(packageName, title, text, max, progress)
        }
        
        fun cancelNotificationTimer() {
            instance?.cancelTimer()
        }
    }
}
