package com.example.regainassignment.ui

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.regainassignment.service.UsageMonitorService
import com.example.regainassignment.util.PermissionUtils

@Composable
fun DiagnosticScreen() {
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Permission Status", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Check 1: Overlay Permission
        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        DiagnosticItem(
            label = "Background Windows",
            status = if (hasOverlayPermission) "Allowed ✓" else "DENIED ⚠️",
            description = "Required to show timer dialogs from background",
            isOk = hasOverlayPermission,
            action = if (!hasOverlayPermission) {
                {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            } else null
        )
        
        // Check 2: Exact Alarms (from previous fix)
        val canScheduleAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else {
            true
        }
        
        DiagnosticItem(
            label = "Exact Alarms",
            status = if (canScheduleAlarms) "Granted ✓" else "DENIED ⚠️",
            description = "Required for accurate timer updates",
            isOk = canScheduleAlarms,
            action = if (!canScheduleAlarms) {
                {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            } else null
        )
        
        // Check 3: Usage Stats
        val hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
        
        DiagnosticItem(
            label = "Usage Stats",
            status = if (hasUsageStats) "Granted ✓" else "DENIED ⚠️",
            description = "Required to detect which apps you're using",
            isOk = hasUsageStats,
            action = if (!hasUsageStats) {
                {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            } else null
        )
        
        // Check 4: Service Running
        val serviceRunning = isServiceRunning(context, UsageMonitorService::class.java)
        
        DiagnosticItem(
            label = "Monitoring Service",
            status = if (serviceRunning) "Running ✓" else "Stopped ⚠️",
            description = "Background service that monitors app usage",
            isOk = serviceRunning
        )

        // Check 5: Battery Optimization (New)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        DiagnosticItem(
            label = "Battery Restricted",
            status = if (isIgnoringBatteryOptimizations) "Unrestricted ✓" else "Restricted ⚠️",
            description = "Disable battery saver for this app to prevent killing",
            isOk = isIgnoringBatteryOptimizations,
            action = if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            } else null
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Overall Status
        val allOk = hasOverlayPermission && canScheduleAlarms && hasUsageStats && serviceRunning && isIgnoringBatteryOptimizations
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (allOk) {
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (allOk) "✓ All Checks Passed" else "⚠️ Issues Detected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (allOk) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                
                Text(
                    if (allOk) {
                        "App is correctly configured and ready to use"
                    } else {
                        "Please fix the issues above by tapping 'Fix' buttons. " +
                        "All items must show ✓ for the app to work properly."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Device Info
        Text(
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DiagnosticItem(
    label: String,
    status: String,
    description: String,
    isOk: Boolean,
    action: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                status,
                color = if (isOk) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontWeight = FontWeight.Bold
            )
        }
        
        if (!isOk && action != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = action,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Fix")
            }
        }
    }
    
    HorizontalDivider()
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}
