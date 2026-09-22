package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.YearMonth

data class MonthReportRow(
    val yearMonth: YearMonth,
    val totalCents: Long,
    val tradeCount: Int,
    val winningCount: Int,
) {
    /** Share of closed trades with realized P/L above zero. Null when the month has none. */
    val hitRate: Double?
        get() = if (tradeCount == 0) null else winningCount.toDouble() / tradeCount
}

/**
 * January through December for [year].
 *
 * A row includes closed trades whose close date is in that month. Open positions
 * are ignored. Realized P/L uses the spreadsheet override when one is stored.
 * A profit greater than zero is a hit. Zero is not.
 */
fun monthlyReport(positions: List<Position>, year: Int): List<MonthReportRow> {
    val byMonth = positions
        .filter { position ->
            position.status == PositionStatus.CLOSED && position.closedOn?.year == year
        }
        .groupBy { YearMonth.from(it.closedOn) }
    return (1..12).map { month ->
        val yearMonth = YearMonth.of(year, month)
        val trades = byMonth[yearMonth].orEmpty()
        val results = trades.map { it.realizedPnlCents() ?: 0L }
        MonthReportRow(
            yearMonth = yearMonth,
            totalCents = results.sum(),
            tradeCount = trades.size,
            winningCount = results.count { it > 0L },
        )
    }
}
