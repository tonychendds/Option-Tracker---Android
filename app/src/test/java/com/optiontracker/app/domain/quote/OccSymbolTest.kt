package com.optiontracker.app.domain.quote

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OccSymbolTest {
    private val expiry = LocalDate.of(2026, 9, 25)

    @Test
    fun buildsYahooOccSymbol() {
        assertEquals(
            "NVDA260925C00230000",
            OccSymbol.yahoo("nvda", OptionType.CALL, 23_000, expiry),
        )
        assertEquals(
            "NVDA260925P00220000",
            OccSymbol.yahoo("NVDA", OptionType.PUT, 22_000, expiry),
        )
        assertEquals(
            "BRKB261218C00500000",
            OccSymbol.yahoo("brk.b", OptionType.CALL, 50_000, LocalDate.of(2026, 12, 18)),
        )
    }

    @Test
    fun rejectsStrikeThatDoesNotFitTheOccField() {
        assertNull(OccSymbol.yahoo("NVDA", OptionType.CALL, 0, expiry))
        assertNull(OccSymbol.yahoo("NVDA", OptionType.CALL, 10_000_000, expiry))
    }

    @Test
    fun midpointWhenBothSidesExistOtherwiseLast() {
        val parsed = YahooOptionQuotes.parse(
            """
            {"spark":{"result":[
              {"symbol":"NVDA260925C00230000","response":[{"meta":{"symbol":"NVDA260925C00230000","bid":1.40,"ask":1.45,"regularMarketPrice":1.42}}]},
              {"symbol":"NVDA260925P00220000","response":[{"meta":{"symbol":"NVDA260925P00220000","bid":null,"ask":0.40,"regularMarketPrice":0.31}}]}
            ]}}
            """.trimIndent(),
        )
        assertEquals("$1.43", parsed["NVDA260925C00230000"])
        assertEquals("$0.31", parsed["NVDA260925P00220000"])
        assertEquals("$1.42", YahooOptionQuotes.select(null, null, "1.42"))
        assertNull(YahooOptionQuotes.select(null, "0.40", null))
    }

    @Test
    fun premiumLineKeepsEntryAndAddsNow() {
        val position = position()
        assertEquals("entry $1.80", OccSymbol.premiumLine(position, QuoteBoard()))
        val symbol = OccSymbol.yahoo("NVDA", OptionType.CALL, 23_000, expiry)!!
        assertEquals(
            "entry $1.80 · now $1.42",
            OccSymbol.premiumLine(position, QuoteBoard(prices = mapOf(symbol to "$1.42"))),
        )
        assertEquals(
            "entry $1.80 · now —",
            OccSymbol.premiumLine(position, QuoteBoard(unavailable = setOf(symbol))),
        )
    }

    private fun position() = Position(
        id = 1,
        ticker = "NVDA",
        side = OptionSide.SELL,
        type = OptionType.CALL,
        strikeCents = 23_000,
        expiry = expiry,
        contracts = 1,
        entryPremiumCents = 180,
        entryFeesCents = 0,
        openedOn = LocalDate.of(2026, 9, 1),
        notes = "",
        status = PositionStatus.OPEN,
        exitPremiumCents = null,
        exitFeesCents = null,
        closedOn = null,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
