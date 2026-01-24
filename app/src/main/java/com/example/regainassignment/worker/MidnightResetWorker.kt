package com.example.regainassignment.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.regainassignment.data.repository.UsageRepository
import com.example.regainassignment.data.repository.TodoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MidnightResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: UsageRepository,
    private val todoRepository: TodoRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.resetDailyUsage()
            
            // Cleanup: Delete completed todos older than 48 hours
            val threshold = System.currentTimeMillis() - (48 * 3600 * 1000L)
            todoRepository.deleteOldCompletedTodos(threshold)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
