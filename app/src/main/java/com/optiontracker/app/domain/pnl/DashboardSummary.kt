package com.optiontracker.app.domain.pnl

import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import java.time.LocalDate
import java.time.YearMonth

enum class ActivityKind {
    OPENED,
    CLOSED,
}

data class RecentActivity(
    val position: Position,
    val kind: ActivityKind,
    val date: LocalDate,
)

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
    val recent: List<RecentActivity>,
)

fun buildDashboardSummary(
    open: List<Position>,
    closed: List<Position>,
    today: LocalDate,
    recentLimit: Int = 5,
): DashboardSummary {
    val month = YearMonth.from(today)
    val closedThisMonth = closed.filter { position ->
        position.closedOn?.let { YearMonth.from(it) == month } == true
    }
    val recent = (
        open.map { RecentActivity(it, ActivityKind.OPENED, it.openedOn) } +
            closed.mapNotNull { position ->
                position.closedOn?.let { RecentActivity(position, ActivityKind.CLOSED, it) }
            }
        )
        .sortedWith(
            compareByDescending<RecentActivity> { it.date }
                .thenByDescending { it.position.updatedAtEpochMillis }
                .thenByDescending { it.position.id },
        )
        .take(recentLimit)

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
        recent = recent,
    )
}
