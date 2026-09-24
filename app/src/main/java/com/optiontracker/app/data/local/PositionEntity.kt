package com.optiontracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val side: String,
    val optionType: String,
    val strikeCents: Long,
    val expiryEpochDay: Long,
    val contracts: Int,
    val entryPremiumCents: Long,
    val entryFeesCents: Long,
    val openedEpochDay: Long,
    val notes: String,
    val status: String,
    val exitPremiumCents: Long?,
    val exitFeesCents: Long?,
    val exitEpochDay: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val account: String = "",
    val realizedOverrideCents: Long? = null,
)
