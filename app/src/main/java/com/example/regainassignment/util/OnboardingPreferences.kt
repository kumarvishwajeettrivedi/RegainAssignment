package com.example.regainassignment.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }
    
    fun setOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }
    
    fun saveUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }
    
    fun getUserName(): String {
        return prefs.getString("user_name", "Friend") ?: "Friend"
    }
    
    fun saveGoal(goal: String) {
        prefs.edit().putString("user_goal", goal).apply()
    }
    
    fun getGoal(): String {
        return prefs.getString("user_goal", "Stay Focused") ?: "Stay Focused"
    }
    
    fun saveCharacter(characterId: Int) {
        prefs.edit().putInt("user_character", characterId).apply()
    }
    
    fun getCharacter(): Int {
        return prefs.getInt("user_character", 1)
    }
}
