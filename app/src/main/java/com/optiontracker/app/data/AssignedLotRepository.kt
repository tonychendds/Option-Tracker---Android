package com.optiontracker.app.data

import com.optiontracker.app.data.local.AssignedLotDao
import com.optiontracker.app.data.local.PositionDao
import com.optiontracker.app.data.local.toDomain
import com.optiontracker.app.data.local.toEntity
import com.optiontracker.app.domain.model.AssignedLot
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.AssignedPnl
import com.optiontracker.app.domain.validation.AssignedLotValidator
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AssignOutcome {
    data class Assigned(val lotId: Long) : AssignOutcome
    data object NotFound : AssignOutcome
    data object NotOpen : AssignOutcome
    data object NotShortPut : AssignOutcome
}

class AssignedLotRepository(
    private val lots: AssignedLotDao,
    private val positions: PositionDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val transact: suspend (suspend () -> Unit) -> Unit = { block -> block() },
) {
    fun observeLots(): Flow<List<AssignedLot>> =
        lots.observeAll().map { rows ->
            rows.map { it.toDomain() }
                .sortedWith(compareByDescending<AssignedLot> { it.assignedOn }.thenBy { it.ticker }.thenByDescending { it.id })
        }

    suspend fun get(id: Long): AssignedLot? = lots.getById(id)?.toDomain()

    suspend fun save(lot: AssignedLot): Long {
        val normalized = lot.copy(ticker = lot.ticker.trim().uppercase(Locale.US))
        val error = AssignedLotValidator.modelError(normalized)
        if (error != null) throw IllegalArgumentException(error)
        val now = nowMillis()
        return if (normalized.id == 0L) {
            lots.insert(
                normalized.copy(
                    sourcePositionId = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ).toEntity(),
            )
        } else {
            val existing = lots.getById(normalized.id) ?: throw IllegalArgumentException("Assignment not found")
            lots.update(
                normalized.copy(
                    sourcePositionId = existing.sourcePositionId,
                    createdAtEpochMillis = existing.createdAtEpochMillis,
                    updatedAtEpochMillis = now,
                ).toEntity(),
            )
            normalized.id
        }
    }

    suspend fun delete(id: Long) {
        lots.deleteById(id)
    }

    /**
     * Closes an open short put at a $0 exit premium so the option keeps its credit,
     * then records the shares at the strike. Stock P/L is not stored on the option.
     */
    suspend fun assignShortPut(positionId: Long, assignedOn: LocalDate): AssignOutcome {
        var outcome: AssignOutcome = AssignOutcome.NotFound
        transact {
            val existing = positions.getById(positionId)
            outcome = when {
                existing == null -> AssignOutcome.NotFound
                existing.status != PositionStatus.OPEN.name -> AssignOutcome.NotOpen
                existing.side != OptionSide.SELL.name || existing.optionType != OptionType.PUT.name ->
                    AssignOutcome.NotShortPut
                else -> {
                    val shares = AssignedPnl.sharesForContracts(existing.contracts)
                    val now = nowMillis()
                    positions.update(
                        existing.copy(
                            status = PositionStatus.CLOSED.name,
                            exitPremiumCents = 0L,
                            exitFeesCents = 0L,
                            exitEpochDay = assignedOn.toEpochDay(),
                            updatedAtEpochMillis = now,
                        ),
                    )
                    val lotId = lots.insert(
                        AssignedLot(
                            id = 0L,
                            ticker = existing.ticker,
                            costBasisCents = existing.strikeCents,
                            shares = shares,
                            assignedOn = assignedOn,
                            sourcePositionId = existing.id,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                        ).toEntity(),
                    )
                    AssignOutcome.Assigned(lotId)
                }
            }
        }
        return outcome
    }
}
