package com.example.taskflow.ui.activity

import android.annotation.SuppressLint
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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.taskflow.R
import com.example.taskflow.data.database.TaskDatabase
import com.example.taskflow.data.model.TaskEntity
import com.example.taskflow.ui.adapter.TaskAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        // Handle button clicks
        val btnToday: Button = findViewById(R.id.btn_today)
        val btnLists: Button = findViewById(R.id.btn_lists)
        val btnSettings: Button = findViewById(R.id.btn_settings)

        btnToday.setOnClickListener {
            // Handle "Today" button click (you can add the functionality here if needed)
        }

        btnLists.setOnClickListener {
            // Navigate to ActivityLists
            val intent = Intent(this, ActivityLists::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            // Navigate to ActivitySettings
            val intent = Intent(this, ActivitySettings::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        taskDatabase = TaskDatabase.getDatabase(applicationContext)

        taskAdapter = TaskAdapter(
            tasks = emptyList(), // Initial empty list
            onTaskCheckedChange = { task, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    taskDatabase.taskDao().updateTask(task.copy(isCompleted = isChecked))
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
            taskDatabase.taskDao().getAllTasks().collect { tasks ->
                taskAdapter.updateTasks(tasks)
            }
        }
    }

    private fun onTaskClick(task: TaskEntity) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(task.name)
        builder.setMessage(task.notes ?: "No note available")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun showAddTaskDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background
        }

        val descriptionEditText: EditText = view.findViewById(R.id.descriptionEditText)
        val notesEditText: EditText = view.findViewById(R.id.notesEditText)
        val calendarIcon: ImageView = view.findViewById(R.id.calendarIcon)
        val clockIcon: ImageView = view.findViewById(R.id.clockIcon)
        val reminderIcon: ImageView = view.findViewById(R.id.reminderIcon)
        val saveButton: Button = view.findViewById(R.id.btnSaveTask)
        val spinnerChooseList: Spinner = view.findViewById(R.id.spinnerChooseList)

        val taskLists = listOf("Personal", "Work", "Shopping") // Dummy task lists
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taskLists)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChooseList.adapter = adapter

        var selectedDate: Long? = null
        var selectedTime: Long? = null
        var earlyReminderTime: Long? = null

        calendarIcon.setOnClickListener {
            showDatePickerDialog { dateInMillis ->
                selectedDate = dateInMillis
            }
        }

        clockIcon.setOnClickListener {
            showTimePickerDialog { timeInMillis ->
                selectedTime = timeInMillis
            }
        }

        reminderIcon.setOnClickListener {
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

            val selectedList = spinnerChooseList.selectedItem.toString() // Get selected list from Spinner

            val task = TaskEntity(
                name = description,
                notes = notes,
                reminderTime = selectedTime,
                earlyReminderTime = earlyReminderTime,
                dueDate = selectedDate,
                isCompleted = false,
                taskList = selectedList // Save the selected list to the task entity
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val taskId: Long = taskDatabase.taskDao().insertTask(task) // Insert task
                    task.id = taskId.toInt() // Convert Long to Int for room entity
                    withContext(Dispatchers.Main) {
                        loadTasks() // Refresh task list
                        scheduleNotification(task) // Schedule notification
                        dialog.dismiss() // Close the dialog
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error saving task", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDatePickerDialog(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }.timeInMillis
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(onTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.timeInMillis
                onTimeSelected(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun showEarlyReminderDialog(onReminderSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val earlyReminderTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.timeInMillis
                onReminderSelected(earlyReminderTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun scheduleNotification(task: TaskEntity) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderBroadcastReceiver::class.java)
        intent.putExtra("task_id", task.id)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
        task.reminderTime?.let {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                it,
                pendingIntent
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TaskFlow Notifications"
            val descriptionText = "Channel for TaskFlow notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("task_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
