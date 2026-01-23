package com.example.regainassignment.ui.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.regainassignment.R

@Composable
fun CharacterSelectionScreen(
    onComplete: (Character) -> Unit
) {
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }
    
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
                .padding(32.dp)
        ) {
            Text(
                text = "Choose your companion",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White // White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Pick a friend to guide you on your journey",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f) // White with alpha
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Grid character selection
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(CHARACTERS) { character ->
                    CharacterCard(
                        character = character,
                        isSelected = selectedCharacter?.id == character.id,
                        onClick = { selectedCharacter = character }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { selectedCharacter?.let { onComplete(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedCharacter != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA726),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text("Let's Go!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: Character,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f), // More compact aspect ratio for emoji display
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFFFFA726).copy(alpha = 0.2f) // Light Orange highlight
            else 
                Color.White // White card
        ),
        border = if (isSelected) 
            BorderStroke(3.dp, Color(0xFFFFA726))
        else 
            null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Always use emoji
            Text(
                text = character.emoji,
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = character.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha=0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
    }
}
