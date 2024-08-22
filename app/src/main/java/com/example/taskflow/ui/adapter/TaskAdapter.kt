package com.example.taskflow.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private var tasks: List<TaskEntity>,
    private val onTaskCheckedChange: (TaskEntity, Boolean) -> Unit,
    private val onTaskClick: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.task_name)
        private val taskTimeTextView: TextView = itemView.findViewById(R.id.task_time)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(task: TaskEntity) {
            taskNameTextView.text = task.name

            if (task.reminderTime != null) {
                val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val formattedTime = dateFormat.format(task.reminderTime)
                taskTimeTextView.text = formattedTime
            } else {
                taskTimeTextView.text = ""
            }

            checkBox.isChecked = task.isCompleted

            itemView.setOnClickListener { onTaskClick(task) }

            taskNameTextView.paintFlags = if (task.isCompleted) {
                taskNameTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                taskNameTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedChange(task, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<TaskEntity>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
