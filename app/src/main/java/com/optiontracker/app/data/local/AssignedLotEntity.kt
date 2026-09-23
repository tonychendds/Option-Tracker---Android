package com.optiontracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assigned_lots")
data class AssignedLotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val costBasisCents: Long,
    val shares: Int,
    val assignedEpochDay: Long,
    val sourcePositionId: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
