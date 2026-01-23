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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regainassignment.R

@Composable
fun GoalSelectionScreen(
    onComplete: (Goal) -> Unit
) {
    var selectedGoal by remember { mutableStateOf<Goal?>(null) }
    
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
                text = "What's your main goal?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White // White text
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Choose what matters most to you",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f) // White with alpha
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(GOALS) { goal ->
                    GoalCard(
                        goal = goal,
                        isSelected = selectedGoal?.id == goal.id,
                        onClick = { selectedGoal = goal }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { selectedGoal?.let { onComplete(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedGoal != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA726),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFFFFA726),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFFFFA726).copy(alpha = 0.2f) // Light Orange highlight
            else 
                Color.White // White card
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Flat
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = goal.emoji,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black, // Black text
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha=0.6f), // Dark grey
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
