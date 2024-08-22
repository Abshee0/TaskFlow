package com.example.taskflow.data.database

import android.content.Context
import androidx.room.Room

object DatabaseBuilder {
    private var INSTANCE: TaskDatabase? = null

    fun getInstance(context: Context): TaskDatabase {
        if (INSTANCE == null) {
            synchronized(TaskDatabase::class) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                ).build()
            }
        }
        return INSTANCE!!
    }
}
