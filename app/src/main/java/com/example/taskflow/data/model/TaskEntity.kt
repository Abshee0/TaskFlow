package com.example.taskflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    val name: String,
    val notes: String,
    val reminderTime: Long?,
    val earlyReminderTime: Long?,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val taskList: String
)
