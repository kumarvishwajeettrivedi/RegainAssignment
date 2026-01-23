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
import com.example.regainassignment.ui.SplashScreen
import com.example.regainassignment.ui.MainScreen
import com.example.regainassignment.ui.onboarding.NameInputScreen
import com.example.regainassignment.ui.onboarding.GoalSelectionScreen
import com.example.regainassignment.ui.onboarding.CharacterSelectionScreen
import com.example.regainassignment.util.PermissionUtils
import com.example.regainassignment.util.OnboardingPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var onboardingPrefs: OnboardingPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UnScrollApp(onboardingPrefs)
                }
            }
        }
    }
}

@Composable
fun UnScrollApp(onboardingPrefs: OnboardingPreferences) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Re-check permissions on resume
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

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                // Check onboarding completion first
                if (!onboardingPrefs.isOnboardingCompleted()) {
                    navController.navigate("onboarding_name") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    // Check permissions
                    val hasUsage = PermissionUtils.hasUsageStatsPermission(context)
                    val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.provider.Settings.canDrawOverlays(context)
                    } else true
                    val hasAlarm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val am = context.getSystemService(android.app.AlarmManager::class.java)
                        am.canScheduleExactAlarms()
                    } else true
                    
                    val nextDest = if (hasUsage && hasOverlay && hasAlarm) "main" else "permissions"
                    
                    navController.navigate(nextDest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
        
        composable("onboarding_name") {
            NameInputScreen { name ->
                onboardingPrefs.saveUserName(name)
                navController.navigate("onboarding_goal")
            }
        }
        
        composable("onboarding_goal") {
            GoalSelectionScreen { goal ->
                onboardingPrefs.saveGoal(goal.title)
                navController.navigate("onboarding_character")
            }
        }
        
        composable("onboarding_character") {
            CharacterSelectionScreen { character ->
                onboardingPrefs.saveCharacter(character.id)
                onboardingPrefs.setOnboardingCompleted()
                
                // After onboarding, check permissions
                val hasUsage = PermissionUtils.hasUsageStatsPermission(context)
                val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else true
                val hasAlarm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val am = context.getSystemService(android.app.AlarmManager::class.java)
                    am.canScheduleExactAlarms()
                } else true
                
                val nextDest = if (hasUsage && hasOverlay && hasAlarm) "main" else "permissions"
                navController.navigate(nextDest) {
                    popUpTo("onboarding_name") { inclusive = true }
                }
            }
        }
        
        composable("permissions") {
            PermissionsScreen(
                navController = navController,
                onboardingPrefs = onboardingPrefs
            )
        }
        
        composable("main") {
            MainScreen(
                diagnosticsNavController = navController,
                onboardingPrefs = onboardingPrefs
            )
        }
        
        composable("diagnostics") {
            DiagnosticScreen()
        }
    }
}