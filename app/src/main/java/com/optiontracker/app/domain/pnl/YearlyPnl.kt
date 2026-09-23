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
 * total stays on the close date: an imported close date is the expiration, and
 * the year figure was defined that way. Open positions are ignored. A stored
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
