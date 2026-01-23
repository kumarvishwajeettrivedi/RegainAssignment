package com.example.regainassignment.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.regainassignment.data.local.TodoEntity
import com.example.regainassignment.receiver.TodoAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TodoAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(todo: TodoEntity) {
        if (todo.isCompleted || todo.scheduledTime <= System.currentTimeMillis()) {
            return
        }

        // Check for permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("TodoAlarmScheduler", "Cannot schedule exact alarms: permission denied")
                // In a real app, you might show a dialog or fallback to inexact
                return
            }
        }

        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
            putExtra("TODO_ID", todo.id)
            putExtra("TODO_TITLE", todo.title)
        }

        // Use a unique ID for the PendingIntent based on the todo ID
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                todo.scheduledTime,
                pendingIntent
            )
            Log.d("TodoAlarmScheduler", "Scheduled alarm for todo ${todo.id} at ${todo.scheduledTime}")
        } catch (e: SecurityException) {
            Log.e("TodoAlarmScheduler", "SecurityException scheduling alarm", e)
        }
    }

    fun cancel(todo: TodoEntity) {
        val intent = Intent(context, TodoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
