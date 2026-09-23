package com.optiontracker.app.domain.quote

import com.optiontracker.app.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Delayed last/regular prices from Yahoo Finance's public spark feed.
 * No API key. Prices are stock prices, not option marks.
 */
object YahooSparkQuotes {
    const val DELAY_LABEL = "Stock quotes delayed 15+ min. Not an option price."
    private const val CACHE_MILLIS = 90_000L
    private val symbolPattern = Regex(""""symbol"\s*:\s*"([A-Za-z0-9.-]+)"\s*""")
    private val pricePattern = Regex(
        """"regularMarketPrice"\s*:\s*(?:"(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)"|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|null)""",
    )

    fun endpoint(yahooSymbols: List<String>): String {
        val symbols = yahooSymbols.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        return "https://query1.finance.yahoo.com/v7/finance/spark?symbols=$symbols&range=1d&interval=1d"
    }

    /** Yahoo uses a hyphen for class shares (`BRK.B` → `BRK-B`). */
    fun requestSymbol(ticker: String): String = quoteKey(ticker).replace('.', '-')

    fun parse(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        val prices = linkedMapOf<String, String>()
        for (match in pricePattern.findAll(body)) {
            val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
            if (raw.isEmpty()) continue
            val formatted = formatPrice(raw) ?: continue
            val symbol = symbolPattern.findAll(body.substring(0, match.range.first))
                .lastOrNull()
                ?.groupValues
                ?.get(1)
                ?: continue
            prices.putIfAbsent(symbol.uppercase(Locale.US), formatted)
        }
        return prices
    }

    fun formatPrice(raw: String): String? {
        val decimal = try {
            BigDecimal(raw)
        } catch (_: NumberFormatException) {
            return null
        }
        if (decimal.signum() < 0) return null
        val cents = try {
            decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        if (cents > Money.MAX_CENTS) return null
        return Money.format(cents)
    }

    fun cacheMillis(): Long = CACHE_MILLIS
}
