package com.optiontracker.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignedLotDao {
    @Query("SELECT * FROM assigned_lots")
    fun observeAll(): Flow<List<AssignedLotEntity>>

    @Query("SELECT * FROM assigned_lots WHERE id = :id")
    suspend fun getById(id: Long): AssignedLotEntity?

    @Insert
    suspend fun insert(entity: AssignedLotEntity): Long

    @Update
    suspend fun update(entity: AssignedLotEntity)

    @Query("DELETE FROM assigned_lots WHERE id = :id")
    suspend fun deleteById(id: Long)
}
