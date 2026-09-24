package com.optiontracker.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.pnl.HistoryMonth
import com.optiontracker.app.domain.pnl.availableReportYears
import com.optiontracker.app.domain.pnl.MonthReportRow
import com.optiontracker.app.domain.pnl.groupClosedTrades
import com.optiontracker.app.domain.pnl.monthlyReport
import com.optiontracker.app.domain.pnl.realizedYearTotal
import com.optiontracker.app.ui.ReportYearStore
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val query: String = "",
    val months: List<HistoryMonth> = emptyList(),
    val hasAnyClosed: Boolean = false,
    val selectedYear: Int = LocalDate.now().year,
    val availableYears: List<Int> = emptyList(),
    val yearTotalCents: Long = 0L,
    val yearTradeCount: Int = 0,
    val monthlyReport: List<MonthReportRow> = emptyList(),
)

class HistoryViewModel(
    repository: PositionRepository,
    private val reportYear: ReportYearStore,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeClosedPositions(),
        query,
        reportYear.year,
    ) { closed, ticker, year ->
        val today = LocalDate.now()
        val years = (availableReportYears(closed, today) + year).distinct().sortedDescending()
        val filtered = if (ticker.isBlank()) {
            closed
        } else {
            closed.filter { it.ticker.contains(ticker.trim(), ignoreCase = true) }
        }
        val months = groupClosedTrades(filtered, "").filter { it.yearMonth.year == year }
        val yearTotal = realizedYearTotal(filtered, year)
        HistoryUiState(
            query = ticker,
            months = months,
            hasAnyClosed = closed.isNotEmpty(),
            selectedYear = year,
            availableYears = years,
            yearTotalCents = yearTotal.totalCents,
            yearTradeCount = yearTotal.tradeCount,
            monthlyReport = monthlyReport(filtered, year),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQuery(value: String) {
        query.value = value.uppercase(Locale.US).take(10)
    }

    fun selectYear(year: Int) {
        reportYear.select(year)
    }
}
