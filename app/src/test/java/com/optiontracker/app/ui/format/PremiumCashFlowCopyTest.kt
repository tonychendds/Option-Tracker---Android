package com.optiontracker.app.ui.format

import com.optiontracker.app.domain.model.OptionSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PremiumCashFlowCopyTest {
    @Test
    fun buyPremiumIsADebitAndSellPremiumIsACredit() {
        assertEquals("Debit", premiumCashFlowLabel(OptionSide.BUY))
        assertEquals("Credit", premiumCashFlowLabel(OptionSide.SELL))
        assertEquals(
            "Debit — you pay this premium. Quoted per share. Total = premium × contracts × 100.",
            premiumCashFlowHint(OptionSide.BUY),
        )
        assertEquals(
            "Credit — you receive this premium. Quoted per share. Total = premium × contracts × 100.",
            premiumCashFlowHint(OptionSide.SELL),
        )
    }

    @Test
    fun exitPremiumReversesTheOpeningCashFlow() {
        assertEquals("Credit", exitPremiumCashFlowLabel(OptionSide.BUY))
        assertEquals("Debit", exitPremiumCashFlowLabel(OptionSide.SELL))
        assertEquals(
            "Credit — you receive this premium when selling to close. Quoted per share. Total = premium × contracts × 100.",
            exitPremiumCashFlowHint(OptionSide.BUY),
        )
        assertEquals(
            "Debit — you pay this premium when buying to close. Quoted per share. Total = premium × contracts × 100.",
            exitPremiumCashFlowHint(OptionSide.SELL),
        )
        assertFalse(exitPremiumCashFlowHint(OptionSide.BUY).contains("you pay"))
        assertFalse(exitPremiumCashFlowHint(OptionSide.SELL).contains("you receive"))
    }
}
