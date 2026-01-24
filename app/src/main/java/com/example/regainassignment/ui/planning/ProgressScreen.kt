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
import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.compose.ui.graphics.StrokeJoin
import com.example.regainassignment.ui.components.UnscrollFooter

// Colors from spec
private val DarkTeal = Color(0xFF1B4D3E)
private val Purple = Color(0xFF7B68EE)
private val PurpleLight = Color(0xFFA78BFA)
private val LightPurple = Color(0xFFD4BBFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onboardingPrefs: OnboardingPreferences,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Daily", "Weekly", "Monthly")
    
    val streakInfo by viewModel.streakInfo.collectAsState()
    val usageData by viewModel.usageData.collectAsState()
    
    // Initial Load
    LaunchedEffect(Unit) {
        viewModel.loadUsageData(0)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            // Header with character
            ProgressHeader(onboardingPrefs)
        }
        
        item {
            // Time period tabs
            TimePeriodTabs(
                selectedTab = selectedTab,
                tabs = tabs,
                usageData = usageData,
                onTabSelected = { 
                    selectedTab = it 
                    viewModel.loadUsageData(it)
                }
            )
        }
        
        item {
            // Streak calendar
            StreakCalendar(streakInfo = streakInfo)
        }
        
        item {
             // Absolute bottom footer
             UnscrollFooter()
             Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProgressHeader(onboardingPrefs: OnboardingPreferences) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) // Match Planner Height
    ) {
        // Curve Background (Planner Style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    DarkTeal, // Solid color to match Planner
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
        )
        
        // Decorative Circles (Planner Style - Top Left & Bottom Left)
        Box(
            modifier = Modifier
                .offset(x = (-30).dp, y = (-20).dp)
                .offset(x = (-30).dp, y = (-20).dp)
                .size(120.dp)
                .background(Purple, CircleShape) 
        )
        
        // Top Right Circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = 40.dp)
                .offset(x = 20.dp, y = 40.dp)
                .size(80.dp)
                .background(Purple, CircleShape)
                .size(80.dp)
                .background(Purple, CircleShape)
        )
        // Bottom Left Circle
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-30).dp)
                .size(60.dp)
                .background(Purple, CircleShape)
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header: Title Top-Left
            Text(
                text = "Progress",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Character Image
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.character_celebration),
                    contentDescription = "Character",
                    modifier = Modifier.size(160.dp)
                )
            }
            
            // Subtitle / Tagline (Planner Style)
            Text(
                text = "You are doing well, see report",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 4.dp)
            )
            
            // Extra spacing at bottom due to curve
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}







@Composable
fun TimePeriodTabs(
    selectedTab: Int,
    tabs: List<String>,
    usageData: List<Float>,
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
        PerformanceGraph(selectedTab = selectedTab, dataPoints = usageData)
    }
}

@Composable
fun PerformanceGraph(selectedTab: Int, dataPoints: List<Float>) {
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
                    text = "App Usage",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
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
                // Real graph visualization
                GraphVisualization(selectedTab = selectedTab, dataPoints = dataPoints)
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
fun GraphVisualization(selectedTab: Int, dataPoints: List<Float>) {
    // Dynamic max value based on data
    val maxValue = (dataPoints.maxOrNull() ?: 1f) * 1.2f
    
    // If empty, show placeholder or just empty
    if (dataPoints.isEmpty()) return
    
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
fun StreakCalendar(streakInfo: StreakInfo) {
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
                            text = "Based on Todo completion",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = streakInfo.currentStreak.toString(),
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
                
                // Days of week (Current Week)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Only show current week for simplicity or map logic for multiple rows
                    // The ViewModel returns 7 days for the current week.
                    StreakWeekRow(
                        days = streakInfo.weekDays.takeIf { it.isNotEmpty() } ?: listOf(
                             DayState.Future, DayState.Future, DayState.Future,
                             DayState.Future, DayState.Future, DayState.Future, DayState.Future
                        ),
                        isTodayCompleted = streakInfo.isTodayCompleted
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
fun StreakWeekRow(days: List<DayState>, isTodayCompleted: Boolean = false) {
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
                                .background(
                                    if (isTodayCompleted) Color.White else Color.Transparent, 
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTodayCompleted) {
                                Text(
                                    text = "⭐",
                                    fontSize = 16.sp
                                )
                            }
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


