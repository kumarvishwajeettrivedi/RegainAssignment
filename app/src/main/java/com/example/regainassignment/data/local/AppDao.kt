package com.example.regainassignment.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_usage")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppEntity?

    @Query("SELECT * FROM app_usage WHERE totalUsageToday > 0 OR isLimitEnabled = 1")
    fun getTrackedApps(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE app_usage SET totalUsageToday = 0, sessionExpiryTime = 0")
    suspend fun resetDailyUsage()

    @Query("UPDATE app_usage SET totalUsageToday = :usage, lastInteractTime = :timestamp WHERE packageName = :packageName")
    suspend fun updateUsage(packageName: String, usage: Long, timestamp: Long)

    @Query("UPDATE app_usage SET sessionExpiryTime = :expiryTime WHERE packageName = :packageName")
    suspend fun updateSessionExpiry(packageName: String, expiryTime: Long)

    @Query("UPDATE app_usage SET isLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateLimitStatus(packageName: String, enabled: Boolean)
    
    @Query("UPDATE app_usage SET isTemporarilyBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateTemporaryBlock(packageName: String, isBlocked: Boolean)
    
    @Query("UPDATE app_usage SET sessionStartTime = :startTime, selectedSessionDuration = :duration, sessionExpiryTime = :expiryTime WHERE packageName = :packageName")
    suspend fun updateSessionInfo(packageName: String, startTime: Long, duration: Long, expiryTime: Long)
    
    @Query("UPDATE app_usage SET remainingSessionTime = :remainingTime WHERE packageName = :packageName")
    suspend fun updateRemainingTime(packageName: String, remainingTime: Long)
    
    @Query("UPDATE app_usage SET isTemporarilyBlocked = 0")
    suspend fun clearAllTemporaryBlocks()
}
