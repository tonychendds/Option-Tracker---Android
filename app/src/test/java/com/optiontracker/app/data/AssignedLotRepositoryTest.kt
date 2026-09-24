package com.optiontracker.app.data

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.AssignedPnl
import com.optiontracker.app.domain.pnl.realizedPnlCents
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignedLotRepositoryTest {
    private val positionDao = FakePositionDao()
    private val options = PositionRepository(positionDao, nowMillis = { 10L })
    private val assigned = AssignedLotRepository(FakeAssignedLotDao(), positionDao, nowMillis = { 20L })

    @Test
    fun assignShortPutKeepsCreditAndCreatesSharesAtTheStrike() = runBlocking {
        val id = options.save(put(contracts = 2, premium = 150, fees = 65))
        val outcome = assigned.assignShortPut(id, LocalDate.of(2026, 9, 22))
        val lotId = (outcome as AssignOutcome.Assigned).lotId
        val closed = options.getPosition(id)!!
        assertEquals(PositionStatus.CLOSED, closed.status)
        assertEquals(0L, closed.exitPremiumCents)
        assertEquals(0L, closed.exitFeesCents)
        assertEquals(LocalDate.of(2026, 9, 22), closed.closedOn)
        assertEquals(29_935L, closed.realizedPnlCents())

        val lot = assigned.get(lotId)!!
        assertEquals("NVDA", lot.ticker)
        assertEquals(18_000L, lot.costBasisCents)
        assertEquals(200, lot.shares)
        assertEquals(id, lot.sourcePositionId)
        assertEquals(LocalDate.of(2026, 9, 22), lot.assignedOn)
        assertEquals(listOf(lotId), assigned.observeLots().first().map { it.id })
    }

    @Test
    fun unrealizedUsesQuoteMinusStrikeTimesShares() {
        assertEquals(977_400L, AssignedPnl.unrealizedCents(22_887, 18_000, 200))
        assertEquals(977_400L, AssignedPnl.unrealizedFromQuote("$228.87", 18_000, 200))
        assertNull(AssignedPnl.unrealizedFromQuote("—", 18_000, 200))
        assertNull(AssignedPnl.unrealizedFromQuote("", 18_000, 200))
    }

    @Test
    fun onlyOpenShortPutsCanBeAssigned() = runBlocking {
        val call = options.save(put().copy(type = OptionType.CALL))
        val longPut = options.save(put().copy(side = OptionSide.BUY))
        assertEquals(AssignOutcome.NotShortPut, assigned.assignShortPut(call, LocalDate.of(2026, 9, 22)))
        assertEquals(AssignOutcome.NotShortPut, assigned.assignShortPut(longPut, LocalDate.of(2026, 9, 22)))
        assertTrue(options.observeOpenPositions().first().size == 2)
        assertTrue(assigned.observeLots().first().isEmpty())
    }

    @Test
    fun manualLotCanBeEditedAndDeletedWithoutReopeningTheOption() = runBlocking {
        val optionId = options.save(put())
        assigned.assignShortPut(optionId, LocalDate.of(2026, 9, 22))
        val manualId = assigned.save(
            com.optiontracker.app.domain.model.AssignedLot(
                id = 0L,
                ticker = "amd",
                costBasisCents = 15_000L,
                shares = 100,
                assignedOn = LocalDate.of(2026, 8, 1),
                sourcePositionId = 99L,
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
            ),
        )
        val manual = assigned.get(manualId)!!
        assertEquals("AMD", manual.ticker)
        assertNull(manual.sourcePositionId)
        assigned.save(manual.copy(costBasisCents = 14_000L, shares = 50, assignedOn = LocalDate.of(2026, 8, 2)))
        val edited = assigned.get(manualId)!!
        assertEquals(14_000L, edited.costBasisCents)
        assertEquals(50, edited.shares)
        assertEquals(LocalDate.of(2026, 8, 2), edited.assignedOn)
        assigned.delete(manualId)
        assertNull(assigned.get(manualId))
        assertEquals(PositionStatus.CLOSED, options.getPosition(optionId)!!.status)
        assertEquals(1, assigned.observeLots().first().size)
    }
}

private fun put(
    contracts: Int = 1,
    premium: Long = 150,
    fees: Long = 0,
) = Position(
    id = 0,
    ticker = "nvda",
    side = OptionSide.SELL,
    type = OptionType.PUT,
    strikeCents = 18_000,
    expiry = LocalDate.of(2026, 10, 16),
    contracts = contracts,
    entryPremiumCents = premium,
    entryFeesCents = fees,
    openedOn = LocalDate.of(2026, 9, 1),
    notes = "",
    status = PositionStatus.OPEN,
    exitPremiumCents = null,
    exitFeesCents = null,
    closedOn = null,
    createdAtEpochMillis = 0,
    updatedAtEpochMillis = 0,
)
