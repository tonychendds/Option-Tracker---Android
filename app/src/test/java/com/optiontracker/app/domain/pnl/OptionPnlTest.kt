package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import org.junit.Assert.assertEquals
import org.junit.Test

class OptionPnlTest {
    @Test
    fun longCallProfitMatchesEquityMultiplier() {
        // Buy 1 at $2.50, $1.00 fee. Close at $4.00, $1.00 fee.
        // Notional 250 and 400 dollars. P/L = 150 - 1 - 1 = $148.
        val pnl = OptionPnl.realizedPnlCents(
            side = OptionSide.BUY,
            contracts = 1,
            entryPremiumCents = 250,
            entryFeesCents = 100,
            exitPremiumCents = 400,
            exitFeesCents = 100,
        )
        assertEquals(14_800L, pnl)
        assertEquals(25_000L, OptionPnl.notionalCents(250, 1))
    }

    @Test
    fun shortPutProfitMatchesEquityMultiplier() {
        // Sell 2 at $3.00, $1.30 fee. Close at $1.20, $1.30 fee.
        // Notional $600 vs $240. P/L = 360 - 1.30 - 1.30 = $357.40.
        val pnl = OptionPnl.realizedPnlCents(
            side = OptionSide.SELL,
            contracts = 2,
            entryPremiumCents = 300,
            entryFeesCents = 130,
            exitPremiumCents = 120,
            exitFeesCents = 130,
        )
        assertEquals(35_740L, pnl)
        assertEquals(60_000L, OptionPnl.notionalCents(300, 2))
    }

    @Test
    fun longLossAndWorthlessClose() {
        val pnl = OptionPnl.realizedPnlCents(
            side = OptionSide.BUY,
            contracts = 1,
            entryPremiumCents = 200,
            entryFeesCents = 0,
            exitPremiumCents = 50,
            exitFeesCents = 0,
        )
        assertEquals(-15_000L, pnl)

        val expired = OptionPnl.realizedPnlCents(
            side = OptionSide.BUY,
            contracts = 1,
            entryPremiumCents = 100,
            entryFeesCents = 65,
            exitPremiumCents = 0,
            exitFeesCents = 0,
        )
        assertEquals(-10_065L, expired)
    }

    @Test
    fun shortLossWhenExitIsHigher() {
        val pnl = OptionPnl.realizedPnlCents(
            side = OptionSide.SELL,
            contracts = 3,
            entryPremiumCents = 100,
            entryFeesCents = 50,
            exitPremiumCents = 250,
            exitFeesCents = 50,
        )
        // Entry notional 100*3*100 = 30_000. Exit 250*3*100 = 75_000.
        // P/L = 30_000 - 75_000 - 50 - 50 = -45_100.
        assertEquals(-45_100L, pnl)
    }

    @Test
    fun entryCashFlowCreditsAndDebits() {
        assertEquals(
            -25_100L,
            OptionPnl.entryCashFlowCents(OptionSide.BUY, 250, 1, 100),
        )
        assertEquals(
            59_870L,
            OptionPnl.entryCashFlowCents(OptionSide.SELL, 300, 2, 130),
        )
    }

    @Test
    fun feesCanTurnAShortIntoADebit() {
        // $0.01 premium × 1 × 100 = $1.00 credit, $2.00 fees.
        assertEquals(
            -100L,
            OptionPnl.entryCashFlowCents(OptionSide.SELL, 1, 1, 200),
        )
    }
}
