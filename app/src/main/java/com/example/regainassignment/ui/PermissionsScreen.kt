package com.example.regainassignment.ui

import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.regainassignment.util.PermissionUtils

@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsageStats by remember { mutableStateOf(false) }
    var hasNotification by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotification = isGranted
            // Check all again
            if (hasUsageStats && hasNotification) {
                onAllPermissionsGranted()
            }
        }
    )

    fun checkPermissions() {
        hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
        hasNotification = PermissionUtils.hasNotificationPermission(context)
        val hasOverlay = Settings.canDrawOverlays(context)

        if (hasUsageStats && hasNotification && hasOverlay) {
            onAllPermissionsGranted()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Permissions Required",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "App Limiter needs these permissions to function correctly.",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            PermissionItem(
                title = "Usage Access",
                description = "To track how much time you spend on apps.",
                isGranted = hasUsageStats,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    title = "Notifications",
                    description = "To show active session timer in status bar.",
                    isGranted = hasNotification,
                    onClick = {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Overlay Permission (Critical for reliable blocking)
            val hasOverlay = Settings.canDrawOverlays(context)
             PermissionItem(
                title = "Display Over Other Apps",
                description = "Required to show the timer/blocking screen when you open apps.",
                isGranted = hasOverlay,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (isGranted) {
                Text(text = "GRANTED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                Button(onClick = onClick) {
                    Text("Grant")
                }
            }
        }
    }
}
