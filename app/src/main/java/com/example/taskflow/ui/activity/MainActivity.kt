package com.example.taskflow.ui.activity

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Settings
import android.widget.Button
import com.example.taskflow.R
import com.example.taskflow.ui.adapter.TaskAdapter
import com.example.taskflow.data.database.TaskDao
import com.example.taskflow.data.database.TaskDatabase
import com.example.taskflow.data.model.TaskEntity


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskAdapter: TaskAdapter

    // Request permission for notifications
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, proceed with notifications
            } else {
                // Permission denied, show an explanation to the user
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        // Check and request POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted, proceed with notifications
                }
                else -> {
                    // Request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        taskDatabase = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java, "task_database"
        ).fallbackToDestructiveMigration().build()

        taskDao = taskDatabase.taskDao()

        // Instantiate the adapter with the correct parameters
        taskAdapter = TaskAdapter(
            tasks = emptyList(), // Initial empty list
            onTaskCheckedChange = { task, isChecked ->
                // Update the task in the database when the checkbox is checked/unchecked
                lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task.copy(isCompleted = isChecked))
                    withContext(Dispatchers.Main) {
                        loadTasks() // Refresh the list
                    }
                }
            },
            onTaskClick = { task ->
                onTaskClick(task) // Handle task click
            }
        )

        recyclerView.adapter = taskAdapter

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            // Show dialog to add new task
            showAddTaskDialog()
        }

        loadTasks()
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            taskDao.getAllTasks().collect { tasks ->
                taskAdapter.updateTasks(tasks)
            }
        }
    }

    private fun onTaskClick(task: TaskEntity) {
        // Handle task click (e.g., mark as done, show details)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(task.name)
        builder.setMessage(task.notes ?: "No note available")
        builder.setPositiveButton("OK", null)
        builder.show()
        Toast.makeText(this, "Clicked on: ${task.name}", Toast.LENGTH_SHORT).show()
    }

    private fun showAddTaskDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        // Customize dialog window appearance
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background
        }

        val descriptionEditText: EditText = view.findViewById(R.id.descriptionEditText)
        val notesEditText: EditText = view.findViewById(R.id.notesEditText)
        val calendarIcon: ImageView = view.findViewById(R.id.calendarIcon)
        val clockIcon: ImageView = view.findViewById(R.id.clockIcon)
        val reminderIcon: ImageView = view.findViewById(R.id.reminderIcon)
        val saveButton: Button = view.findViewById(R.id.btnSaveTask)

        var selectedDate: Long? = null
        var selectedTime: Long? = null
        var earlyReminderTime: Long? = null

        calendarIcon.setOnClickListener {
            // Show Date Picker
            showDatePickerDialog { dateInMillis ->
                selectedDate = dateInMillis
            }
        }

        clockIcon.setOnClickListener {
            // Show Time Picker
            showTimePickerDialog { timeInMillis ->
                selectedTime = timeInMillis
            }
        }

        reminderIcon.setOnClickListener {
            // Show Early Reminder Picker
            showEarlyReminderDialog { reminderInMillis ->
                earlyReminderTime = reminderInMillis
            }
        }

        saveButton.setOnClickListener {
            val description = descriptionEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()

            if (description.isEmpty()) {
                Toast.makeText(this, "Task name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val task = TaskEntity(
                name = description,
                notes = notes,
                reminderTime = selectedTime,
                earlyReminderTime = earlyReminderTime,
                dueDate = selectedDate,
                isCompleted = false
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val taskId: Long = taskDao.insertTask(task) // Insert and get row ID as Long
                    task.id = taskId.toInt() // Convert the Long to Int
                    withContext(Dispatchers.Main) {
                        loadTasks()
                        scheduleNotification(task)
                        dialog.dismiss() // Dismiss the dialog after saving
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to save task", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }




    private fun showDatePickerDialog(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.timeInMillis)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(onTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            onTimeSelected(calendar.timeInMillis)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        timePickerDialog.show()
    }

    private fun showEarlyReminderDialog(onReminderSelected: (Long) -> Unit) {
        val options = arrayOf("10 minutes before", "30 minutes before", "1 hour before", "Custom")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Early Reminder")
        builder.setItems(options) { _, which ->
            val currentTime = System.currentTimeMillis()
            val reminderTime = when (which) {
                0 -> currentTime - 10 * 60 * 1000  // 10 minutes before
                1 -> currentTime - 30 * 60 * 1000  // 30 minutes before
                2 -> currentTime - 60 * 60 * 1000  // 1 hour before
                else -> {
                    // For custom, show a time picker
                    showTimePickerDialog { customTime ->
                        onReminderSelected(customTime)
                    }
                    return@setItems
                }
            }
            onReminderSelected(reminderTime)
        }
        builder.show()
    }

    private fun getDueDate(datePicker: DatePicker): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, datePicker.year)
            set(Calendar.MONTH, datePicker.month)
            set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getReminderTime(timePicker: TimePicker): Long {
        val hour = timePicker.hour
        val minute = timePicker.minute
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return calendar.timeInMillis
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val channel = NotificationChannel("taskflow_channel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun scheduleNotification(task: TaskEntity) {
        val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            putExtra("task_name", task.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id, // Use task ID as the request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Schedule the main reminder
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            task.reminderTime ?: System.currentTimeMillis(),
            pendingIntent
        )
        // Schedule the early reminder if available
        task.earlyReminderTime?.let {
            val earlyIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                putExtra("task_name", task.name + " (Early Reminder)")
            }
            val earlyPendingIntent = PendingIntent.getBroadcast(
                this,
                task.id + 1000,  // Different request code for early reminder
                earlyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                it,
                earlyPendingIntent
            )
        }
    }
}
