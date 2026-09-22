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
        assertTrue(draft.missingFields.isEmpty())
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
    fun missingPriceStillPrefillsTheRest() {
        val result = BrokerScreenshotParser.parse(
            """
            TSLL 09/25/2026 11.00 C
            Action Sell to Open
            Quantity 100
            Trade Date 09/22/2026
            """.trimIndent(),
        )
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals("TSLL", draft.ticker)
        assertEquals(OptionSide.SELL, draft.side)
        assertEquals("100", draft.contractsText)
        assertEquals("", draft.premiumText)
        assertEquals(LocalDate.of(2026, 9, 22), draft.openedOn)
        assertEquals(listOf("price"), draft.missingFields)
        assertTrue(draft.summary.contains("Missing price"))
        assertTrue(draft.summary.contains("tap Save"))
    }

    @Test
    fun symbolOnlyPrefillsPartialDraft() {
        val result = BrokerScreenshotParser.parse("TSLL 09/25/2026 11.00 C")
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals("TSLL", draft.ticker)
        assertEquals(OptionType.CALL, draft.type)
        assertEquals("11.00", draft.strikeText)
        assertEquals(LocalDate.of(2026, 9, 25), draft.expiry)
        assertEquals(null, draft.side)
        assertEquals("", draft.contractsText)
        assertEquals("", draft.premiumText)
        assertEquals(null, draft.openedOn)
        assertEquals(listOf("action", "quantity", "price", "trade date"), draft.missingFields)
    }

    @Test
    fun parsesAdjacentLabelAndValueLines() {
        assertSchwab(
            BrokerScreenshotParser.parse(
                """
                Trade Transaction Details
                TSLL 09/25/2026 11.00 C
                Trade Date
                09/22/2026
                Settle Date
                09/23/2026
                Order Id
                1008019740597
                Action
                Sell to Open
                Quantity
                100
                Price
                ${'$'}0.11
                Principal
                ${'$'}1,100.00
                Commission
                ${'$'}65.00
                Industry Fee
                ${'$'}1.55
                Total
                ${'$'}1,033.45
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesColumnMajorBlocksWhenLabelsAreNotNextToValues() {
        assertSchwab(
            BrokerScreenshotParser.parse(
                """
                Trade Transaction Details
                TSLL 09/25/2026 11.00 C
                Print
                Transactions
                Trade Details
                Trade Date
                Settle Date
                Order Id
                CUSIP #
                Action
                Quantity
                Price
                Principal
                Commission
                Industry Fee
                Total
                09/22/2026
                09/23/2026
                1008019740597
                Sell to Open
                100
                ${'$'}0.11
                ${'$'}1,100.00
                ${'$'}65.00
                ${'$'}1.55
                ${'$'}1,033.45
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesNoisyLabelsFromTheValueColumn() {
        assertSchwab(
            BrokerScreenshotParser.parse(
                """
                TSLL 09/25/2026 11.00 C
                Irade Date
                Settle Date
                Order ld
                CUSIP #
                Actlon
                Ouantity
                Pr1ce
                Principa1
                Cornmission
                lndustry Fee
                Tota1
                09/22/2026
                09/23/2026
                1008019740597
                Sell to Open
                100
                ${'$'}0.11
                ${'$'}1,100.00
                ${'$'}65.00
                ${'$'}1.55
                ${'$'}1,033.45
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesTesseractReadingOfTheSchwabScreenshot() {
        assertSchwab(
            BrokerScreenshotParser.parse(
                """
                1:18 Grok Bot 5Guc
                client.schwab.com/a
                Trade Transaction Details
                TSLL 09/25/2026 11.00 C Print
                Transactions Trade Details
                Trade Date 09/22/2026
                Settle Date 09/23/2026
                Order Id 1008019740597
                CUSIP #
                Action Sell to Open
                Quantity 100
                Price ${'$'}0.11
                Principal ${'$'}1,100.00
                Commission ${'$'}65.00
                Industry Fee ${'$'}1.55
                Total ${'$'}1,033.45
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesTesseractSparseLinesFromTheSchwabScreenshot() {
        assertSchwab(
            BrokerScreenshotParser.parse(
                """
                1:18 Grok Bot
                client.schwab.com/a
                Trade Transaction Details
                TSLL 09/25/2026 11.00 C
                Print
                Transactions
                Trade Details
                Trade Date
                09/22/2026
                Settle Date
                09/23/2026
                Order Id
                1008019740597
                CUSIP #
                Action
                Sell to Open
                Quantity
                100
                Price
                ${'$'}0.11
                Principal
                ${'$'}1,100.00
                Commission
                ${'$'}65.00
                Industry Fee
                ${'$'}1.55
                Total
                ${'$'}1,033.45
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun keepsIndustryFeeWhenTheTotalLineIsMissing() {
        val result = BrokerScreenshotParser.parse(
            """
            TSLL 09/25/2026 11.00 C
            Trade Date
            Settle Date
            Order Id
            Action
            Quantity
            Price
            Principal
            Commission
            Industry Fee
            09/22/2026
            09/23/2026
            1008019740597
            Sell to Open
            100
            ${'$'}0.11
            ${'$'}1,100.00
            ${'$'}65.00
            ${'$'}1.55
            """.trimIndent(),
        )
        val draft = (result as BrokerParseResult.Ready).draft
        assertEquals("66.55", draft.feesText)
        assertTrue(draft.missingFields.isEmpty())
    }

    private fun assertSchwab(result: BrokerParseResult) {
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
        assertTrue(draft.missingFields.isEmpty())
    }

    @Test
    fun emptyImageTextFails() {
        val result = BrokerScreenshotParser.parse("   \n")
        assertTrue(result is BrokerParseResult.Failed)
    }
}