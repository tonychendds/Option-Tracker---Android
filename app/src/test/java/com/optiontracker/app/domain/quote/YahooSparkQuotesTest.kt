package com.optiontracker.app.domain.quote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooSparkQuotesTest {
    @Test
    fun mapsClassShareAndParsesBatchPrices() {
        assertEquals("BRK-B", YahooSparkQuotes.requestSymbol(" brk.b "))
        assertEquals("NVDA", YahooSparkQuotes.requestSymbol("nvda"))
        val parsed = YahooSparkQuotes.parse(
            """
            {"spark":{"result":[
              {"symbol":"NVDA","response":[{"meta":{"symbol":"NVDA","regularMarketPrice":228.87,"currency":"USD"}}]},
              {"symbol":"BRK-B","response":[{"meta":{"symbol":"BRK-B","regularMarketPrice":503.495}}]},
              {"symbol":"AMD","response":[{"meta":{"symbol":"AMD","regularMarketPrice":null}}]}
            ]}}
            """.trimIndent(),
        )
        assertEquals(mapOf("NVDA" to "$228.87", "BRK-B" to "$503.50"), parsed)
    }

    @Test
    fun blankOrBrokenPayloadIsEmpty() {
        assertTrue(YahooSparkQuotes.parse("").isEmpty())
        assertTrue(YahooSparkQuotes.parse("not json").isEmpty())
        assertEquals(null, YahooSparkQuotes.formatPrice("-1"))
    }

    @Test
    fun loaderDedupesCachesAndKeepsLastPrice() = runBlocking {
        var calls = 0
        var requested = emptySet<String>()
        var fail = false
        val loader = UnderlyingQuoteLoader(
            source = { tickers ->
                calls += 1
                requested = tickers
                if (fail) error("offline")
                tickers.filter { it != "AMD" }.associateWith { "$10.00" }
            },
            nowMillis = { 1_000L },
        )
        val first = loader.refresh(listOf("nvda", "NVDA", "AMD"), force = false)
        assertEquals(1, calls)
        assertEquals(setOf("NVDA", "AMD"), requested)
        assertEquals("$10.00", first.prices["NVDA"])
        assertEquals(setOf("AMD"), first.unavailable)
        assertEquals("$10.00", underlyingQuoteLabel("nvda", first))
        assertEquals("—", underlyingQuoteLabel("AMD", first))
        assertEquals("", underlyingQuoteLabel("TSLA", first))

        loader.refresh(listOf("NVDA", "AMD"), force = false)
        assertEquals(1, calls)

        fail = true
        val kept = loader.refresh(listOf("NVDA", "TSLA"), force = true)
        assertEquals("$10.00", kept.prices["NVDA"])
        assertTrue("TSLA" in kept.unavailable)
    }
}
