package com.example.regainassignment.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.regainassignment.R
import com.example.regainassignment.service.UsageMonitorService
import com.example.regainassignment.util.PermissionUtils
import com.example.regainassignment.util.OnboardingPreferences
import com.example.regainassignment.ui.onboarding.CHARACTERS
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    navController: NavController,
    onboardingPrefs: OnboardingPreferences
) {
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

    // Update state when user returns to app
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Nature Background
        Image(
            painter = painterResource(id = R.drawable.nature_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Character emoji
            val characterId = onboardingPrefs.getCharacter()
            val character = CHARACTERS.find { it.id == characterId } ?: CHARACTERS.first()
            
            Text(
                text = character.emoji,
                fontSize = 120.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Let's Set Things Up!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White // White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle
            Text(
                text = "I need a few permissions to help you stay focused and productive",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f), // White with alpha
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Permission Cards
            PermissionCard(
                icon = Icons.Default.Settings,
                title = "Track Usage",
                description = "Let me help you track your app time",
                isGranted = hasUsageStats,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "I'll send you helpful reminders",
                    isGranted = hasNotification,
                    onClick = {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PermissionCard(
                icon = Icons.Default.Phone,
                title = "Display Overlay",
                description = "I'll gently remind you to stay focused",
                isGranted = hasOverlay,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Battery Optimization
            var hasBatteryUsage by remember { mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context)) }
            
            // Check battery optimization on resume
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                         hasBatteryUsage = PermissionUtils.isIgnoringBatteryOptimizations(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            PermissionCard(
                icon = Icons.Default.Info,
                title = "Ignore Battery Opt",
                description = "Allow me to work in background",
                isGranted = hasBatteryUsage,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Continue Button
            val allGranted = hasUsageStats && hasNotification && hasOverlay && hasBatteryUsage
            Button(
                onClick = {
                    if (allGranted) {
                        val intent = Intent(context, UsageMonitorService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        
                        // Navigate to onboarding if not completed, otherwise to main
                        if (!onboardingPrefs.isOnboardingCompleted()) {
                            navController.navigate("onboarding_name") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        } else {
                            navController.navigate("main") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        }
                    }
                },
                enabled = allGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA726),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (allGranted) "Continue" else "Grant All Permissions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = { if (!isGranted) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                Color(0xFF4CAF50).copy(alpha = 0.2f) // Light Green for granted
            else 
                Color.White // White for pending
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        enabled = !isGranted
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isGranted) 
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else 
                            Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) 
                        Color(0xFF2E7D32) // Dark Green
                    else 
                        Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha=0.6f)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            // Status
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF4CAF50), // Green check
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Grant",
                    tint = Color.Black.copy(alpha=0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
