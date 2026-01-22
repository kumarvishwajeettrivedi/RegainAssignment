package com.example.regainassignment.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class ServiceCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        
        if (!isServiceRunning(context, com.example.regainassignment.service.UsageMonitorService::class.java)) {
            android.util.Log.w("Regain", "Watchdog: Service is dead. Restarting...")
            startMonitoringService(context)
        } else {
            android.util.Log.d("Regain", "Watchdog: Service is alive.")
        }
        
        return Result.success()
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, com.example.regainassignment.service.UsageMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
