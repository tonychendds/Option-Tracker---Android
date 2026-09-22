package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthlyReportTest {
    @Test
    fun listsEveryMonthAndUsesCloseDateAndOverride() {
        val open = closed(
            id = 1,
            closedOn = LocalDate.of(2026, 3, 1),
            status = PositionStatus.OPEN,
            realizedOverrideCents = 9_900L,
        )
        val january = closed(
            id = 2,
            closedOn = LocalDate.of(2026, 1, 15),
            realizedOverrideCents = 100L,
        )
        val septemberWin = closed(
            id = 3,
            closedOn = LocalDate.of(2026, 9, 10),
            side = OptionSide.BUY,
            premium = 250,
            fees = 100,
            exitPremium = 400,
            exitFees = 100,
        )
        val septemberLoss = closed(
            id = 4,
            closedOn = LocalDate.of(2026, 9, 20),
            realizedOverrideCents = -5_000L,
        )
        val septemberFlat = closed(
            id = 5,
            closedOn = LocalDate.of(2026, 9, 21),
            realizedOverrideCents = 0L,
        )
        val august = closed(
            id = 6,
            closedOn = LocalDate.of(2026, 8, 15),
            side = OptionSide.SELL,
            premium = 100,
            fees = 0,
            exitPremium = 40,
            exitFees = 0,
        )
        val priorYear = closed(
            id = 7,
            closedOn = LocalDate.of(2025, 11, 3),
            realizedOverrideCents = 5_000L,
        )
        val positions = listOf(
            open,
            january,
            septemberWin,
            septemberLoss,
            septemberFlat,
            august,
            priorYear,
        )

        val rows = monthlyReport(positions, 2026)

        assertEquals(12, rows.size)
        assertEquals(YearMonth.of(2026, 1), rows.first().yearMonth)
        assertEquals(YearMonth.of(2026, 12), rows.last().yearMonth)

        val januaryRow = rows[0]
        assertEquals(100L, januaryRow.totalCents)
        assertEquals(1, januaryRow.tradeCount)
        assertEquals(1, januaryRow.winningCount)
        assertEquals(1.0, januaryRow.hitRate)

        val february = rows[1]
        assertEquals(0L, february.totalCents)
        assertEquals(0, february.tradeCount)
        assertNull(february.hitRate)

        val augustRow = rows[7]
        assertEquals(6_000L, augustRow.totalCents)
        assertEquals(1, augustRow.winningCount)
        assertEquals(1.0, augustRow.hitRate)

        val september = rows[8]
        assertEquals(14_800L + (-5_000L), september.totalCents)
        assertEquals(3, september.tradeCount)
        assertEquals(1, september.winningCount)
        assertEquals(1.0 / 3.0, september.hitRate!!, 0.0001)

        assertEquals(0, rows[2].tradeCount)
        assertEquals(rows.sumOf { it.totalCents }, realizedYearTotal(positions, 2026).totalCents)
        assertEquals(rows.sumOf { it.tradeCount }, realizedYearTotal(positions, 2026).tradeCount)

        val prior = monthlyReport(positions, 2025)
        assertEquals(5_000L, prior[10].totalCents)
        assertEquals(1, prior[10].tradeCount)
        assertEquals(0, prior.sumOf { it.tradeCount } - 1)
    }

    private fun closed(
        id: Long,
        closedOn: LocalDate,
        status: PositionStatus = PositionStatus.CLOSED,
        side: OptionSide = OptionSide.BUY,
        premium: Long = 100,
        fees: Long = 0,
        exitPremium: Long? = 100,
        exitFees: Long? = 0,
        realizedOverrideCents: Long? = null,
    ) = Position(
        id = id,
        ticker = "AAPL",
        side = side,
        type = OptionType.CALL,
        strikeCents = 20_000,
        expiry = closedOn,
        contracts = 1,
        entryPremiumCents = premium,
        entryFeesCents = fees,
        openedOn = closedOn.minusDays(10),
        notes = "",
        status = status,
        exitPremiumCents = exitPremium,
        exitFeesCents = exitFees,
        closedOn = closedOn,
        createdAtEpochMillis = id,
        updatedAtEpochMillis = id,
        realizedOverrideCents = realizedOverrideCents,
    )
}