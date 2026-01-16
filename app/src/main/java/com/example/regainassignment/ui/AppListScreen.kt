package com.example.regainassignment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.regainassignment.data.local.AppEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Settings
import androidx.navigation.NavController

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val apps by viewModel.appList.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps: List<AppEntity> = if (searchQuery.isBlank()) {
        apps
    } else {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Regain App Limiter") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { navController.navigate("diagnostics") }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = "Diagnostics"
                            )
                        }
                    }
                )
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(items = filteredApps, key = { it.packageName }) { app ->
                AppItem(
                    app = app,
                    onToggleLimit = { enabled ->
                        viewModel.toggleAppLimit(app.packageName, enabled)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppEntity,
    onToggleLimit: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Usage today: ${formatUsage(app.totalUsageToday)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Switch(
            checked = app.isLimitEnabled,
            onCheckedChange = onToggleLimit
        )
    }
}

fun formatUsage(millis: Long): String {
    val minutes = (millis / 1000) / 60
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
