package com.example.regainassignment

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.regainassignment.worker.MidnightResetWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class RegainApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var repository: com.example.regainassignment.data.repository.UsageRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleMidnightReset()
        scheduleServiceWatchdog()
        startMonitoringService()
    }
    
    private fun startMonitoringService() {
        val serviceIntent = android.content.Intent(this, com.example.regainassignment.service.UsageMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun scheduleMidnightReset() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        // Set execution around 00:00:00
        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        dueDate.set(Calendar.MILLISECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        
        val dailyWorkRequest = PeriodicWorkRequestBuilder<MidnightResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MidnightReset",
            ExistingPeriodicWorkPolicy.UPDATE, // Update ensures we don't accumulate
            dailyWorkRequest
        )
    }

    private fun scheduleServiceWatchdog() {
        val watchdogRequest = PeriodicWorkRequestBuilder<com.example.regainassignment.worker.ServiceCheckWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceWatchdog",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            watchdogRequest
        )
    }
}
