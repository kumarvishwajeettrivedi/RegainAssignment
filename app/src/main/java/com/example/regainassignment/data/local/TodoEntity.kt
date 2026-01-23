package com.example.regainassignment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val scheduledTime: Long, // timestamp in millis
    val isCompleted: Boolean = false,
    val isInProgress: Boolean = false,
    val notificationEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
