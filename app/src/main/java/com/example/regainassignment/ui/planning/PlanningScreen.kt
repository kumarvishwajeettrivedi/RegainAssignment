package com.example.regainassignment.ui.planning

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.regainassignment.R
import com.example.regainassignment.data.local.TodoEntity
import com.example.regainassignment.ui.TodoViewModel
import com.example.regainassignment.util.OnboardingPreferences
import java.text.SimpleDateFormat
import java.util.*

private val DarkTeal = Color(0xFF1B4D3E)
private val Purple = Color(0xFF9B59B6)
private val CalendarSelectedColor = Color(0xFF536DFE) // Blue-ish pop for selected date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    viewModel: TodoViewModel = hiltViewModel(),
    onboardingPrefs: OnboardingPreferences
) {
    val todos by viewModel.allTodos.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    
    // Calendar State
    val calendar = remember { Calendar.getInstance() }
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    
    // Selected Date State (Default to today)
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Detailed Todo State
    var selectedTodo by remember { mutableStateOf<TodoEntity?>(null) }
    
    // Filter todos
    val filteredTodos = remember(todos, selectedDate) {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        todos.filter { it.scheduledTime in startOfDay..endOfDay }
            .sortedBy { it.scheduledTime }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF9F0)), // Creamy background like ref
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                PlannerHeader()
            }
            
            // Monthly Calendar Strip
            item {
                MonthlyCalendarStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { newDate -> selectedDate = newDate }
                )
            }
            
            // Add Tasks Button (Right Aligned, Half Width)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val isPastDate = isDateInPast(selectedDate)
                    
                    Button(
                        onClick = { 
                            if (!isPastDate) showAddSheet = true 
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.5f) // Half width
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isPastDate
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Tasks", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp)) // Added space after button
            }
            
            // Tasks
            items(filteredTodos) { todo ->
                TodoTimelineItem(
                    todo = todo,
                    onToggleComplete = { viewModel.toggleCompletion(todo.id, !todo.isCompleted) },
                    onDelete = { viewModel.deleteTodo(todo) },
                    onClick = { selectedTodo = todo }
                )
            }
            
            // Empty State
            if (filteredTodos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tasks planned",
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    
    if (showAddSheet) {
        AddTaskBottomSheet(
            initialDate = selectedDate.timeInMillis,
            onDismiss = { showAddSheet = false },
            onAdd = { title, description, time ->
                viewModel.addTodo(title, description, time)
                showAddSheet = false
            }
        )
    }
    
    if (selectedTodo != null) {
        TodoDetailDialog(
            todo = selectedTodo!!,
            onDismiss = { selectedTodo = null },
            onDelete = {
                viewModel.deleteTodo(selectedTodo!!)
                selectedTodo = null
            },
            onToggleComplete = {
                val current = selectedTodo!!
                viewModel.toggleCompletion(current.id, !current.isCompleted)
                // Update local state instance to reflect change immediately in dialog if needed, 
                // but since 'todos' is a flow, the dialog might not auto-update if we pass a static entity.
                // However, we are passing 'selectedTodo' which is state. 
                // Better approach: Find the up-to-date todo from 'todos' list to pass to dialog?
                // Or just toggle and assume list behind updates. 
                // User said "tick is clicked then just fill the circle". 
                // Let's rely on the fact that if we toggle, the flow updates, but 'selectedTodo' is a distinct object.
                // We should probably update 'selectedTodo' to match the new state.
                selectedTodo = current.copy(isCompleted = !current.isCompleted)
            }
        )
    }
}

// Helper to check if date is strictly in the past (before today)
fun isDateInPast(cal: Calendar): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val checkDate = Calendar.getInstance().apply {
        timeInMillis = cal.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    return checkDate.before(today)
}

@Composable
fun PlannerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Curve Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    DarkTeal, // Solid color to match button
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
        )
        
        // Decorative Circles (Soft Overlay)
        Box(
            modifier = Modifier
                .offset(x = (-30).dp, y = (-20).dp)
                .offset(x = (-30).dp, y = (-20).dp)
                .size(120.dp)
                .background(Purple, CircleShape) // Solid Purple
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = 40.dp)
                .offset(x = 20.dp, y = 40.dp)
                .size(80.dp)
                .background(Purple, CircleShape) // Solid Purple
                .size(80.dp)
                .background(Purple, CircleShape) // Solid Purple
        )
        // New Circle (Bottom Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-30).dp)
                .size(60.dp)
                .background(Purple, CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Title Top-Left
            Text(
                text = "Planner",
                fontSize = 34.sp, // Larger Header
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Character Image (Static)
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Using the specific resource or a placeholder if not strictly available by name
                // Assuming R.drawable.character_togo exists based on file search
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.character_togo_new), // UI UPDATE: New Image
                    contentDescription = "Character",
                    modifier = Modifier.size(150.dp) // Reduced size (was 200.dp)
                )
            }
            
            // Tagline
            Text(
                text = "Plan your day with togo.",
                fontSize = 20.sp, // A bit big (was 18.sp)
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun MonthlyCalendarStrip(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH) // 1-based
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentDay - 2).coerceAtLeast(0))
    
    // Generate list of days for current month
    val days = remember {
        (1..daysInMonth).map { day ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            cal
        }
    }
    
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(days) { date ->
            val dayNum = date.get(Calendar.DAY_OF_MONTH)
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date.time)
            val isSelected = date.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
            val isToday = dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            
            // Clean Text UI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(48.dp) // Slightly wider touch target
                    .clickable { onDateSelected(date) }
            ) {
                Text(
                    text = dayName,
                    fontSize = 13.sp, // Larger Day Name
                    color = if (isSelected) CalendarSelectedColor else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = dayNum.toString(),
                    fontSize = 20.sp, // Larger Day Number
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) CalendarSelectedColor else Color.Black
                )
            }
        }
    }
}

@Composable
fun TodoTimelineItem(
    todo: TodoEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    val isOverdue = !todo.isCompleted && todo.scheduledTime < currentTime
    
    val colorIndex = (todo.id % 4).toInt()
    val cardColor = when (colorIndex) {
        0 -> Color(0xFFFF8A80) // Reddish Pink
        1 -> Color(0xFF9FA8DA) // Periwinkle
        2 -> Color(0xFFA5D6A7) // Green
        else -> Color(0xFFFFD54F) // Amber
    }
    
    // Timeline Color: Static Purple (No animation as requested)
    val timelineColor = Purple
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Match card height
            .padding(horizontal = 16.dp)
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.White, CircleShape) 
                    .border(2.dp, timelineColor, CircleShape) 
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f) // Fills the remaining height
                    .background(timelineColor) 
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onClick() }, // Open Detail View
            shape = RoundedCornerShape(8.dp), // Slightly square corners like ref
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) { // Remove padding from here
                Column(modifier = Modifier.padding(20.dp)) { // Increased padding (was 16dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = todo.title,
                            fontSize = 20.sp, // Larger task title (was 18.sp)
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        
                        Text(
                            text = timeFormat.format(Date(todo.scheduledTime)),
                            fontSize = 15.sp, // Larger time (was 14.sp)
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = todo.subtitle,
                        fontSize = 14.sp, // Larger subtitle (was 12.sp)
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp)) // Space before controls
                    
                    // Controls Row (Bottom Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         // Delete button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                           Icon(
                               imageVector = Icons.Outlined.Delete,
                               contentDescription = "Delete Task",
                               tint = Color.White.copy(alpha = 0.8f)
                           )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Done Circle Button (Same size as delete - 24dp)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (todo.isCompleted) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .border(2.dp, Color.White, CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) { onToggleComplete() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (todo.isCompleted) {
                                // Checkmark icon
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = cardColor, // Use card color for contrast
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    initialDate: Long,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialDate })
    }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }
    
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val now = Calendar.getInstance()
    
    // Helper to check validity
    fun isValidTime(): Boolean {
        val checkDate = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        return checkDate.after(now)
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Add New Task", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Read-only Date (Locked to selected date in planner)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(dateFormat.format(selectedDate.time), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> selectedHour = hour; selectedMinute = minute },
                        selectedHour,
                        selectedMinute,
                        false
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = DarkTeal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Time", fontSize = 12.sp, color = Color.Gray)
                            Text(String.format("%02d:%02d", selectedHour, selectedMinute), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkTeal)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour)
                        selectedDate.set(Calendar.MINUTE, selectedMinute)
                        selectedDate.set(Calendar.SECOND, 0)
                        
                        if (selectedDate.after(now)) {
                             onAdd(title, description, selectedDate.timeInMillis)
                        } else {
                            Toast.makeText(context, "Cannot set task in the past", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Add Task", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TodoDetailDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    
    // Logic for color same as list
    val colorIndex = (todo.id % 4).toInt()
    val cardColor = when (colorIndex) {
        0 -> Color(0xFFFF8A80)
        1 -> Color(0xFF9FA8DA)
        2 -> Color(0xFFA5D6A7)
        else -> Color(0xFFFFD54F)
    }
    
    val targetTimelineColor = if (todo.isCompleted) Color(0xFFFF9800) else Purple
    val animatedCheckColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetTimelineColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "DetailCheckColor"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f) // Increased width
                .padding(vertical = 16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Removed Close IconButton as requested

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp) // Adjusted padding
                ) {
                    // Header Row: Title (Left) + Controls (Top Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Title
                        Text(
                            text = todo.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        
                        // Controls (Delete + Tick)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                             // Delete
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp) // Smaller 24dp
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Check (24dp same as delete icon)
                            Box(
                                modifier = Modifier
                                    .size(24.dp) // Standardized to 24dp
                                    .background(
                                        if (todo.isCompleted) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { onToggleComplete() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (todo.isCompleted) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = cardColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Date & Time (Combined)
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                             Icon(
                                imageVector = Icons.Default.DateRange, 
                                contentDescription = null, 
                                tint = Color.White, 
                                modifier = Modifier.size(16.dp)
                            )
                             Text(
                                text = "${dateFormat.format(Date(todo.scheduledTime))} • ${timeFormat.format(Date(todo.scheduledTime))}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Description (Full)
                    if (todo.subtitle.isNotBlank()) {
                        Text(
                            text = todo.subtitle,
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 26.sp,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}
