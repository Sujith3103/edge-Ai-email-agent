package com.example.smartgmail.database

import androidx.room.Database
import androidx.room.RoomDatabase

import com.example.smartgmail.database.dao.EmailDao
import com.example.smartgmail.database.dao.EmailAnalysisDao
import com.example.smartgmail.database.dao.TaskDao
import com.example.smartgmail.database.dao.EventDao
import com.example.smartgmail.database.dao.KnowledgeDao

import com.example.smartgmail.database.entity.EmailEntity
import com.example.smartgmail.database.entity.EmailAnalysisEntity
import com.example.smartgmail.database.entity.TaskEntity
import com.example.smartgmail.database.entity.EventEntity
import com.example.smartgmail.database.entity.KnowledgeEntity


@Database(
    entities = [
        EmailEntity::class,
        EmailAnalysisEntity::class,
        TaskEntity::class,
        EventEntity::class,
        KnowledgeEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emailDao(): EmailDao

    abstract fun emailAnalysisDao(): EmailAnalysisDao

    abstract fun taskDao(): TaskDao

    abstract fun eventDao(): EventDao

    abstract fun knowledgeDao(): KnowledgeDao
}
