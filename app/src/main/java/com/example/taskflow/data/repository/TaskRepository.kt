package com.example.taskflow.data.repository

import android.content.Context
import com.example.taskflow.data.database.DatabaseBuilder
import com.example.taskflow.data.database.TaskDao
import com.example.taskflow.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(context: Context) {
    private val taskDao: TaskDao = DatabaseBuilder.getInstance(context).taskDao()

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity) = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(taskId: Int) = taskDao.deleteTask(taskId)
}