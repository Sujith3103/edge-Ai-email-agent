package com.example.smartgmail.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartgmail.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(
        task: TaskEntity
    )

    @Insert
    suspend fun insertTasks(
        tasks: List<TaskEntity>
    )

    @Query(
        "SELECT * FROM tasks WHERE completed = 0"
    )
    fun getIncompleteTasks(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE completed = 1"
    )
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Update
    suspend fun updateTask(
        task: TaskEntity
    )
}