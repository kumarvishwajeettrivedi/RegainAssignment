package com.example.regainassignment.data.repository

import com.example.regainassignment.data.local.TodoDao
import com.example.regainassignment.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor(
    private val todoDao: TodoDao
) {
    fun getAllTodos(): Flow<List<TodoEntity>> = todoDao.getAllTodos()
    
    fun getIncompleteTodos(): Flow<List<TodoEntity>> = todoDao.getIncompleteTodos()
    
    fun getDueTodosCount(currentTime: Long): Flow<Int> = todoDao.getDueTodosCount(currentTime)
    
    suspend fun getTodoById(id: Long): TodoEntity? = todoDao.getTodoById(id)
    
    suspend fun insertTodo(todo: TodoEntity): Long = todoDao.insertTodo(todo)
    
    suspend fun updateTodo(todo: TodoEntity) = todoDao.updateTodo(todo)
    
    suspend fun deleteTodo(todo: TodoEntity) = todoDao.deleteTodo(todo)
    
    suspend fun toggleCompletion(id: Long, isCompleted: Boolean) {
        todoDao.updateCompletionStatus(id, isCompleted)
    }
    
    suspend fun toggleProgress(id: Long, isInProgress: Boolean) {
        todoDao.updateProgressStatus(id, isInProgress)
    }
    
    suspend fun deleteCompletedTodos() = todoDao.deleteCompletedTodos()
}
