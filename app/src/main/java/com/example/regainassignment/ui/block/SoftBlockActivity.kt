package com.example.regainassignment.ui.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.regainassignment.data.local.AppEntity
import com.example.regainassignment.data.repository.UsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Soft-blocking dialog that appears when session/daily limit is exceeded.
 * 
 * UX Flow:
 * 1. Screen 1: "Close Instagram" or "Continue Anyway"
 * 2. If "Continue Anyway" clicked → Screen 2: Time extension options [5m][10m][15m][20m]
 * 3. User selects time → Extension granted → Dialog closes
 * 4. If limit still exceeded and user reopens app → Dialog shows again
 * 
 * Important: This dialog CANNOT be dismissed
 * - No back button action
 * - No swipe-to-dismiss
 * - User must make a choice
 */
@AndroidEntryPoint
class SoftBlockActivity : ComponentActivity() {

    @Inject
    lateinit var repository: UsageRepository
    
    private var hasUserInteracted = false
    
    private val blockedPackageName by lazy { intent.getStringExtra("package_name") ?: "" }
    private val appName by lazy { intent.getStringExtra("app_name") ?: "" }
    private val limitType by lazy { intent.getStringExtra("limit_type") ?: "ASK_TIME" } // ASK_TIME, TIME_UP, BLOCKED
    private val usageTime by lazy { intent.getLongExtra("usage_time", 0L) }

    /**
     * Disable back button - user must choose an option
     * Pressing back should act as "Close App" (Go Home)
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // If user presses back, they want to leave. So close the app.
        closeApp()
    }

    // For Android 13+
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // Handle back press
                closeApp()
            }
        }
        
        setContent {
            MaterialTheme {
                SoftBlockScreen(
                    appName = appName,
                    limitType = limitType,
                    usageTime = usageTime,
                    onCloseApp = {
                        if (!hasUserInteracted) {
                            hasUserInteracted = true
                            closeApp()
                        }
                    },
                    onContinue = { 
                        // UX-only action (switching screens), no repository call yet
                        // But we can track it if we want to prevent rapid switching
                         // hasUserInteracted = true // Don't block screen navigation
                    },
                    onStartSession = { minutes ->
                        if (!hasUserInteracted) {
                            hasUserInteracted = true
                            startSession(minutes)
                        }
                    }
                )
            }
        }
    }

    /**
     * Close the restricted app
     */
    private fun closeApp() {
        lifecycleScope.launch {
            // Set state to BLOCKED
            repository.updateSessionState(blockedPackageName, AppEntity.STATE_BLOCKED)
            
            clearDialogFlag()
        }
        
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Start/Extend session
     */
    private fun startSession(minutes: Int) {
        lifecycleScope.launch {
            val extensionMs = minutes * 60 * 1000L
            
            when (limitType) {
                "TIME_UP" -> {
                    // User needs more time - extend the existing session
                    repository.grantExtension(blockedPackageName, minutes)
                    repository.updateSessionState(blockedPackageName, AppEntity.STATE_ACTIVE)
                }
                "ASK_TIME", "BLOCKED" -> {
                    // Set session info atomically
                    repository.setSessionInfo(
                        blockedPackageName,
                        System.currentTimeMillis(),
                        extensionMs,
                        0L
                    )
                    // State is already set to ACTIVE inside setSessionInfo
                    
                    // Small delay to ensure database write completes
                    kotlinx.coroutines.delay(100)
                }
            }
            
            clearDialogFlag()
        }
        finish()
    }

    private fun clearDialogFlag() {
        val prefs = getSharedPreferences("regain_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dialog_showing", false).apply()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Always clear dialog flag when activity is destroyed
        // This prevents stuck flag if activity is killed by system
        clearDialogFlag()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftBlockScreen(
    appName: String,
    limitType: String,
    usageTime: Long,
    onCloseApp: () -> Unit,
    onContinue: () -> Unit,
    onStartSession: (Int) -> Unit
) {
    var currentScreen by remember { 
        mutableStateOf(
            when (limitType) {
                "ASK_TIME" -> "TIME_SELECTION"
                "BLOCKED" -> "BLOCKED_MSG"
                else -> "TIME_UP_MSG"
            }
        ) 
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f) // Darker dim
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp), 
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), // More rounded
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag Handle Indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .padding(bottom = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                                androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    when (currentScreen) {
                        "TIME_SELECTION" -> {
                            TimeSelectionContent(appName, onStartSession)
                        }
                        "BLOCKED_MSG" -> {
                             BlockedMsgContent(appName, onCloseApp) {
                                 currentScreen = "TIME_SELECTION"
                                 onContinue()
                             }
                        }
                        "TIME_UP_MSG" -> {
                            TimeUpContent(appName, usageTime, onCloseApp) {
                                 currentScreen = "TIME_SELECTION"
                                 onContinue()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelectionContent(appName: String, onTimeSelected: (Int) -> Unit) {
    Text(
        text = "How much time?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Choose duration for $appName",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeOptionButton(5, Modifier.weight(1f)) { onTimeSelected(it) }
            TimeOptionButton(10, Modifier.weight(1f)) { onTimeSelected(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeOptionButton(15, Modifier.weight(1f)) { onTimeSelected(it) }
            TimeOptionButton(20, Modifier.weight(1f)) { onTimeSelected(it) }
        }
    }
}

@Composable
fun BlockedMsgContent(appName: String, onClose: () -> Unit, onOpenAnyway: () -> Unit) {
    Icon(
        Icons.Default.Warning, 
        contentDescription = null, 
        tint = MaterialTheme.colorScheme.error, 
        modifier = Modifier.size(56.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "$appName is turned off", 
        style = MaterialTheme.typography.headlineSmall, 
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "You've chosen to block this app for now.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
        onClick = onClose, 
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), 
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Text("Keep Closed", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onOpenAnyway, modifier = Modifier.fillMaxWidth()) {
        Text("Open anyway", color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TimeUpContent(appName: String, usageTime: Long, onClose: () -> Unit, onContinue: () -> Unit) {
    Icon(
        Icons.Default.Warning, 
        contentDescription = null, 
        tint = MaterialTheme.colorScheme.tertiary, // Softer warning color 
        modifier = Modifier.size(56.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("Time's Up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "You used $appName for ${com.example.regainassignment.util.formatTime(usageTime)}", 
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge
    )
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
        onClick = onClose, 
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // Positive action to close 
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Text("Close $appName", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("I need more time", color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TimeOptionButton(minutes: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    OutlinedButton(
        onClick = { onClick(minutes) },
        modifier = modifier.height(64.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
