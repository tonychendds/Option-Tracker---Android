package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate

data class YearRealizedTotal(
    val year: Int,
    val totalCents: Long,
    val tradeCount: Int,
)

/**
 * Years to offer on Home and History, newest first.
 *
 * Includes the current year, each year a trade was closed, and each year a
 * closed trade was opened. Month lists use the open year, so a year that only
 * contains opens still has to be selectable.
 */
fun availableReportYears(positions: List<Position>, today: LocalDate): List<Int> {
    val closedTrades = positions.filter { it.status == PositionStatus.CLOSED }
    val years = closedTrades.mapNotNull { it.closedOn?.year }.toMutableSet()
    years += closedTrades.map { it.openedOn.year }
    years += today.year
    return years.sortedDescending()
}

/**
 * Realized P/L for closed trades whose **close** date falls in [year].
 *
 * Month reports attribute that same P/L to the open month instead. This year
 * total stays on the close date. An import that has no closeDate column, or a
 * blank close date, still uses the expiration as the close date. A file that
 * includes closeDate uses that date. Open positions are ignored. A stored
 * spreadsheet override is used when set. The window is the full calendar year.
 */
fun realizedYearTotal(positions: List<Position>, year: Int): YearRealizedTotal {
    val matching = positions.filter { position ->
        position.status == PositionStatus.CLOSED && position.closedOn?.year == year
    }
    return YearRealizedTotal(
        year = year,
        totalCents = matching.sumOf { it.realizedPnlCents() ?: 0L },
        tradeCount = matching.size,
    )
}

fun ytdLabel(year: Int): String = "$year YTD"
