package com.example.regainassignment.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.regainassignment.service.UsageMonitorService
import com.example.regainassignment.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State holders
    var hasUsageStats by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasNotification by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionUtils.hasNotificationPermission(context)
            } else true
        )
    }
    var hasOverlay by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }
    
    // Launcher for notification permission
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotification = isGranted }
    )

    // Critical: Update state when user returns to app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotification = PermissionUtils.hasNotificationPermission(context)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hasOverlay = Settings.canDrawOverlays(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Setup Regain", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "To help you reduce screen time, Regain needs a few permissions to function.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Cards
            PermissionItemCard(
                title = "1. Usage Access",
                description = "Required to track how much time you spend on apps like Instagram.",
                isGranted = hasUsageStats,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItemCard(
                    title = "2. Notifications",
                    description = "Required to show the timer and daily usage in status bar.",
                    isGranted = hasNotification,
                    onClick = {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            PermissionItemCard(
                title = "3. Display Over Apps",
                description = "Required to show the 'Time's Up' blocking screen.",
                isGranted = hasOverlay,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            // 4. Background Settings (Generic for Xiaomi/OnePlus/etc)
            // This is manual because we can't easily check it
            PermissionItemCard(
                title = "4. Background Settings",
                description = "Enable 'Background Pop-ups', 'Open new windows while running in background', or 'Allow Background Activity' to ensure the timer works.",
                isGranted = false, // Always show "Open" or handle logic. 
                // Better UI: Use a different button style or just "Open Settings"
                // For consistnecy, we keep isGranted=false but change button text implicitly via Logic?
                // No, standard card expects isGranted.
                // Let's modify the card or just accept it shows "Grant" (we can interpret as "Open")
                onClick = {
                    openBackgroundSettings(context)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Continue Button
            val allGranted = hasUsageStats && hasNotification && hasOverlay
            Button(
                onClick = {
                    if (allGranted) {
                        val intent = Intent(context, UsageMonitorService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        navController.navigate("app_list") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                },
                enabled = allGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (allGranted) "Get Started" else "Grant Permissions to Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// Helper to open manufacturer specific settings
fun openBackgroundSettings(context: Context) {
    try {
        // 1. Try Xiaomi specific intent
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.putExtra("extra_pkgname", context.packageName)
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return
        }
        
        // 2. Try generic Application Details (works for OnePlus, Pixel, etc)
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        settingsIntent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(settingsIntent)
        
    } catch (e: Exception) {
         // Fallback to generic settings if anything crashes
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        settingsIntent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(settingsIntent)
    }
}


@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    // Special handling for the manual "Background Settings" card 
    // We want it to look "Actionable" but not strictly "Red" if ignored, 
    // but the user logic implies it's "Extra". 
    // However, to keep it simple, we reuse the component.
    
    // If it's the "Background Settings" title, we always show "Open Settings" instead of "Grant"
    val isBackgroundCard = title.contains("Background Settings")
    
    // For Background Card, we might visually show it as "Info" or "Warning" style if we wanted,
    // but standard gray/red is fine logic.
    // If we want it to NOT block progress, we treat it as isGranted = false visually but Button = "Open".
    
    Card(
        onClick = { if (!isGranted || isBackgroundCard) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        enabled = (!isGranted || isBackgroundCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            if (isGranted && !isBackgroundCard) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        .padding(4.dp)
                )
            } else {
                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isBackgroundCard) "Open" else "Grant")
                }
            }
        }
    }
}
