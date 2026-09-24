package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import kotlin.math.abs

enum class SymbolPeriod {
    YTD,
    ALL,
}

enum class SymbolSort {
    /** Largest winners and losers first. */
    PNL,
    NAME,
}

data class SymbolTotal(
    val ticker: String,
    val totalCents: Long,
    val tradeCount: Int,
    val winningCount: Int,
    val losingCount: Int,
) {
    /** Share of trades with realized P/L above zero. Zero is not a win. */
    val hitRate: Double?
        get() = if (tradeCount == 0) null else winningCount.toDouble() / tradeCount
}

/**
 * Closed-option realized P/L rolled up by ticker.
 *
 * [year] null means all closed trades. A year uses the **close** date, the same
 * window as [realizedYearTotal]. A stored override replaces the calculated P/L.
 * Open positions are ignored. Assigned stock lots are not included.
 */
fun symbolTotals(
    positions: List<Position>,
    year: Int?,
    sort: SymbolSort = SymbolSort.PNL,
): List<SymbolTotal> {
    val rows = positions
        .filter { it.status == PositionStatus.CLOSED && (year == null || it.closedOn?.year == year) }
        .groupBy { it.ticker }
        .map { (ticker, trades) ->
            val results = trades.map { it.realizedPnlCents() ?: 0L }
            SymbolTotal(
                ticker = ticker,
                totalCents = results.sum(),
                tradeCount = trades.size,
                winningCount = results.count { it > 0L },
                losingCount = results.count { it < 0L },
            )
        }
    return when (sort) {
        SymbolSort.PNL -> rows.sortedWith(
            compareByDescending<SymbolTotal> { abs(it.totalCents) }.thenBy { it.ticker },
        )
        SymbolSort.NAME -> rows.sortedBy { it.ticker }
    }
}

fun tradesForSymbol(positions: List<Position>, ticker: String, year: Int?): List<Position> =
    positions.filter { position ->
        position.status == PositionStatus.CLOSED &&
            position.ticker.equals(ticker, ignoreCase = true) &&
            (year == null || position.closedOn?.year == year)
    }
