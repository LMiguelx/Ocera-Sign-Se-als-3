package com.example.signrecognition3.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "gestures")
data class GestureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gesture: String,
    val timestamp: Long = System.currentTimeMillis()
)