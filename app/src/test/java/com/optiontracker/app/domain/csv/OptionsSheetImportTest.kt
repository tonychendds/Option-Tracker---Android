package com.optiontracker.app.domain.csv

import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.pnl.realizedYearTotal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionsSheetImportTest {
    @Test
    fun importsEveryRowFromTheOptionsSheet() {
        val text = javaClass.getResource("/option_tracker_import.csv")!!.readText()
        val result = CsvTradeParser.parse(text)

        assertNull(result.fileError)
        assertEquals(result.errors.joinToString("\n"), 0, result.skippedCount)
        assertTrue(result.errors.isEmpty())
        assertEquals(8, result.openCount)
        assertEquals(94, result.closedCount)
        assertEquals(102, result.positions.size)
        assertTrue(result.replacedExisting)

        val first = result.positions.first()
        assertEquals("", first.account)
        assertEquals("TSLL", first.ticker)
        assertEquals(1_100L, first.strikeCents)
        assertEquals(727L, first.entryPremiumCents)
        assertEquals(105L, first.exitPremiumCents)
        assertEquals(685_000L, first.realizedOverrideCents)
        assertEquals(LocalDate.of(2026, 5, 1), first.closedOn)
        assertTrue(result.positions.any { it.account.isEmpty() })

        val closed = result.positions.filter { it.status == PositionStatus.CLOSED }
        assertEquals(12_941_400L, closed.sumOf { it.realizedOverrideCents ?: 0L })

        val year2026 = realizedYearTotal(result.positions, 2026)
        assertEquals(93, year2026.tradeCount)
        assertEquals(12_846_400L, year2026.totalCents)

        val year2027 = realizedYearTotal(result.positions, 2027)
        assertEquals(1, year2027.tradeCount)
        assertEquals(95_000L, year2027.totalCents)

        assertEquals(0, realizedYearTotal(result.positions.filter { it.status == PositionStatus.OPEN }, 2026).tradeCount)
    }
}
