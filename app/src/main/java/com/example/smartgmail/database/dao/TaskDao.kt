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

    @Query("DELETE FROM tasks WHERE emailId = :emailId")
    suspend fun deleteTasksByEmailId(emailId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("""
        SELECT * FROM tasks 
        WHERE completed = 0 
        AND (description LIKE '%' || :query || '%' OR dueDate LIKE '%' || :query || '%')
        LIMIT :limit
    """)
    suspend fun searchTasks(query: String, limit: Int = 5): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY dueDate ASC, dueTime ASC LIMIT :limit")
    suspend fun getUpcomingTasks(limit: Int = 5): List<TaskEntity>
}
