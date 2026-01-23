package com.example.regainassignment.ui.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

        // Darker Overlay for better text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "What's your main goal?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose what matters most to you right now",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Vertical List Selection
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp), // Space for button
                modifier = Modifier.weight(1f)
            ) {
                items(GOALS) { goal ->
                    GoalListCard(
                        goal = goal,
                        isSelected = selectedGoal?.id == goal.id,
                        onClick = { selectedGoal = goal }
                    )
                }
            }
        }
        
        // Sticky Button at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    kotlin.run {
                        // Gradient fade for button area
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.8f))
                        )
                    }
                )
                .padding(24.dp)
        ) {
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
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text(
                    "Continue", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GoalListCard(
    goal: Goal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color.White 
            else 
                Color.White.copy(alpha = 0.95f)
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, Color(0xFFFFA726))
        else 
            null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // EMOJI Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                         if (isSelected) Color(0xFFFFF3E0) else Color(0xFFF5F5F5),
                         RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = goal.emoji,
                    fontSize = 32.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, Color.Gray.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}
