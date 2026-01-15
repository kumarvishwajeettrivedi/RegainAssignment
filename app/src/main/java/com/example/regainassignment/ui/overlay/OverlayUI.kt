package com.example.regainassignment.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BlockOverlayContent(
    dailyUsage: String,
    onTimeSelected: (Int) -> Unit,
    appName: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)) // Dim background
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.BottomCenter // Align to bottom
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp), // Flush to bottom
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(), // Handle gesture bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle handle substitute
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Usage Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = dailyUsage,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Open $appName for...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val options = listOf(5, 10, 15, 20)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeOptionButton(options[0], Modifier.weight(1f)) { onTimeSelected(it) }
                    TimeOptionButton(options[1], Modifier.weight(1f)) { onTimeSelected(it) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeOptionButton(options[2], Modifier.weight(1f)) { onTimeSelected(it) }
                    TimeOptionButton(options[3], Modifier.weight(1f)) { onTimeSelected(it) }
                }
            }
        }
    }
}

@Composable
fun TimeOptionButton(minutes: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    OutlinedButton(
        onClick = { onClick(minutes) },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f))
    ) {
        Text(
            text = "$minutes m",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
// SessionTimerContent removed as requested (moved to notification)

@Composable
fun TimeUpContent(
    appName: String,
    usageMinutes: String,
    onClose: () -> Unit,
    onOpenAnyway: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)) // Dim background
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter // Align to bottom
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            
                Text(
                    text = "Time's Up!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You've used $appName for $usageMinutes today.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close $appName", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onOpenAnyway,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Open Anyway (5m)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun TemporaryBlockContent(
    appName: String,
    dailyUsage: String,
    onTimeSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "\uD83D\uDD12", // Lock emoji
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$appName is paused",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Usage Today: $dailyUsage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!showOptions) {
                    // Gatekeeper State
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Close $appName", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showOptions = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Open Anyway", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // Time Selection State
                    Text(
                        text = "Unlock for...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(5, 10, 15, 20)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimeOptionButton(options[0], Modifier.weight(1f)) { onTimeSelected(it) }
                        TimeOptionButton(options[1], Modifier.weight(1f)) { onTimeSelected(it) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimeOptionButton(options[2], Modifier.weight(1f)) { onTimeSelected(it) }
                        TimeOptionButton(options[3], Modifier.weight(1f)) { onTimeSelected(it) }
                    }
                }
            }
        }
    }
}
