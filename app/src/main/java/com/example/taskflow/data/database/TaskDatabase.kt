package com.example.taskflow.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.taskflow.data.model.TaskEntity

@Database(entities = [TaskEntity::class], version = 8, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}