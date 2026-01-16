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

import com.example.regainassignment.ui.AppListScreen
import com.example.regainassignment.ui.PermissionsScreen
import com.example.regainassignment.ui.DiagnosticScreen
import com.example.regainassignment.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
    
    // Logic to determine start destination
    val hasUsage = PermissionUtils.hasUsageStatsPermission(context)
    val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.provider.Settings.canDrawOverlays(context)
    } else true
    val hasAlarm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val am = context.getSystemService(android.app.AlarmManager::class.java)
        am.canScheduleExactAlarms()
    } else true
    
    val allGranted = hasUsage && hasOverlay && hasAlarm
    val startDest = if (allGranted) "app_list" else "permissions"
    
    // Re-check on resume
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 val u = PermissionUtils.hasUsageStatsPermission(context)
                 val o = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                     android.provider.Settings.canDrawOverlays(context)
                 } else true
                 val a = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val am = context.getSystemService(android.app.AlarmManager::class.java)
                     am.canScheduleExactAlarms()
                 } else true
                 
                 if (!u || !o || !a) {
                     if (navController.currentDestination?.route != "permissions") {
                         navController.navigate("permissions") {
                             popUpTo(0) { inclusive = true }
                         }
                     }
                 }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("permissions") {
            PermissionsScreen(navController = navController)
        }
        composable("app_list") {
            AppListScreen(navController = navController)
        }
        composable("diagnostics") {
            DiagnosticScreen()
        }
    }
}