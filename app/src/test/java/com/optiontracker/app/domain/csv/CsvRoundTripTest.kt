package com.optiontracker.app.domain.csv

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.monthlyReport
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.domain.pnl.realizedYearTotal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvRoundTripTest {
    @Test
    fun exportThenImportKeepsTradesAndRealizedTotals() {
        val trades = listOf(
            position(
                id = 1,
                ticker = "SPY",
                side = OptionSide.SELL,
                type = OptionType.PUT,
                strike = 50_000,
                contracts = 2,
                entry = 300,
                entryFees = 65,
                opened = LocalDate.of(2026, 9, 1),
                expiry = LocalDate.of(2026, 10, 16),
                account = "CASH",
                notes = "hedge",
            ),
            position(
                id = 2,
                ticker = "AAPL",
                side = OptionSide.BUY,
                type = OptionType.CALL,
                strike = 20_000,
                contracts = 1,
                entry = 250,
                exit = 400,
                entryFees = 100,
                opened = LocalDate.of(2026, 1, 5),
                expiry = LocalDate.of(2026, 1, 16),
                closedOn = LocalDate.of(2026, 2, 2),
                account = "IRA",
                notes = "rolled, earnings",
                override = 14_800,
                status = PositionStatus.CLOSED,
            ),
            position(
                id = 3,
                ticker = "QQQ",
                side = OptionSide.SELL,
                type = OptionType.PUT,
                strike = 40_000,
                contracts = 1,
                entry = 100,
                exit = 40,
                opened = LocalDate.of(2025, 12, 1),
                expiry = LocalDate.of(2025, 12, 19),
                closedOn = LocalDate.of(2026, 1, 8),
                account = "HSA",
                status = PositionStatus.CLOSED,
            ),
            position(
                id = 4,
                ticker = "MSFT",
                side = OptionSide.BUY,
                type = OptionType.CALL,
                strike = 30_000,
                contracts = 1,
                entry = 125,
                exit = 25,
                entryFees = 30,
                exitFees = 20,
                opened = LocalDate.of(2026, 3, 1),
                expiry = LocalDate.of(2026, 3, 20),
                closedOn = LocalDate.of(2026, 3, 18),
                account = "ROTH",
                notes = "said \"cut\", later",
                status = PositionStatus.CLOSED,
            ),
            position(
                id = 5,
                ticker = "IWM",
                side = OptionSide.SELL,
                type = OptionType.PUT,
                strike = 18_000,
                contracts = 1,
                entry = 200,
                exit = 0,
                opened = LocalDate.of(2026, 4, 1),
                expiry = LocalDate.of(2026, 4, 17),
                closedOn = LocalDate.of(2026, 4, 17),
                account = "CASH",
                status = PositionStatus.CLOSED,
            ),
        )

        val csv = CsvTradeWriter.write(trades)
        assertEquals(CsvTradeWriter.HEADER, csv.lineSequence().first())
        assertEquals(
            "status,account,ticker,side,right,strike,openDate,expDate,closeDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride",
            CsvTradeWriter.HEADER,
        )
        assertTrue(csv.contains("2026-01-08"))
        assertTrue(csv.contains("\"said \"\"cut\"\", later\""))
        assertEquals("option-tracker-trades-2026-09-24.csv", CsvTradeWriter.suggestedFileName(LocalDate.of(2026, 9, 24)))

        val imported = CsvTradeParser.parse(csv)
        assertNull(imported.fileError)
        assertEquals(0, imported.skippedCount)
        assertEquals(trades.map { it.roundTrip() }, imported.positions.map { it.roundTrip() })
        assertEquals(-10_050L, imported.positions[3].realizedPnlCents())
        assertEquals(50L, imported.positions[3].entryFeesCents)
        assertEquals(0L, imported.positions[3].exitFeesCents)

        assertEquals(realizedYearTotal(trades, 2026), realizedYearTotal(imported.positions, 2026))
        assertEquals(realizedYearTotal(trades, 2025), realizedYearTotal(imported.positions, 2025))
        assertEquals(monthlyReport(trades, 2026), monthlyReport(imported.positions, 2026))
        assertEquals(monthlyReport(trades, 2025), monthlyReport(imported.positions, 2025))
    }

    private fun Position.roundTrip(): Position = copy(
        id = 0L,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
        entryFeesCents = entryFeesCents + (exitFeesCents ?: 0L),
        exitFeesCents = if (status == PositionStatus.CLOSED) 0L else null,
        exitPremiumCents = if (status == PositionStatus.OPEN) null else exitPremiumCents,
        closedOn = if (status == PositionStatus.OPEN) null else closedOn,
        realizedOverrideCents = if (status == PositionStatus.OPEN) null else realizedOverrideCents,
    )

    private fun position(
        id: Long,
        ticker: String,
        side: OptionSide,
        type: OptionType,
        strike: Long,
        contracts: Int,
        entry: Long,
        opened: LocalDate,
        expiry: LocalDate,
        account: String = "",
        notes: String = "",
        entryFees: Long = 0L,
        exit: Long? = null,
        exitFees: Long? = null,
        closedOn: LocalDate? = null,
        override: Long? = null,
        status: PositionStatus = PositionStatus.OPEN,
    ) = Position(
        id = id,
        ticker = ticker,
        side = side,
        type = type,
        strikeCents = strike,
        expiry = expiry,
        contracts = contracts,
        entryPremiumCents = entry,
        entryFeesCents = entryFees,
        openedOn = opened,
        notes = notes,
        status = status,
        exitPremiumCents = exit,
        exitFeesCents = exitFees,
        closedOn = closedOn,
        createdAtEpochMillis = id,
        updatedAtEpochMillis = id,
        account = account,
        realizedOverrideCents = override,
    )
}
