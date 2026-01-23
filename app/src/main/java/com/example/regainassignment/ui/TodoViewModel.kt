package com.example.regainassignment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regainassignment.data.local.TodoEntity
import com.example.regainassignment.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {
    
    val allTodos: StateFlow<List<TodoEntity>> = todoRepository.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val incompleteTodos: StateFlow<List<TodoEntity>> = todoRepository.getIncompleteTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val dueTodosCount: StateFlow<Int> = todoRepository.getDueTodosCount(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    fun addTodo(title: String, subtitle: String, scheduledTime: Long) {
        viewModelScope.launch {
            val todo = TodoEntity(
                title = title,
                subtitle = subtitle,
                scheduledTime = scheduledTime
            )
            todoRepository.insertTodo(todo)
        }
    }
    
    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.updateTodo(todo)
        }
    }
    
    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }
    
    fun toggleCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.toggleCompletion(id, isCompleted)
        }
    }
    
    fun toggleProgress(id: Long, isInProgress: Boolean) {
        viewModelScope.launch {
            todoRepository.toggleProgress(id, isInProgress)
        }
    }
    
    fun deleteCompletedTodos() {
        viewModelScope.launch {
            todoRepository.deleteCompletedTodos()
        }
    }
}
