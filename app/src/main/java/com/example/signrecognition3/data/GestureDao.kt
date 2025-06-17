package com.example.signrecognition3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Insert
    suspend fun insertGesture(gesture: GestureEntity)

    @Query("SELECT * FROM gestures ORDER BY timestamp DESC")
    fun getAllGesturesFlow(): Flow<List<GestureEntity>>
}