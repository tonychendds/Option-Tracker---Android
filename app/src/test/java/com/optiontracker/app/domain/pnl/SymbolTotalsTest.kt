package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolTotalsTest {
    @Test
    fun rollsUpOverridesAndSortsLargestFirst() {
        val rows = symbolTotals(
            positions = listOf(
                closed("NVDA", closedOn = LocalDate.of(2026, 3, 1), override = 84_000),
                closed("NVDA", closedOn = LocalDate.of(2026, 4, 1), exit = 100, premium = 300),
                closed("AMD", closedOn = LocalDate.of(2026, 5, 1), override = -20_000),
                closed("TSLA", closedOn = LocalDate.of(2026, 1, 1), exit = 400, premium = 100),
                closed("OPEN", closedOn = null, status = PositionStatus.OPEN),
            ),
            year = 2026,
        )
        assertEquals(listOf("NVDA", "TSLA", "AMD"), rows.map { it.ticker })
        val nvda = rows.first()
        assertEquals(84_000L + realizedBuy(premium = 300, exit = 100), nvda.totalCents)
        assertEquals(2, nvda.tradeCount)
        assertEquals(1, nvda.winningCount)
        assertEquals(1, nvda.losingCount)
        assertEquals(0.5, nvda.hitRate)
    }

    @Test
    fun ytdUsesCloseYearAndAllIncludesOtherYears() {
        val positions = listOf(
            closed("NVDA", closedOn = LocalDate.of(2026, 9, 1), override = 100),
            closed("AMD", closedOn = LocalDate.of(2025, 12, 1), override = 500),
        )
        assertEquals(listOf("NVDA"), symbolTotals(positions, year = 2026).map { it.ticker })
        assertEquals(setOf("NVDA", "AMD"), symbolTotals(positions, year = null).map { it.ticker }.toSet())
        assertEquals(600L, symbolTotals(positions, year = null).sumOf { it.totalCents })
    }

    @Test
    fun nameSortAndZeroIsNotAWin() {
        val flat = closed("ZZZ", closedOn = LocalDate.of(2026, 1, 1), exit = 100, premium = 100)
        val rows = symbolTotals(listOf(flat, closed("AAA", closedOn = LocalDate.of(2026, 1, 2), override = 1)), year = 2026, sort = SymbolSort.NAME)
        assertEquals(listOf("AAA", "ZZZ"), rows.map { it.ticker })
        assertEquals(0, rows.last().winningCount)
        assertEquals(0, rows.last().losingCount)
        assertTrue(tradesForSymbol(listOf(flat), "zzz", 2026).size == 1)
        assertTrue(tradesForSymbol(listOf(flat), "zzz", 2025).isEmpty())
    }
}

private fun realizedBuy(premium: Long, exit: Long): Long =
    OptionPnl.realizedPnlCents(OptionSide.BUY, 1, premium, 0, exit, 0)

private fun closed(
    ticker: String,
    closedOn: LocalDate?,
    premium: Long = 100,
    exit: Long = 100,
    override: Long? = null,
    status: PositionStatus = PositionStatus.CLOSED,
) = Position(
    id = 0,
    ticker = ticker,
    side = OptionSide.BUY,
    type = OptionType.CALL,
    strikeCents = 10_000,
    expiry = LocalDate.of(2026, 12, 18),
    contracts = 1,
    entryPremiumCents = premium,
    entryFeesCents = 0,
    openedOn = LocalDate.of(2026, 1, 1),
    notes = "",
    status = status,
    exitPremiumCents = if (status == PositionStatus.CLOSED) exit else null,
    exitFeesCents = if (status == PositionStatus.CLOSED) 0 else null,
    closedOn = closedOn,
    createdAtEpochMillis = 0,
    updatedAtEpochMillis = 0,
    realizedOverrideCents = override,
)
