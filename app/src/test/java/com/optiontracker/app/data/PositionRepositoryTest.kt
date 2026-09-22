package com.optiontracker.app.data

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.realizedPnlCents
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionRepositoryTest {
    private val repo = PositionRepository(FakePositionDao(), nowMillis = { 1_700_000_000_000 })

    @Test
    fun createReadUpdateDelete() = runBlocking {
        val id = repo.save(draft(ticker = "spy", strikeCents = 50_000, expiry = LocalDate.of(2026, 12, 18)))
        val created = repo.getPosition(id)!!
        assertEquals("SPY", created.ticker)
        assertEquals(PositionStatus.OPEN, created.status)
        assertEquals(1_700_000_000_000, created.createdAtEpochMillis)
        assertEquals(listOf(id), repo.observeOpenPositions().first().map { it.id })

        repo.save(created.copy(strikeCents = 51_000, notes = " adjusted "))
        val updated = repo.getPosition(id)!!
        assertEquals(51_000L, updated.strikeCents)
        assertEquals("adjusted", updated.notes)
        assertEquals(created.createdAtEpochMillis, updated.createdAtEpochMillis)

        repo.delete(id)
        assertNull(repo.getPosition(id))
        assertTrue(repo.observeOpenPositions().first().isEmpty())
    }

    @Test
    fun openPositionsSortByExpiry() = runBlocking {
        val later = repo.save(draft(ticker = "MSFT", expiry = LocalDate.of(2026, 11, 20)))
        val sooner = repo.save(draft(ticker = "AAPL", expiry = LocalDate.of(2026, 10, 16)))
        val sameDay = repo.save(draft(ticker = "AMD", expiry = LocalDate.of(2026, 10, 16)))
        assertEquals(listOf(sooner, sameDay, later), repo.observeOpenPositions().first().map { it.id })
    }

    @Test
    fun closeMovesTradeAndStoresRealizedInputs() = runBlocking {
        val id = repo.save(
            draft(
                ticker = "AAPL",
                side = OptionSide.BUY,
                contracts = 1,
                premium = 250,
                fees = 100,
            ),
        )
        val outcome = repo.closePosition(
            id = id,
            exitPremiumCents = 400,
            exitFeesCents = 100,
            closedOn = LocalDate.of(2026, 9, 10),
        )
        assertEquals(CloseOutcome.Closed, outcome)
        assertTrue(repo.observeOpenPositions().first().isEmpty())
        val closed = repo.observeClosedPositions().first().single()
        assertEquals(PositionStatus.CLOSED, closed.status)
        assertEquals(400L, closed.exitPremiumCents)
        assertEquals(LocalDate.of(2026, 9, 10), closed.closedOn)
        assertEquals(14_800L, closed.realizedPnlCents())
        assertEquals(CloseOutcome.NotOpen, repo.closePosition(id, 100, 0, LocalDate.of(2026, 9, 11)))
    }

    @Test
    fun missingCloseAndInvalidSave() = runBlocking {
        assertEquals(
            CloseOutcome.NotFound,
            repo.closePosition(99, 100, 0, LocalDate.of(2026, 9, 1)),
        )
        var threw = false
        try {
            repo.save(draft(ticker = "bad ticker"))
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    private fun draft(
        ticker: String,
        strikeCents: Long = 20_000,
        expiry: LocalDate = LocalDate.of(2026, 10, 16),
        side: OptionSide = OptionSide.BUY,
        contracts: Int = 1,
        premium: Long = 150,
        fees: Long = 0,
    ) = Position(
        id = 0,
        ticker = ticker,
        side = side,
        type = OptionType.CALL,
        strikeCents = strikeCents,
        expiry = expiry,
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
}
