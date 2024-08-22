package com.example.taskflow.data.model

data class Task(
    val id: Long,
    val description: String,
    val isDone: Boolean = false,
    val reminderTime: Long? = null
)