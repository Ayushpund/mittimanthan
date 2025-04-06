package com.example.pict.models

data class ActivityItem(
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: ActivityType
)
