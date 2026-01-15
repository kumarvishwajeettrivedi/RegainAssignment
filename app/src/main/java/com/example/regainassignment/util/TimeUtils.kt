package com.example.regainassignment.util

/**
 * Format milliseconds to human-readable time string
 * Examples:
 * - 3600000 -> "1h 0m"
 * - 5400000 -> "1h 30m"
 * - 300000 -> "5m"
 */
fun formatTime(milliseconds: Long): String {
    val totalMinutes = (milliseconds / 60000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
