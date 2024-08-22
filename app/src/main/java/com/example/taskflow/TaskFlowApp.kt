package com.example.taskflow

import android.app.Application
import com.example.taskflow.data.database.DatabaseBuilder

class TaskFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseBuilder.getInstance(this)
    }
}