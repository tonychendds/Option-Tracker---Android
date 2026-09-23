package com.optiontracker.app.domain.model

import java.time.LocalDate

/**
 * Shares bought because a short put was assigned.
 * Cost basis is the put's strike per share. Stock P/L stays on this lot.
 */
data class AssignedLot(
    val id: Long,
    val ticker: String,
    val costBasisCents: Long,
    val shares: Int,
    val assignedOn: LocalDate,
    val sourcePositionId: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
