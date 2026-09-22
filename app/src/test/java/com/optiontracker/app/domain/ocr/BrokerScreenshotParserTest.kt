package com.optiontracker.app.domain.ocr

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerScreenshotParserTest {
    @Test
    fun parsesSchwabSellToOpenSample() {
        val result = BrokerScreenshotParser.parse(
            """
            Trade Transaction Details
            TSLL 09/25/2026 11.00 C
            Transactions          Trade Details
            Trade Date            09/22/2026
            Settle Date           09/23/2026
            Order Id              1008019740597
            CUSIP #
            Action                Sell to Open
            Quantity              100
            Price                 ${'$'}0.11
            Principal             ${'$'}1,100.00
            Commission            ${'$'}65.00
            Industry Fee          ${'$'}1.55
            Total                 ${'$'}1,033.45
            """.trimIndent(),
        )
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals("TSLL", draft.ticker)
        assertEquals(OptionSide.SELL, draft.side)
        assertEquals(OptionType.CALL, draft.type)
        assertEquals("11.00", draft.strikeText)
        assertEquals(LocalDate.of(2026, 9, 25), draft.expiry)
        assertEquals("100", draft.contractsText)
        assertEquals("0.11", draft.premiumText)
        assertEquals("66.55", draft.feesText)
        assertEquals(LocalDate.of(2026, 9, 22), draft.openedOn)
        assertEquals("Schwab order 1008019740597", draft.notes)
        assertFalse(draft.closingLeg)
        assertTrue(draft.summary.contains("Detected Sell Call TSLL"))
        assertTrue(draft.summary.contains("tap Save"))
    }

    @Test
    fun parsesIsoDatePutAndBuyToOpen() {
        val result = BrokerScreenshotParser.parse(
            """
            AAPL 2026-01-16 150.5 P
            Action Buy to Open
            Qty 2
            Price ${'$'}3.10
            Trade Date 2026-01-02
            Commission ${'$'}1.00
            """.trimIndent(),
        )
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals("AAPL", draft.ticker)
        assertEquals(OptionSide.BUY, draft.side)
        assertEquals(OptionType.PUT, draft.type)
        assertEquals("150.50", draft.strikeText)
        assertEquals(LocalDate.of(2026, 1, 16), draft.expiry)
        assertEquals("2", draft.contractsText)
        assertEquals("3.10", draft.premiumText)
        assertEquals("1.00", draft.feesText)
        assertEquals(LocalDate.of(2026, 1, 2), draft.openedOn)
        assertEquals("Schwab", draft.notes)
    }

    @Test
    fun closingActionIsNotedAndNotAppliedAsAClose() {
        val result = BrokerScreenshotParser.parse(
            """
            NVDA 10/02/2026 180 C
            Action: Buy to Close
            Quantity: 1
            Price: ${'$'}0.40
            Trade Date: 09/22/2026
            Regulatory Fee ${'$'}0.05
            """.trimIndent(),
        )
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals(OptionSide.BUY, draft.side)
        assertTrue(draft.closingLeg)
        assertEquals("0.05", draft.feesText)
        assertTrue(draft.summary.contains("Buy to Close"))
        assertTrue(draft.summary.contains("does not close"))
        assertTrue(draft.notes.contains("does not close"))
    }

    @Test
    fun missingPriceLeavesManualEntry() {
        val result = BrokerScreenshotParser.parse(
            """
            TSLL 09/25/2026 11.00 C
            Action Sell to Open
            Quantity 100
            Trade Date 09/22/2026
            """.trimIndent(),
        )
        val failed = result as BrokerParseResult.Failed
        assertTrue(failed.message.contains("price"))
        assertTrue(failed.message.contains("manually"))
    }

    @Test
    fun emptyImageTextFails() {
        val result = BrokerScreenshotParser.parse("   \n")
        assertTrue(result is BrokerParseResult.Failed)
    }
}