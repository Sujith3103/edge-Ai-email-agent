package com.example.smartgmail.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val incompleteTasks: StateFlow<List<TaskEntity>> =
        taskRepository.getIncompleteTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val completedTasks: StateFlow<List<TaskEntity>> =
        taskRepository.getCompletedTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun updateTaskCompletion(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(completed = completed))
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TasksViewModel(taskRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
