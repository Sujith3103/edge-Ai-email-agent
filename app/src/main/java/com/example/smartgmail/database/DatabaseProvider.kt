package com.example.smartgmail.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(
        context: Context
    ): AppDatabase {

        return INSTANCE
            ?: synchronized(this) {

                INSTANCE
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "smartgmail.db"
                    )
                        .build()
                        .also {
                            INSTANCE = it
                        }
            }
    }
}