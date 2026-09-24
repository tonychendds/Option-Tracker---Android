package com.optiontracker.app.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrLayoutTest {
    @Test
    fun joinsSchwabWordBoxesIntoRowsBeforeParsing() {
        val lines = listOf(
            OcrLine("Trade", 68, 337, 43),
            OcrLine("Transaction", 221, 336, 44),
            OcrLine("Details", 528, 336, 44),
            OcrLine("TSLL", 69, 550, 32),
            OcrLine("09/25/2026", 185, 550, 34),
            OcrLine("11.00", 442, 550, 32),
            OcrLine("C", 567, 550, 32),
            OcrLine("Print", 922, 550, 32),
            OcrLine("Transactions", 69, 710, 26),
            OcrLine("Trade", 790, 708, 28),
            OcrLine("Details", 898, 708, 28),
            OcrLine("Trade", 69, 816, 33),
            OcrLine("Date", 198, 817, 32),
            OcrLine("Settle", 69, 931, 33),
            OcrLine("Date", 200, 932, 32),
            OcrLine("Order", 70, 1046, 33),
            OcrLine("Id", 196, 1046, 33),
            OcrLine("CUSIP", 70, 1163, 32),
            OcrLine("#", 210, 1163, 32),
            OcrLine("Action", 68, 1273, 50),
            OcrLine("Quantity", 70, 1389, 49),
            OcrLine("Price", 71, 1509, 32),
            OcrLine("Principal", 71, 1615, 54),
            OcrLine("Commission", 70, 1730, 51),
            OcrLine("Industry", 72, 1854, 42),
            OcrLine("Fee", 248, 1855, 32),
            OcrLine("Total", 68, 1964, 48),
            OcrLine("09/22/2026", 771, 817, 34),
            OcrLine("09/23/2026", 771, 932, 34),
            OcrLine("1008019740597", 682, 1047, 32),
            OcrLine("Sell", 770, 1277, 33),
            OcrLine("to", 854, 1281, 29),
            OcrLine("Open", 908, 1278, 41),
            OcrLine("100", 939, 1393, 32),
            OcrLine("${'$'}0.11", 900, 1504, 41),
            OcrLine("${'$'}1,100.00", 814, 1619, 43),
            OcrLine("${'$'}65.00", 874, 1734, 41),
            OcrLine("${'$'}1.55", 900, 1850, 41),
            OcrLine("${'$'}1,033.45", 806, 1965, 45),
        )
        val joined = OcrLayout.joinLines(lines)
        assertTrue(joined.contains("TSLL 09/25/2026 11.00 C"))
        assertTrue(joined.contains("Trade Date 09/22/2026"))
        assertTrue(joined.contains("Action Sell to Open"))
        assertTrue(joined.contains("Quantity 100"))
        assertTrue(joined.contains("Price ${'$'}0.11"))
        assertTrue(joined.contains("Industry Fee ${'$'}1.55"))

        val draft = (BrokerScreenshotParser.parse(joined) as BrokerParseResult.Ready).draft
        assertEquals("TSLL", draft.ticker)
        assertEquals("100", draft.contractsText)
        assertEquals("0.11", draft.premiumText)
        assertEquals("66.55", draft.feesText)
        assertEquals(java.time.LocalDate.of(2026, 9, 22), draft.openedOn)
        assertTrue(draft.missingFields.isEmpty())
    }
}
