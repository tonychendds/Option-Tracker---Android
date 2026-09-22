package com.optiontracker.app.data.local

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate

fun PositionEntity.toDomain(): Position = Position(
    id = id,
    ticker = ticker,
    side = OptionSide.valueOf(side),
    type = OptionType.valueOf(optionType),
    strikeCents = strikeCents,
    expiry = LocalDate.ofEpochDay(expiryEpochDay),
    contracts = contracts,
    entryPremiumCents = entryPremiumCents,
    entryFeesCents = entryFeesCents,
    openedOn = LocalDate.ofEpochDay(openedEpochDay),
    notes = notes,
    status = PositionStatus.valueOf(status),
    exitPremiumCents = exitPremiumCents,
    exitFeesCents = exitFeesCents,
    closedOn = exitEpochDay?.let(LocalDate::ofEpochDay),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun Position.toEntity(): PositionEntity = PositionEntity(
    id = id,
    ticker = ticker,
    side = side.name,
    optionType = type.name,
    strikeCents = strikeCents,
    expiryEpochDay = expiry.toEpochDay(),
    contracts = contracts,
    entryPremiumCents = entryPremiumCents,
    entryFeesCents = entryFeesCents,
    openedEpochDay = openedOn.toEpochDay(),
    notes = notes,
    status = status.name,
    exitPremiumCents = exitPremiumCents,
    exitFeesCents = exitFeesCents,
    exitEpochDay = closedOn?.toEpochDay(),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
