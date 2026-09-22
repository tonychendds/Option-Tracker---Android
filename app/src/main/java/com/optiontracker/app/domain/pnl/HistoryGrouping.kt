package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.Position
import java.time.LocalDate
import java.time.YearMonth

data class HistoryMonth(
    val yearMonth: YearMonth,
    val totalPnlCents: Long,
    val trades: List<Position>,
)

fun groupClosedTrades(closed: List<Position>, tickerQuery: String): List<HistoryMonth> {
    val query = tickerQuery.trim()
    val filtered = if (query.isEmpty()) {
        closed
    } else {
        closed.filter { it.ticker.contains(query, ignoreCase = true) }
    }
    return filtered
        .mapNotNull { position -> position.closedOn?.let { closedOn -> closedOn to position } }
        .groupBy({ YearMonth.from(it.first) }, { it.second })
        .entries
        .sortedByDescending { it.key }
        .map { (month, trades) ->
            val ordered = trades.sortedWith(
                compareByDescending<Position> { it.closedOn ?: LocalDate.MIN }
                    .thenByDescending { it.id },
            )
            HistoryMonth(
                yearMonth = month,
                totalPnlCents = ordered.sumOf { it.realizedPnlCents() ?: 0L },
                trades = ordered,
            )
        }
}
