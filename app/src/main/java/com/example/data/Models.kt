package com.example.data

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val registrationDate: String = ""
) : Serializable

data class PeriodLog(
    val id: String = "",
    val userId: String = "",
    val startDate: String = "", // yyyy-MM-dd
    val endDate: String = "",   // yyyy-MM-dd
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val symptoms: List<String> = emptyList()
) : Serializable

data class FeedbackItem(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val timestamp: Long = 0L
) : Serializable

data class AdminNotification(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val target: String = "All" // "All" or a specific user's email
) : Serializable

data class YogaPose(
    val title: String,
    val description: String,
    val duration: String,
    val benefits: String,
    val steps: List<String>
)
