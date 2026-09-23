package com.optiontracker.app.domain.quote

import java.util.Locale

data class QuoteBoard(
    val prices: Map<String, String> = emptyMap(),
    val unavailable: Set<String> = emptySet(),
    val refreshing: Boolean = false,
)

fun interface UnderlyingQuoteSource {
    /** Formatted prices keyed by [quoteKey]. Missing keys were not returned. */
    suspend fun fetch(tickers: Set<String>): Map<String, String>
}

fun quoteKey(ticker: String): String = ticker.trim().uppercase(Locale.US)

/** Label beside a ticker. Blank while a quote is still loading. */
fun underlyingQuoteLabel(ticker: String, board: QuoteBoard): String {
    val key = quoteKey(ticker)
    if (key.isEmpty()) return ""
    board.prices[key]?.let { return it }
    return if (key in board.unavailable) "—" else ""
}
