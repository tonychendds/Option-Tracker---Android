package com.optiontracker.app.data.local

import com.optiontracker.app.domain.model.AssignedLot
import java.time.LocalDate

fun AssignedLotEntity.toDomain(): AssignedLot = AssignedLot(
    id = id,
    ticker = ticker,
    costBasisCents = costBasisCents,
    shares = shares,
    assignedOn = LocalDate.ofEpochDay(assignedEpochDay),
    sourcePositionId = sourcePositionId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun AssignedLot.toEntity(): AssignedLotEntity = AssignedLotEntity(
    id = id,
    ticker = ticker,
    costBasisCents = costBasisCents,
    shares = shares,
    assignedEpochDay = assignedOn.toEpochDay(),
    sourcePositionId = sourcePositionId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
