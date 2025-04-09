package com.example.taskflow.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.taskflow.R

class ActivitySettings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // Set your layout

        val switchDarkMode: SwitchCompat = findViewById(R.id.switch_dark_mode)
        val switchNotifications: SwitchCompat = findViewById(R.id.switch_notifications)

        // Retrieve saved preferences
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isDarkModeEnabled = sharedPreferences.getBoolean("dark_mode", false) // Default to false
        val areNotificationsEnabled = sharedPreferences.getBoolean("notifications", true) // Default to true

        // Initialize switches based on saved preferences
        switchDarkMode.isChecked = isDarkModeEnabled
        switchNotifications.isChecked = areNotificationsEnabled

        // Set listeners to handle changes
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Save Dark Mode preference
            val editor = sharedPreferences.edit()
            editor.putBoolean("dark_mode", isChecked)
            editor.apply()

            // Handle Dark Mode change
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Dark Mode Enabled", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Dark Mode Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Save Notifications preference
            val editor = sharedPreferences.edit()
            editor.putBoolean("notifications", isChecked)
            editor.apply()

            // Handle Notifications setting change
            if (isChecked) {
                Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                // Implement actual notification enabling logic
            } else {
                Toast.makeText(this, "Notifications Disabled", Toast.LENGTH_SHORT).show()
                // Implement actual notification disabling logic
            }
        }
    }
}
