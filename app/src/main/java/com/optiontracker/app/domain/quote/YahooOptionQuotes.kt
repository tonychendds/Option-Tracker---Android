package com.optiontracker.app.domain.quote

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Delayed option premiums from the same Yahoo spark feed as stock quotes.
 * When bid and ask are both present, the mark is their midpoint.
 * Otherwise the regular-session last price is used.
 */
object YahooOptionQuotes {
    private val symbolPattern = Regex(""""symbol"\s*:\s*"([A-Za-z0-9]+)"\s*""")
    private val number = """(?:"(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)"|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|null)"""

    fun parse(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        val markers = symbolPattern.findAll(body).toList()
        val prices = linkedMapOf<String, String>()
        var index = 0
        while (index < markers.size) {
            val symbol = markers[index].groupValues[1].uppercase()
            var next = index + 1
            while (next < markers.size && markers[next].groupValues[1].equals(symbol, ignoreCase = true)) {
                next += 1
            }
            val end = if (next < markers.size) markers[next].range.first else body.length
            val slice = body.substring(markers[index].range.first, end)
            val premium = select(field(slice, "bid"), field(slice, "ask"), field(slice, "regularMarketPrice"))
            if (premium != null) prices.putIfAbsent(symbol, premium)
            index = next
        }
        return prices
    }

    /** Midpoint when both sides exist. Otherwise last. Null when neither is usable. */
    fun select(bidRaw: String?, askRaw: String?, lastRaw: String?): String? {
        val bid = bidRaw?.let(::decimal)
        val ask = askRaw?.let(::decimal)
        val chosen = if (bid != null && ask != null && bid.signum() >= 0 && ask.signum() >= 0) {
            bid.add(ask).divide(BigDecimal(2), 2, RoundingMode.HALF_UP)
        } else {
            lastRaw?.let(::decimal)?.takeIf { it.signum() >= 0 }
        } ?: return null
        return YahooSparkQuotes.formatPrice(chosen.toPlainString())
    }

    private fun field(slice: String, name: String): String? {
        val match = Regex(""""$name"\s*:\s*$number""").find(slice) ?: return null
        val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
        return raw.ifEmpty { null }
    }

    private fun decimal(raw: String): BigDecimal? = try {
        BigDecimal(raw)
    } catch (_: NumberFormatException) {
        null
    }
}
