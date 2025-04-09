package com.example.taskflow.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ActivityLists : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists) // Set your layout

        val fabAddList: FloatingActionButton = findViewById(R.id.fab_add_list)
        fabAddList.setOnClickListener {
            // Navigate to CreateTaskActivity
            val intent = Intent(this, CreateTaskActivity::class.java)
            startActivity(intent)
        }
    }
}
