package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import java.time.LocalDate
import java.time.YearMonth

data class DashboardSummary(
    val openCount: Int,
    val openContracts: Int,
    val netPremiumCents: Long,
    val callContracts: Int,
    val putContracts: Int,
    val longContracts: Int,
    val shortContracts: Int,
    val realizedThisMonthCents: Long,
    val closedThisMonthCount: Int,
    val selectedYear: Int,
    val availableYears: List<Int>,
    val realizedYearCents: Long,
    val closedYearCount: Int,
    /** Closed trades grouped by the month they were opened. January through December, or through [today]'s month when [selectedYear] is the current year. */
    val monthlyRealized: List<MonthReportRow>,
)

fun buildDashboardSummary(
    open: List<Position>,
    closed: List<Position>,
    today: LocalDate,
    selectedYear: Int = today.year,
): DashboardSummary {
    val month = YearMonth.from(today)
    val closedThisMonth = closed.filter { position ->
        position.status == PositionStatus.CLOSED && YearMonth.from(position.openedOn) == month
    }
    val yearTotal = realizedYearTotal(closed, selectedYear)
    val years = (availableReportYears(closed, today) + selectedYear).distinct().sortedDescending()
    val throughMonth = if (selectedYear == today.year) today.monthValue else 12
    val monthlyRealized = monthlyReport(closed, selectedYear).take(throughMonth)

    return DashboardSummary(
        openCount = open.size,
        openContracts = open.sumOf { it.contracts },
        netPremiumCents = open.sumOf { it.entryCashFlowCents() },
        callContracts = open.filter { it.type == OptionType.CALL }.sumOf { it.contracts },
        putContracts = open.filter { it.type == OptionType.PUT }.sumOf { it.contracts },
        longContracts = open.filter { it.side == OptionSide.BUY }.sumOf { it.contracts },
        shortContracts = open.filter { it.side == OptionSide.SELL }.sumOf { it.contracts },
        realizedThisMonthCents = closedThisMonth.sumOf { it.realizedPnlCents() ?: 0L },
        closedThisMonthCount = closedThisMonth.size,
        selectedYear = selectedYear,
        availableYears = years,
        realizedYearCents = yearTotal.totalCents,
        closedYearCount = yearTotal.tradeCount,
        monthlyRealized = monthlyRealized,
    )
}
