package com.example.regainassignment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info // Fallback if RemoveRedEye fails, but RemoveRedEye should exist if Extended.
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
// If RemoveRedEye needs extended, we use Info which is always there, or we try to find the import.
// RemoveRedEye is in Filled in Core? Check docs or assume Extended.
// Let's use 'Home' or 'Info' temporarily if Eye fails, but let's try 'Eye' or 'Face'.
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.regainassignment.ui.planning.PlanningScreen
import com.example.regainassignment.util.OnboardingPreferences
import com.example.regainassignment.ui.theme.DarkTeal
import com.example.regainassignment.ui.theme.CleanWhite

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object FocusMode : BottomNavItem("focus_mode", Icons.Filled.Face, "Focus")
    object Planning : BottomNavItem("planning", Icons.Filled.List, "Planning")
    object Progress : BottomNavItem("progress", Icons.Filled.Star, "Progress")
}

@Composable
fun MainScreen(
    diagnosticsNavController: NavController,
    onboardingPrefs: OnboardingPreferences
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = hiltViewModel()
    val dueTodosCount by todoViewModel.dueTodosCount.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.FocusMode.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.FocusMode.route) {
                AppListScreen(
                    navController = diagnosticsNavController,
                    onboardingPrefs = onboardingPrefs
                )
            }
            composable(BottomNavItem.Planning.route) {
                PlanningScreen(viewModel = todoViewModel)
            }
            composable(BottomNavItem.Progress.route) {
                // Progress screen placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Progress Screen - Coming Soon")
                }
            }
        }
        
        // Floating bottom navigation
        FloatingBottomNav(
            navController = navController,
            dueTodosCount = dueTodosCount,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FloatingBottomNav(
    navController: NavHostController,
    dueTodosCount: Int,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.FocusMode,
        BottomNavItem.Planning,
        BottomNavItem.Progress
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Floating navigation bar
    Row(
        modifier = modifier
            .padding(horizontal = 48.dp, vertical = 16.dp) // Narroer width
            .height(64.dp)
            .wrapContentWidth() 
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .background(DarkTeal, RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp), // Equal 8dp padding all around (top/bottom is 8dp via calculation)
        horizontalArrangement = Arrangement.spacedBy(12.dp), // Space between icons
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape) // Fix square ripple
                    .background(
                        if (isSelected) CleanWhite else Color.Transparent,
                        CircleShape
                    )
                    .clickable {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) DarkTeal else CleanWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
