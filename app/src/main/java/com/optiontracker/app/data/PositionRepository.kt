package com.optiontracker.app.data

import com.optiontracker.app.data.local.PositionDao
import com.optiontracker.app.data.local.toDomain
import com.optiontracker.app.data.local.toEntity
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface CloseOutcome {
    data object Closed : CloseOutcome
    data object NotFound : CloseOutcome
    data object NotOpen : CloseOutcome
}

class PositionRepository(
    private val dao: PositionDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val transact: suspend (suspend () -> Unit) -> Unit = { block -> block() },
) {
    fun observeOpenPositions(): Flow<List<Position>> =
        dao.observeOpen().map { rows ->
            rows.map { it.toDomain() }
                .sortedWith(compareBy({ it.expiry }, { it.ticker }, { it.id }))
        }

    fun observeClosedPositions(): Flow<List<Position>> =
        dao.observeClosed().map { rows ->
            rows.map { it.toDomain() }
                .sortedWith(
                    compareByDescending<Position> { it.closedOn ?: LocalDate.MIN }
                        .thenByDescending { it.id },
                )
        }

    fun observePosition(id: Long): Flow<Position?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun getPosition(id: Long): Position? = dao.getById(id)?.toDomain()

    suspend fun save(position: Position): Long {
        val normalized = position.copy(
            ticker = position.ticker.trim().uppercase(Locale.US),
            notes = position.notes.trim(),
        )
        val error = PositionValidator.modelError(normalized)
        if (error != null) throw IllegalArgumentException(error)
        val now = nowMillis()
        return if (normalized.id == 0L) {
            dao.insert(
                normalized.copy(
                    status = PositionStatus.OPEN,
                    exitPremiumCents = null,
                    exitFeesCents = null,
                    closedOn = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ).toEntity(),
            )
        } else {
            val existing = dao.getById(normalized.id)
                ?: throw IllegalArgumentException("Position not found")
            if (existing.status != PositionStatus.OPEN.name) {
                throw IllegalStateException("Closed positions are read-only")
            }
            dao.update(
                normalized.copy(
                    status = PositionStatus.OPEN,
                    exitPremiumCents = null,
                    exitFeesCents = null,
                    closedOn = null,
                    createdAtEpochMillis = existing.createdAtEpochMillis,
                    updatedAtEpochMillis = now,
                ).toEntity(),
            )
            normalized.id
        }
    }

    suspend fun closePosition(
        id: Long,
        exitPremiumCents: Long,
        exitFeesCents: Long,
        closedOn: LocalDate,
    ): CloseOutcome {
        if (exitPremiumCents < 0L || exitFeesCents < 0L) {
            throw IllegalArgumentException("Exit premium and fees cannot be negative")
        }
        val existing = dao.getById(id) ?: return CloseOutcome.NotFound
        if (existing.status != PositionStatus.OPEN.name) return CloseOutcome.NotOpen
        dao.update(
            existing.copy(
                status = PositionStatus.CLOSED.name,
                exitPremiumCents = exitPremiumCents,
                exitFeesCents = exitFeesCents,
                exitEpochDay = closedOn.toEpochDay(),
                updatedAtEpochMillis = nowMillis(),
            ),
        )
        return CloseOutcome.Closed
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun replaceAll(positions: List<Position>) {
        val now = nowMillis()
        val entities = positions.map { position ->
            val normalized = position.copy(
                id = 0L,
                ticker = position.ticker.trim().uppercase(Locale.US),
                account = position.account.trim(),
                notes = position.notes.trim(),
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )
            val error = PositionValidator.modelError(normalized)
            if (error != null) throw IllegalArgumentException(error)
            normalized.toEntity()
        }
        transact {
            dao.deleteAll()
            if (entities.isNotEmpty()) dao.insertAll(entities)
        }
    }
}
