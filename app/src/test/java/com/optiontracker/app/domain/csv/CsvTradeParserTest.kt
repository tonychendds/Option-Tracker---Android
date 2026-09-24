package com.optiontracker.app.domain.csv

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.realizedPnlCents
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTradeParserTest {
    @Test
    fun mapsOpenAndClosedRowsAndPrefersRealizedOverride() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Open,CASH,spy,Sell,Put,500,2026-09-01,2026-10-16,2,3.00,,0.65,hedge,
            Closed,IRA,AAPL,Buy,Call,200.00,2026-09-01,2026-09-18,1,2.50,4.00,1.00,"rolled, earnings",148.00
            Closed,HSA,QQQ,Sell,Put,400,2026-08-01,2026-08-21,1,1.00,,,0,
            """.trimIndent(),
        )

        assertNull(result.fileError)
        assertEquals(1, result.openCount)
        assertEquals(1, result.closedCount)
        assertEquals(1, result.skippedCount)
        assertTrue(result.errors.single().contains("Row 4"))
        assertTrue(result.replacedExisting)

        val open = result.positions[0]
        assertEquals(PositionStatus.OPEN, open.status)
        assertEquals("CASH", open.account)
        assertEquals("SPY", open.ticker)
        assertEquals(OptionSide.SELL, open.side)
        assertEquals(OptionType.PUT, open.type)
        assertEquals(50_000L, open.strikeCents)
        assertEquals(2, open.contracts)
        assertEquals(300L, open.entryPremiumCents)
        assertEquals(65L, open.entryFeesCents)
        assertNull(open.exitPremiumCents)
        assertNull(open.closedOn)
        assertNull(open.realizedOverrideCents)

        val closed = result.positions[1]
        assertEquals(PositionStatus.CLOSED, closed.status)
        assertEquals("IRA", closed.account)
        assertEquals(OptionSide.BUY, closed.side)
        assertEquals(OptionType.CALL, closed.type)
        assertEquals(LocalDate.of(2026, 9, 18), closed.closedOn)
        assertEquals(LocalDate.of(2026, 9, 18), closed.expiry)
        assertEquals(100L, closed.entryFeesCents)
        assertEquals(0L, closed.exitFeesCents)
        assertEquals("rolled, earnings", closed.notes)
        assertEquals(14_800L, closed.realizedOverrideCents)
        assertEquals(14_800L, closed.realizedPnlCents())
    }

    @Test
    fun blankOverrideUsesCalculatedProfit() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Closed,ROTH,MSFT,Buy,Call,300,2026-01-02,2026-01-16,1,1.00,2.00,0,,
            """.trimIndent(),
        )
        val closed = result.positions.single()
        assertNull(closed.realizedOverrideCents)
        // (200 - 100) dollars notional, no fees.
        assertEquals(10_000L, closed.realizedPnlCents())
    }

    @Test
    fun negativeOverrideAndQuotedQuotes() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Closed,IRA,IWM,Buy,Put,180,2026-03-01,2026-03-20,1,1.25,0.25,0,"said ""cut""${'"'},($50.00)
            """.trimIndent(),
        )
        val closed = result.positions.single()
        assertEquals("said \"cut\"", closed.notes)
        assertEquals(-5_000L, closed.realizedPnlCents())
    }

    @Test
    fun openRowIgnoresExitAndOverride() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Open,,BRK.B,Buy,Call,450.5,2026-09-01,2026-12-18,1,10,4,,"still open",99
            """.trimIndent(),
        )
        val open = result.positions.single()
        assertEquals("BRK.B", open.ticker)
        assertEquals(45_050L, open.strikeCents)
        assertNull(open.exitPremiumCents)
        assertNull(open.realizedOverrideCents)
        assertEquals("", open.account)
        assertEquals(0L, open.entryFeesCents)
    }

    @Test
    fun closeDateIsUsedWhenPresentAndExpirationRemainsTheFallback() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,closeDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Closed,IRA,AAPL,Buy,Call,200,2026-09-01,2026-09-18,2026-10-02,1,2.50,4.00,1.00,,
            Closed,HSA,QQQ,Sell,Put,400,2026-08-01,2026-08-21,,1,1.00,0.40,0,,
            Closed,CASH,SPY,Sell,Put,500,2026-07-01,2026-07-17,not-a-date,1,1.00,0.10,0,,
            """.trimIndent(),
        )
        assertNull(result.fileError)
        assertEquals(2, result.closedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(LocalDate.of(2026, 10, 2), result.positions[0].closedOn)
        assertEquals(LocalDate.of(2026, 9, 18), result.positions[0].expiry)
        assertEquals(LocalDate.of(2026, 8, 21), result.positions[1].closedOn)
        assertTrue(result.errors.single().contains("Close date"))
    }

    @Test
    fun missingHeaderDoesNotProduceTrades() {
        val result = CsvTradeParser.parse("ticker,side\nAAPL,Buy\n")
        assertFalse(result.replacedExisting)
        assertTrue(result.positions.isEmpty())
        assertTrue(result.fileError!!.contains("openDate"))
    }

    @Test
    fun skipsInvalidRowsAndKeepsTheRest() {
        val result = CsvTradeParser.parse(
            """
            status,account,ticker,side,right,strike,openDate,expDate,contracts,entryPremium,exitPremium,fees,notes,realizedOverride
            Open,CASH,AAPL,Hold,Call,100,2026-09-01,2026-10-01,1,1,0,0,,
            Open,CASH,AMD,Buy,Call,100,2026-09-01,2026-10-01,1,1.5,,,note,
            
            """.trimIndent(),
        )
        assertEquals(1, result.openCount)
        assertEquals(1, result.skippedCount)
        assertEquals("AMD", result.positions.single().ticker)
        assertEquals("note", result.positions.single().notes)
    }
}