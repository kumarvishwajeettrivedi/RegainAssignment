package com.example.regainassignment.ui.onboarding

data class Goal(
    val id: Int,
    val title: String,
    val emoji: String,
    val description: String
)

val GOALS = listOf(
    Goal(1, "Learn & Grow", "📚", "Focus on education and personal development"),
    Goal(2, "Build Discipline", "💪", "Break bad habits and build good ones"),
    Goal(3, "Boost Productivity", "⚡", "Get more done in less time"),
    Goal(4, "Find Balance", "🧘", "Reduce screen time and find peace"),
    Goal(5, "Stay Focused", "🎯", "Deep work and concentration mode"),
    Goal(6, "Sleep Better", "😴", "Improve sleep quality and schedule")
)

data class Character(
    val id: Int,
    val emoji: String,
    val name: String,
    val description: String
)

val CHARACTERS = listOf(
    Character(1, "🦝", "Togo the Raccoon", "Always focused and determined"),
    Character(2, "🐱", "Luna the Cat", "Calm, collected, and mindful"),
    Character(3, "🐶", "Buddy the Dog", "Loyal companion for your goals"),
    Character(4, "🐻", "Bruno the Bear", "Strong and disciplined"),
    Character(5, "🦊", "Foxy the Fox", "Smart and strategic thinker"),
    Character(6, "🐼", "Zen the Panda", "Peaceful and balanced")
)
