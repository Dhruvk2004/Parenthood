package com.example.boardingscreen.data.model

data class AppUsageLimit(
    val appName: String,
    val appPackageName: String,
    val dailyLimitMinutes: Int,
    val currentUsageMinutes: Int
)
