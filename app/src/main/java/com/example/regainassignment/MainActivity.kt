package com.example.regainassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.regainassignment.service.CoreAccessibilityService
import com.example.regainassignment.ui.AppListScreen
import com.example.regainassignment.ui.PermissionsScreen
import com.example.regainassignment.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Using MaterialTheme directly if custom theme not readily available or to ensure simplicity
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RegainApp()
                }
            }
        }
    }
}

@Composable
fun RegainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Initial check
    val hasUsage = PermissionUtils.hasUsageStatsPermission(context)
    val hasOverlay = PermissionUtils.hasOverlayPermission(context)
    val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context, CoreAccessibilityService::class.java)
    val hasNotification = PermissionUtils.hasNotificationPermission(context)
    
    // Check all essential permissions
    val allGrantedInitial = hasUsage && hasOverlay && hasAccessibility && hasNotification
    
    val startDest = if (allGrantedInitial) "app_list" else "permissions"
    
    // Re-check on Resume
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val usage = PermissionUtils.hasUsageStatsPermission(context)
                val overlay = PermissionUtils.hasOverlayPermission(context)
                val accessibility = PermissionUtils.isAccessibilityServiceEnabled(context, CoreAccessibilityService::class.java)
                val notification = PermissionUtils.hasNotificationPermission(context)
                
                if (!usage || !overlay || !accessibility || !notification) {
                     // Only navigate if not already there to avoid loops/stutter
                     val currentRoute = navController.currentDestination?.route
                     if (currentRoute != "permissions") {
                         navController.navigate("permissions") {
                             popUpTo(0) { inclusive = true } // Clear back stack
                         }
                     }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("permissions") {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    navController.navigate("app_list") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        composable("app_list") {
            AppListScreen()
        }
    }
}