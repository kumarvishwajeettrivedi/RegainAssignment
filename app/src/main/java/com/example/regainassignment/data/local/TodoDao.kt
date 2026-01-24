package com.example.regainassignment.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY scheduledTime ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoEntity?
    
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY scheduledTime ASC")
    fun getIncompleteTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND scheduledTime <= :currentTime")
    fun getDueTodosCount(currentTime: Long): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long
    
    @Update
    suspend fun updateTodo(todo: TodoEntity)
    
    @Delete
    suspend fun deleteTodo(todo: TodoEntity)
    
    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean)
    
    @Query("UPDATE todos SET isInProgress = :isInProgress WHERE id = :id")
    suspend fun updateProgressStatus(id: Long, isInProgress: Boolean)
    
    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteCompletedTodos()

    @Query("DELETE FROM todos WHERE isCompleted = 1 AND scheduledTime < :threshold")
    suspend fun deleteOldCompletedTodos(threshold: Long)
}
