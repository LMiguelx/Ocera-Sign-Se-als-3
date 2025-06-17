package com.example.signrecognition3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GestureEntity::class], version = 1)
abstract class GestureDatabase : RoomDatabase() {
    abstract fun gestureDao(): GestureDao

    companion object {
        @Volatile
        private var INSTANCE: GestureDatabase? = null

        fun getInstance(context: Context): GestureDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GestureDatabase::class.java,
                    "gesture_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}