package com.example.regainassignment.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.regainassignment.service.UsageMonitorService
import com.example.regainassignment.util.PermissionUtils

/**
 * Restarts UsageMonitorService after device reboot.
 * 
 * Without this, the monitoring service would stop after reboot
 * and user would need to open the app again.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Only start service if user has granted Usage Stats permission
            if (PermissionUtils.hasUsageStatsPermission(context)) {
                startMonitoringService(context)
            }
        }
    }
    
    private fun startMonitoringService(context: Context) {
        val serviceIntent = Intent(context, UsageMonitorService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
