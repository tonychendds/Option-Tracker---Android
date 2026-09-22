package com.optiontracker.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions WHERE status = 'OPEN'")
    fun observeOpen(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE status = 'CLOSED'")
    fun observeClosed(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE id = :id")
    fun observeById(id: Long): Flow<PositionEntity?>

    @Query("SELECT * FROM positions WHERE id = :id")
    suspend fun getById(id: Long): PositionEntity?

    @Insert
    suspend fun insert(entity: PositionEntity): Long

    @Update
    suspend fun update(entity: PositionEntity)

    @Query("DELETE FROM positions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
