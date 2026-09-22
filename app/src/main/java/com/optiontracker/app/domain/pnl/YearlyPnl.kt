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
 * Calendar years that have a closed trade, plus the current year.
 * Newest first.
 */
fun availableReportYears(positions: List<Position>, today: LocalDate): List<Int> {
    val years = positions.mapNotNull { position ->
        if (position.status != PositionStatus.CLOSED) null else position.closedOn?.year
    }.toMutableSet()
    years += today.year
    return years.sortedDescending()
}

/**
 * Realized P/L for closed trades whose close date falls in [year].
 * Open positions are ignored. A stored spreadsheet override is used when set.
 * The window is the full calendar year, including later months, because an
 * imported close date is the expiration date.
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
