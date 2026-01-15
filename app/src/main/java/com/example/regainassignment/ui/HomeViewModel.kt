package com.example.regainassignment.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regainassignment.data.local.AppEntity
import com.example.regainassignment.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: UsageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val priorityPackages = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.snapchat.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.linkedin.android",
        "com.discord"
    )

    val appList: StateFlow<List<AppEntity>> = repository.getAllApps()
        .map { list ->
            list.sortedWith(
                compareByDescending<AppEntity> { it.packageName in priorityPackages }
                    .thenByDescending { it.totalUsageToday } // Then by usage
                    .thenBy { it.appName } // Then alphabetical
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshApps()
    }

    private fun refreshApps() {
        viewModelScope.launch {
            repository.refreshApps(context)
        }
    }

    fun toggleAppLimit(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleLimit(packageName, enabled)
        }
    }
}
