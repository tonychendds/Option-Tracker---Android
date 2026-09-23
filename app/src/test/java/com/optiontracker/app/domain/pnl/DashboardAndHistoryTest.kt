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

    @Test
    fun ytdSumsClosedTradesAcrossMonthsAndSkipsOtherYears() {
        val openWithCloseDate = longCall().copy(
            closedOn = LocalDate.of(2026, 3, 1),
            realizedOverrideCents = 9_900L,
        )
        val januaryOverride = closedThisMonth().copy(
            id = 5,
            ticker = "MSFT",
            openedOn = LocalDate.of(2026, 1, 5),
            closedOn = LocalDate.of(2026, 1, 15),
            realizedOverrideCents = 100L,
        )
        val december = closedLastMonth().copy(
            id = 6,
            openedOn = LocalDate.of(2026, 12, 1),
            closedOn = LocalDate.of(2026, 12, 18),
            realizedOverrideCents = -2_000L,
        )
        val priorYear = closedThisMonth().copy(
            id = 7,
            ticker = "IWM",
            openedOn = LocalDate.of(2025, 11, 1),
            closedOn = LocalDate.of(2025, 11, 3),
            realizedOverrideCents = 5_000L,
        )
        val closed = listOf(closedThisMonth(), closedLastMonth(), januaryOverride, december, priorYear)
        val summary = buildDashboardSummary(
            open = listOf(openWithCloseDate),
            closed = closed,
            today = today,
            selectedYear = 2026,
        )

        // September buy $148, August sell $60, January override $1, December override −$20.
        assertEquals(14_800L + 6_000L + 100L + (-2_000L), summary.realizedYearCents)
        assertEquals(4, summary.closedYearCount)
        assertEquals(14_800L, summary.realizedThisMonthCents)
        assertEquals(1, summary.closedThisMonthCount)
        assertEquals(listOf(2026, 2025), summary.availableYears)
        assertEquals("2026 YTD", ytdLabel(summary.selectedYear))

        val prior = realizedYearTotal(closed + openWithCloseDate, 2025)
        assertEquals(5_000L, prior.totalCents)
        assertEquals(1, prior.tradeCount)

        val openOnly = realizedYearTotal(listOf(openWithCloseDate), 2026)
        assertEquals(0L, openOnly.totalCents)
        assertEquals(0, openOnly.tradeCount)
    }

    @Test
    fun historyYearTotalMatchesTheMonthsInThatYear() {
        val priorYear = closedThisMonth().copy(
            id = 7,
            ticker = "IWM",
            openedOn = LocalDate.of(2025, 11, 1),
            closedOn = LocalDate.of(2025, 11, 3),
            realizedOverrideCents = 5_000L,
        )
        val closed = listOf(closedThisMonth(), closedLastMonth(), priorYear)
        val months = groupClosedTrades(closed, "").filter { it.yearMonth.year == 2026 }
        val total = realizedYearTotal(closed, 2026)
        assertEquals(2, months.size)
        assertEquals(total.totalCents, months.sumOf { it.totalPnlCents })
        assertEquals(14_800L + 6_000L, total.totalCents)
        assertEquals(5_000L, realizedYearTotal(closed, 2025).totalCents)
    }

    @Test
    fun homeMonthUsesOpenDateAndYtdKeepsCloseYear() {
        val openedSeptemberClosedOctober = closedThisMonth().copy(
            id = 8,
            openedOn = LocalDate.of(2026, 9, 4),
            closedOn = LocalDate.of(2026, 10, 16),
            realizedOverrideCents = 2_500L,
        )
        val stillOpen = longCall().copy(realizedOverrideCents = 9_900L)
        val openedLastDecemberClosedThisJanuary = closedThisMonth().copy(
            id = 9,
            openedOn = LocalDate.of(2025, 12, 20),
            closedOn = LocalDate.of(2026, 1, 8),
            realizedOverrideCents = 800L,
        )
        val closed = listOf(openedSeptemberClosedOctober, openedLastDecemberClosedThisJanuary)
        val summary = buildDashboardSummary(
            open = listOf(stillOpen),
            closed = closed,
            today = today,
            selectedYear = 2026,
        )
        assertEquals(2_500L, summary.realizedThisMonthCents)
        assertEquals(1, summary.closedThisMonthCount)
        assertEquals(2_500L + 800L, summary.realizedYearCents)
        assertEquals(2, summary.closedYearCount)

        val september = buildDashboardSummary(
            open = emptyList(),
            closed = closed,
            today = LocalDate.of(2026, 10, 1),
            selectedYear = 2026,
        )
        assertEquals(0L, september.realizedThisMonthCents)
        assertEquals(0, september.closedThisMonthCount)

        val report = monthlyReport(closed + stillOpen, 2026)
        assertEquals(2_500L, report[8].totalCents)
        assertEquals(1, report[8].tradeCount)
        assertEquals(0, report[0].tradeCount)
        val priorDecember = monthlyReport(closed, 2025)[11]
        assertEquals(800L, priorDecember.totalCents)
        assertEquals(1, priorDecember.tradeCount)
        assertEquals(800L, realizedYearTotal(closed, 2026).totalCents - 2_500L)
        assertEquals(0L, realizedYearTotal(closed, 2025).totalCents)
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
