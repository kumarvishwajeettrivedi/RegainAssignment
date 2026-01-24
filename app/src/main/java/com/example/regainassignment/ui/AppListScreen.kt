package com.example.regainassignment.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.regainassignment.data.local.AppEntity
import com.example.regainassignment.ui.components.FocusScreenHeader
import com.example.regainassignment.ui.components.AppIcon
import com.example.regainassignment.ui.components.UnscrollFooter
import com.example.regainassignment.util.OnboardingPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onboardingPrefs: OnboardingPreferences
) {
    val apps by viewModel.appList.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Get user info
    val userName = onboardingPrefs.getUserName()
    val characterId = onboardingPrefs.getCharacter()
    
    // Hardcoded list of distraction keywords - UPDATED with Discord, YouTube, and Chrome
    val distractionKeywords = listOf(
        "instagram", "whatsapp", "snapchat", "twitter", "facebook", "tiktok",
        "discord", "youtube", "chrome", "game", "freefire", "pubg", "callofduty", "tencent"
    )
    
    // Split apps into Distractions and Others
    val distractionApps = apps.filter { app -> 
        distractionKeywords.any { k -> app.packageName.contains(k, ignoreCase = true) || app.appName.contains(k, ignoreCase = true) }
    }
    
    val otherApps = if (searchQuery.isBlank()) {
        apps.filter { !distractionApps.contains(it) }
    } else {
        apps.filter { !distractionApps.contains(it) && it.appName.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF2F2F7)) // Light Grey (iOS style)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header with character and user info
        FocusScreenHeader(
            userName = userName,
            characterId = characterId,
            dueTodosCount = 0, // Will be updated when we integrate todos
            onSettingsClick = {
                navController.navigate("diagnostics")
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding(), // Move up with keyboard
            contentPadding = PaddingValues(bottom = 96.dp, top = 16.dp) // Extra bottom padding for floating nav
        ) {
            // Unproductive Group
            if (distractionApps.isNotEmpty()) {
                item {
                    UnproductiveGroupCard(
                        apps = distractionApps,
                        onLimitAll = {
                            val allLimited = distractionApps.all { it.isLimitEnabled }
                            distractionApps.forEach { app ->
                                viewModel.toggleAppLimit(app.packageName, !allLimited)
                            }
                        },
                        onToggleAppLimit = { pkg, enabled ->
                             viewModel.toggleAppLimit(pkg, enabled)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Search & Other Apps
            item {
                Text(
                    text = "All Apps",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(items = otherApps, key = { it.packageName }) { app ->
                AppItemCard(
                    app = app,
                    onToggleLimit = { enabled ->
                        viewModel.toggleAppLimit(app.packageName, enabled)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UnscrollFooter()
            }
        }
    }
    }
}



@Composable
fun UnproductiveGroupCard(
    apps: List<AppEntity>, 
    onLimitAll: () -> Unit,
    onToggleAppLimit: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allLimited = apps.all { it.isLimitEnabled }
    val visibleApps = if (expanded) apps else apps.take(3)
    val hasMore = apps.size > 3
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No shadow
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${apps.size} Distracting Apps Found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show apps with undo option if limited
            visibleApps.forEach { app ->
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(vertical = 8.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     // Real app icon
                     AppIcon(
                         packageName = app.packageName,
                         appName = app.appName,
                         size = 40.dp
                     )
                     Spacer(modifier = Modifier.width(12.dp))
                     Column(modifier = Modifier.weight(1f)) {
                         Text(
                             text = app.appName, 
                             style = MaterialTheme.typography.bodyMedium,
                             fontWeight = FontWeight.Medium,
                             color = MaterialTheme.colorScheme.onErrorContainer
                         )
                         // Added missing usage time
                         Text(
                             text = formatUsage(app.totalUsageToday),
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                         )
                     }
                     
                     Switch(
                         checked = app.isLimitEnabled,
                         onCheckedChange = { onToggleAppLimit(app.packageName, it) },
                         colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF6B00), // Accent Orange
                            checkedTrackColor = Color(0xFFFF6B00).copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                         )
                     )
                 }
            }
            
            if (hasMore) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black // Black text/icon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expanded) "Show Less" else "Show More (${apps.size - 3} more)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black // Black text
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Button: either "Limit All" or "Undo All"
            if (allLimited) {
                OutlinedButton(
                    onClick = onLimitAll, // This will undo all when clicked
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Undo All Limits")
                }
            } else {
                Button(
                    onClick = onLimitAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726), // Orange
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limit All")
                }
            }
        }
    }
}

@Composable
fun AppItemCard(
    app: AppEntity,
    onToggleLimit: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No shadow
        colors = CardDefaults.cardColors(containerColor = Color.White) // White card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real app icon
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatUsage(app.totalUsageToday),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = app.isLimitEnabled,
                onCheckedChange = onToggleLimit,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF6B00), // Accent Orange
                    checkedTrackColor = Color(0xFFFF6B00).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        placeholder = { Text("Search your apps...") },
        leadingIcon = { 
            Icon(
                Icons.Default.Search, 
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent, // Transparent inside the bordered box
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.Black, // Black text
            unfocusedTextColor = Color.Black,
            cursorColor = Color.Black
        ),
        singleLine = true
    )
}

fun getCharacterIdString(characterId: Int): String {
    return when (characterId) {
        0 -> "raccoon"
        1 -> "cat"
        2 -> "dog"
        3 -> "bear"
        4 -> "fox"
        5 -> "panda"
        else -> "raccoon"
    }
}

fun getCharacterEmoji(characterId: String): String {
    return when (characterId) {
        "raccoon" -> "🦝"
        "cat" -> "🐱"
        "dog" -> "🐶"
        "bear" -> "🐻"
        "fox" -> "🦊"
        "panda" -> "🐼"
        else -> "🦝" // Default to raccoon
    }
}

fun formatUsage(millis: Long): String {
    val minutes = (millis / 1000) / 60
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
