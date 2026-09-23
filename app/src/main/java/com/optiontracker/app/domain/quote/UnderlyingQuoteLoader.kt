package com.optiontracker.app.domain.quote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One fetch per unique ticker, reused for [YahooSparkQuotes.cacheMillis] unless forced.
 * A failed fetch keeps the last price. Tickers with no price become unavailable.
 */
class UnderlyingQuoteLoader(
    private val source: UnderlyingQuoteSource,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val gate = Mutex()
    private val prices = linkedMapOf<String, String>()
    private val unavailable = linkedSetOf<String>()
    private var fetchedAt = 0L

    fun board(): QuoteBoard = QuoteBoard(prices.toMap(), unavailable.toSet())

    suspend fun refresh(tickers: Collection<String>, force: Boolean): QuoteBoard = gate.withLock {
        val keys = tickers.map { quoteKey(it) }.filter { it.isNotEmpty() }.distinct()
        val now = nowMillis()
        val warm = !force && fetchedAt != 0L && now - fetchedAt < YahooSparkQuotes.cacheMillis()
        val needed = if (warm) {
            keys.filter { it !in prices && it !in unavailable }
        } else {
            keys
        }
        if (needed.isEmpty()) return board()
        val fetched = try {
            source.fetch(needed.toSet())
        } catch (_: Exception) {
            emptyMap()
        }
        for (key in needed) {
            val price = fetched[key]
            if (price != null) {
                prices[key] = price
                unavailable.remove(key)
            } else if (key !in prices) {
                unavailable.add(key)
            }
        }
        fetchedAt = now
        board()
    }
}
