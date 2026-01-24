package com.example.regainassignment.ui.planning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regainassignment.data.local.TodoEntity
import com.example.regainassignment.data.repository.TodoRepository
import com.example.regainassignment.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StreakInfo(
    val currentStreak: Int,
    val isTodayCompleted: Boolean,
    val weekDays: List<DayState>
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val usageRepository: UsageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _usageData = MutableStateFlow<List<Float>>(emptyList())
    val usageData: StateFlow<List<Float>> = _usageData.asStateFlow()
    
    // Default to empty info
    val streakInfo: StateFlow<StreakInfo> = todoRepository.getAllTodos()
        .map { calculateStreak(it) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            StreakInfo(0, false, emptyList())
        )

    fun loadUsageData(interval: Int) {
        viewModelScope.launch {
            val data = usageRepository.getHistoricalUsage(context, interval)
            _usageData.value = data["total"] ?: emptyList()
        }
    }
    
    private fun calculateStreak(todos: List<TodoEntity>): StreakInfo {
        // 1. Group todos by day (midnight timestamp)
        val todosByDay = todos.groupBy { 
            val c = Calendar.getInstance().apply { timeInMillis = it.scheduledTime }
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        
        // 2. Calculate completion status for each day
        // Any completed todo = Streak (Motivation mode)
        val dayCompletion = todosByDay.mapValues { (_, dayTodos) ->
             dayTodos.isNotEmpty() && dayTodos.any { it.isCompleted }
        }
        
        // 3. Calculate Current Streak (looking back from Today/Yesterday)
        var currentStreak = 0
        val today = Calendar.getInstance().apply {
             set(Calendar.HOUR_OF_DAY, 0)
             set(Calendar.MINUTE, 0)
             set(Calendar.SECOND, 0)
             set(Calendar.MILLISECOND, 0)
        }
        
        val checkDate = Calendar.getInstance().apply { timeInMillis = today.timeInMillis }
        
        // Basic consecutive check logic with new "Any" rule
        // Check Today first
        val todayMillis = checkDate.timeInMillis
        val isTodayDone = dayCompletion[todayMillis] ?: false
        if (isTodayDone) currentStreak++
        
        checkDate.add(Calendar.DAY_OF_YEAR, -1)
        while(true) {
            val dMillis = checkDate.timeInMillis
            val isDone = dayCompletion[dMillis] ?: false
            if(isDone) {
                currentStreak++
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // If it was skipped, break? User requested "Weekly based on todo".
                // "When as soon as user compelte any todo their strek will be counted"
                // Standard streak logic implies consecutive. "Weekly" might imply just count for this week?
                // But Streak typically means consecutive. I will keep consecutive but "eased" (any todo).
                break
            }
        }
        
        // 4. Generate data for Current Week (Mon-Sun)
        val weekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val days = mutableListOf<DayState>()
        for (i in 0..6) {
            val d = Calendar.getInstance().apply { timeInMillis = weekStart.timeInMillis }
            d.add(Calendar.DAY_OF_YEAR, i)
            val dMillis = d.timeInMillis
            
            val state = if (dMillis > todayMillis) {
                DayState.Future
            } else if (dMillis == todayMillis) {
                DayState.Current 
            } else {
                val isDone = dayCompletion[dMillis] ?: false
                if (isDone) DayState.Completed else DayState.Missed
            }
            days.add(state)
        }
        
        return StreakInfo(currentStreak, isTodayDone, days)
    }
}
