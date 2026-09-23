package com.optiontracker.app.ui.symbols

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.pnl.SymbolPeriod
import com.optiontracker.app.domain.pnl.SymbolSort
import com.optiontracker.app.domain.pnl.SymbolTotal
import com.optiontracker.app.domain.pnl.availableReportYears
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.domain.pnl.symbolTotals
import com.optiontracker.app.domain.pnl.tradesForSymbol
import com.optiontracker.app.ui.ReportYearStore
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SymbolsUiState(
    val period: SymbolPeriod = SymbolPeriod.YTD,
    val sort: SymbolSort = SymbolSort.PNL,
    val year: Int = LocalDate.now().year,
    val availableYears: List<Int> = emptyList(),
    val rows: List<SymbolTotal> = emptyList(),
)

class SymbolsViewModel(
    repository: PositionRepository,
    private val reportYear: ReportYearStore,
) : ViewModel() {
    private val period = MutableStateFlow(SymbolPeriod.YTD)
    private val sort = MutableStateFlow(SymbolSort.PNL)

    val uiState: StateFlow<SymbolsUiState> = combine(
        repository.observeClosedPositions(),
        reportYear.year,
        period,
        sort,
    ) { closed, year, selectedPeriod, selectedSort ->
        val today = LocalDate.now()
        val reportYearValue = year
        SymbolsUiState(
            period = selectedPeriod,
            sort = selectedSort,
            year = reportYearValue,
            availableYears = (availableReportYears(closed, today) + reportYearValue).distinct().sortedDescending(),
            rows = symbolTotals(
                positions = closed,
                year = if (selectedPeriod == SymbolPeriod.YTD) reportYearValue else null,
                sort = selectedSort,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SymbolsUiState())

    fun selectPeriod(value: SymbolPeriod) {
        period.value = value
    }

    fun selectSort(value: SymbolSort) {
        sort.value = value
    }

    fun selectYear(year: Int) {
        reportYear.select(year)
    }
}

data class SymbolTradesUiState(
    val ticker: String = "",
    val periodLabel: String = "",
    val trades: List<Position> = emptyList(),
    val totalCents: Long = 0L,
)

class SymbolTradesViewModel(
    repository: PositionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ticker: String = savedStateHandle.get<String>("ticker").orEmpty()
    private val year: Int = savedStateHandle.get<Int>("year") ?: LocalDate.now().year
    private val allTime: Boolean = savedStateHandle.get<Boolean>("allTime") ?: false

    val uiState: StateFlow<SymbolTradesUiState> = repository.observeClosedPositions()
        .map { closed ->
            val trades = tradesForSymbol(closed, ticker, if (allTime) null else year)
            SymbolTradesUiState(
                ticker = ticker,
                periodLabel = if (allTime) "All closed trades" else "$year YTD · close date",
                trades = trades,
                totalCents = trades.sumOf { it.realizedPnlCents() ?: 0L },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SymbolTradesUiState(ticker = ticker))
}
