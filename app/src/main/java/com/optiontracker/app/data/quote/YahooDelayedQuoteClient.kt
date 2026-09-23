package com.optiontracker.app.data.quote

import com.optiontracker.app.domain.quote.ContractQuoteSource
import com.optiontracker.app.domain.quote.UnderlyingQuoteSource
import com.optiontracker.app.domain.quote.YahooOptionQuotes
import com.optiontracker.app.domain.quote.YahooSparkQuotes
import com.optiontracker.app.domain.quote.quoteKey
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Delayed stock prices and option premiums. Failures return no prices so the list can keep going. */
class YahooDelayedQuoteClient : UnderlyingQuoteSource, ContractQuoteSource {
    override suspend fun fetch(tickers: Set<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val keys = tickers.map { quoteKey(it) }.filter { it.isNotEmpty() }.distinct()
        if (keys.isEmpty()) return@withContext emptyMap()
        val merged = linkedMapOf<String, String>()
        for (chunk in keys.chunked(40)) {
            val byYahoo = chunk.groupBy { YahooSparkQuotes.requestSymbol(it) }
            val body = read(YahooSparkQuotes.endpoint(byYahoo.keys.toList())) ?: continue
            val parsed = YahooSparkQuotes.parse(body)
            for ((yahoo, owners) in byYahoo) {
                val price = parsed[yahoo] ?: continue
                owners.forEach { merged[it] = price }
            }
        }
        merged
    }

    override suspend fun fetchPremiums(symbols: Set<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val keys = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        if (keys.isEmpty()) return@withContext emptyMap()
        val merged = linkedMapOf<String, String>()
        for (chunk in keys.chunked(40)) {
            val body = read(YahooSparkQuotes.endpoint(chunk)) ?: continue
            merged.putAll(YahooOptionQuotes.parse(body))
        }
        merged
    }

    private fun read(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "OptionTracker")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
