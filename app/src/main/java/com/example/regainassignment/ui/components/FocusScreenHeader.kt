package com.example.regainassignment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regainassignment.ui.onboarding.CHARACTERS
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FocusScreenHeader(
    userName: String,
    characterId: Int,
    dueTodosCount: Int,
    onSettingsClick: () -> Unit
) {
    // Get character emoji
    val character = CHARACTERS.find { it.id == characterId } ?: CHARACTERS.first()
    
    // Format current date
    val currentDate = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Character + Greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Character avatar
                    val imageRes = com.example.regainassignment.ui.components.CharacterImageLoader.getCharacterImage(character.id)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageRes != null) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = imageRes),
                                contentDescription = character.name,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Text(
                                text = character.emoji,
                                fontSize = 28.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Greeting and date
                    Column {
                        Text(
                            text = "Hello, $userName!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right side: Settings
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSettingsClick() }
                )
            }
        }
    }
}
