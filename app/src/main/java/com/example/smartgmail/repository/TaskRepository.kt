package com.example.smartgmail.repository

import com.example.smartgmail.database.dao.TaskDao
import com.example.smartgmail.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getIncompleteTasks(): Flow<List<TaskEntity>> = taskDao.getIncompleteTasks()
    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(taskId: Long) = taskDao.deleteTask(taskId)
}
