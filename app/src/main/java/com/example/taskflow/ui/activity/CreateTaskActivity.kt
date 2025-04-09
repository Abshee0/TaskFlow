package com.example.taskflow.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.R

class CreateTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_task) // Set your layout for creating a task
    }
}
