package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardAndHistoryTest {
    private val today = LocalDate.of(2026, 9, 22)

    @Test
    fun openSummaryIsNetPremiumWithoutMarks() {
        val summary = buildDashboardSummary(
            open = listOf(longCall(), shortPuts()),
            closed = listOf(closedThisMonth(), closedLastMonth()),
            today = today,
        )
        assertEquals(2, summary.openCount)
        assertEquals(3, summary.openContracts)
        assertEquals(1, summary.callContracts)
        assertEquals(2, summary.putContracts)
        assertEquals(1, summary.longContracts)
        assertEquals(2, summary.shortContracts)
        assertEquals(34_770L, summary.netPremiumCents)
        assertEquals(14_800L, summary.realizedThisMonthCents)
        assertEquals(1, summary.closedThisMonthCount)
    }

    @Test
    fun recentActivityPrefersNewerDates() {
        val summary = buildDashboardSummary(
            open = listOf(longCall()),
            closed = listOf(closedThisMonth()),
            today = today,
            recentLimit = 5,
        )
        assertEquals(ActivityKind.CLOSED, summary.recent.first().kind)
        assertEquals(LocalDate.of(2026, 9, 10), summary.recent.first().date)
    }

    @Test
    fun historyGroupsByMonthAndFiltersTicker() {
        val groups = groupClosedTrades(
            closed = listOf(closedThisMonth(), closedLastMonth()),
            tickerQuery = "aapl",
        )
        assertEquals(1, groups.size)
        assertEquals(YearMonth.of(2026, 9), groups.single().yearMonth)
        assertEquals(14_800L, groups.single().totalPnlCents)
        assertEquals("AAPL", groups.single().trades.single().ticker)
    }

    private fun longCall() = position(
        id = 1,
        ticker = "AAPL",
        side = OptionSide.BUY,
        type = OptionType.CALL,
        contracts = 1,
        premium = 250,
        fees = 100,
        expiry = LocalDate.of(2026, 10, 16),
        opened = LocalDate.of(2026, 9, 1),
    )

    private fun shortPuts() = position(
        id = 2,
        ticker = "SPY",
        side = OptionSide.SELL,
        type = OptionType.PUT,
        contracts = 2,
        premium = 300,
        fees = 130,
        expiry = LocalDate.of(2026, 10, 2),
        opened = LocalDate.of(2026, 9, 2),
    )

    private fun closedThisMonth() = position(
        id = 3,
        ticker = "AAPL",
        side = OptionSide.BUY,
        type = OptionType.CALL,
        contracts = 1,
        premium = 250,
        fees = 100,
        expiry = LocalDate.of(2026, 9, 18),
        opened = LocalDate.of(2026, 9, 1),
        status = PositionStatus.CLOSED,
        exitPremium = 400,
        exitFees = 100,
        closedOn = LocalDate.of(2026, 9, 10),
    )

    private fun closedLastMonth() = position(
        id = 4,
        ticker = "QQQ",
        side = OptionSide.SELL,
        type = OptionType.PUT,
        contracts = 1,
        premium = 100,
        fees = 0,
        expiry = LocalDate.of(2026, 8, 21),
        opened = LocalDate.of(2026, 8, 1),
        status = PositionStatus.CLOSED,
        exitPremium = 40,
        exitFees = 0,
        closedOn = LocalDate.of(2026, 8, 15),
    )

    private fun position(
        id: Long,
        ticker: String,
        side: OptionSide,
        type: OptionType,
        contracts: Int,
        premium: Long,
        fees: Long,
        expiry: LocalDate,
        opened: LocalDate,
        status: PositionStatus = PositionStatus.OPEN,
        exitPremium: Long? = null,
        exitFees: Long? = null,
        closedOn: LocalDate? = null,
    ) = Position(
        id = id,
        ticker = ticker,
        side = side,
        type = type,
        strikeCents = 20_000,
        expiry = expiry,
        contracts = contracts,
        entryPremiumCents = premium,
        entryFeesCents = fees,
        openedOn = opened,
        notes = "",
        status = status,
        exitPremiumCents = exitPremium,
        exitFeesCents = exitFees,
        closedOn = closedOn,
        createdAtEpochMillis = id,
        updatedAtEpochMillis = id,
    )
}
