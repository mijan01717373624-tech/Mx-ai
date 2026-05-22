package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String,
    val description: String,
    val category: String, // "DAILY SCHEDULE", "MEETING", "BRIEF" etc.
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "email_items")
data class EmailItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val subject: String,
    val preview: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCritical: Boolean = false,
    val isRead: Boolean = false
)

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "BIOMETRIC", "ENCRYPTION", "SANDBOX", "PRIVACY"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // "SUCCESS", "SECURE", "PROTECTED"
)
