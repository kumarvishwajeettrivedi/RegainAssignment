package com.example.regainassignment.ui.planning

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.regainassignment.R
import com.example.regainassignment.util.OnboardingPreferences

// Colors from spec
private val DarkTeal = Color(0xFF1B4D3E)
private val Purple = Color(0xFF7B68EE)
private val PurpleLight = Color(0xFFA78BFA)
private val LightPurple = Color(0xFFD4BBFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onboardingPrefs: OnboardingPreferences
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Daily", "Weekly", "Monthly")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        item {
            // Header with character
            ProgressHeader(onboardingPrefs)
        }
        
        item {
            // User name & level card
            UserLevelCard()
        }
        
        item {
            // Milestone progress
            MilestoneProgress()
        }
        
        item {
            // Time period tabs
            TimePeriodTabs(
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )
        }
        
        item {
            // Streak calendar
            StreakCalendar()
        }
        
        item {
            // Coupons section
            CouponsSection()
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProgressHeader(onboardingPrefs: OnboardingPreferences) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Background with decorative elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkTeal, Color(0xFF2D6A57))
                    )
                )
        )
        
        // Purple triangle (left side)
        Box(
            modifier = Modifier
                .offset(x = (-70).dp, y = 20.dp)
                .size(120.dp, 160.dp)
                .background(Purple.copy(alpha = 0.6f))
        )
        
        // Purple circles (right side)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 20.dp)
                .size(60.dp)
                .background(PurpleLight.copy(alpha = 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 100.dp)
                .size(90.dp)
                .background(PurpleLight.copy(alpha = 0.4f), CircleShape)
        )
        
        // Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Back */ }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "Progress",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Placeholder for balance
                Box(modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Character holding chart
            Text(
                text = "🦝",
                fontSize = 80.sp
            )
        }
    }
}

@Composable
fun UserLevelCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-40).dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Name and level
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Togo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                Text(
                    text = " · ",
                    fontSize = 20.sp,
                    color = Color.Gray
                )
                Text(
                    text = "100 XP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(LightPurple)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status message
            Text(
                text = "Complete your daily task by night to get max XP",
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MilestoneProgress() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Milestone dots and line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                // Progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(6.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Purple, Color(0xFF9B8EFF))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(6.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp))
                )
                
                // Milestone dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MilestoneDot(completed = true, label = "0 XP")
                    MilestoneDot(completed = true, label = "50 XP")
                    MilestoneDot(completed = false, current = true, label = "100 XP")
                    MilestoneDot(completed = false, label = "150 XP")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Complete your daily task by night to get max progress",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun MilestoneDot(
    completed: Boolean = false,
    current: Boolean = false,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (current) 28.dp else 20.dp)
                .background(
                    if (completed || current) Purple else Color.White,
                    CircleShape
                )
                .border(
                    width = if (current) 4.dp else 3.dp,
                    color = if (completed || current) Color.White else Color(0xFFBDBDBD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (completed || current) Purple else Color.Gray
        )
    }
}

@Composable
fun TimePeriodTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Button(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == index) DarkTeal else Color.Transparent,
                        contentColor = if (selectedTab == index) Color.White else Color.Gray
                    ),
                    border = if (selectedTab == index) null else BorderStroke(2.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        
        // Performance Graph
        PerformanceGraph(selectedTab = selectedTab)
    }
}

@Composable
fun PerformanceGraph(selectedTab: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Usage Control",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Text(
                        text = "Under Limit",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Graph canvas area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Mock graph visualization
                GraphVisualization(selectedTab = selectedTab)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = when (selectedTab) {
                    0 -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    1 -> listOf("W1", "W2", "W3", "W4")
                    else -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                }
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GraphVisualization(selectedTab: Int) {
    // Mock data points - in real app, this would come from ViewModel
    val dataPoints = remember(selectedTab) {
        when (selectedTab) {
            0 -> listOf(2.5f, 2.8f, 2.2f, 3.1f, 2.6f, 2.9f, 2.4f) // Daily (hours)
            1 -> listOf(18f, 20f, 17f, 22f) // Weekly (hours)
            else -> listOf(85f, 92f, 78f, 88f, 95f, 87f) // Monthly (hours)
        }
    }
    val maxValue = 4f // Daily limit in hours
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val spacing = width / (dataPoints.size - 1)
        
        // Draw data line with fill
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value / maxValue * height).coerceIn(0f, height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            // Draw data points
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                radius = 4.dp.toPx(),
                center = Offset(x, y),
                style = Fill
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White,
                radius = 6.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Draw the line
        drawPath(
            path = path,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Fill under line
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f),
                    androidx.compose.ui.graphics.Color.Transparent
                )
            )
        )
    }
}

@Composable
fun StreakCalendar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal =16.dp)
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF7C3AED)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with streak info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Streak",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Longest: 12 days",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "5",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Column {
                            Text(
                                text = "Days",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "🔥",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Days of week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Streak day cells (4 weeks)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Week 1 - showing pattern: completed, completed, completed, completed, completed, current, future
                    StreakWeekRow(
                        days = listOf(
                            DayState.Completed, DayState.Completed, DayState.Completed,
                            DayState.Completed, DayState.Completed, DayState.Current, DayState.Future
                        )
                    )
                    // Week 2 - past week with one missed day
                    StreakWeekRow(
                        days = listOf(
                            DayState.Completed, DayState.Completed, DayState.Missed,
                            DayState.Completed, DayState.Completed, DayState.Completed, DayState.Completed
                        )
                    )
                    // Week 3 - older week
                    StreakWeekRow(
                        days = listOf(
                            DayState.Completed, DayState.Completed, DayState.Completed,
                            DayState.Completed, DayState.Missed, DayState.Completed, DayState.Completed
                        )
                    )
                    // Week 4 - oldest visible week
                    StreakWeekRow(
                        days = listOf(
                            DayState.Completed, DayState.Missed, DayState.Completed,
                            DayState.Completed, DayState.Completed, DayState.Completed, DayState.Completed
                        )
                    )
                }
            }
        }
    }
}

enum class DayState {
    Completed, Missed, Current, Future
}

@Composable
fun StreakWeekRow(days: List<DayState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { state ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    DayState.Completed -> {
                        // White circle with yellow star
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 16.sp
                            )
                        }
                    }
                    DayState.Missed -> {
                        // Dark purple circle (missed day)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color(0xFF6B21A8).copy(alpha = 0.8f),
                                    CircleShape
                                )
                        )
                    }
                    DayState.Current -> {
                        // White circle with purple border (current day)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(3.dp, Color.White, CircleShape)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Can show star if completed today
                            Text(
                                text = "⭐",
                                fontSize = 16.sp
                            )
                        }
                    }
                    DayState.Future -> {
                        // Hollow outline (future day)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    2.dp,
                                    Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CouponsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Coupons",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            
            // Character illustration
            Text(
                text = "🦝",
                fontSize = 40.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(2) { index ->
                CouponCard()
            }
        }
    }
}

@Composable
fun CouponCard() {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color(0xFF2C2C2C))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎫 GET IT",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                Text(
                    text ="Get Flat 55% OFF*",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "on Cookies Fee",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unlock with 50 XP 🔒",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
