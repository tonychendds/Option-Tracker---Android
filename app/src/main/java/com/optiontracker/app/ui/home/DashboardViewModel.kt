package com.optiontracker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.pnl.DashboardSummary
import com.optiontracker.app.domain.pnl.buildDashboardSummary
import com.optiontracker.app.ui.ReportYearStore
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(val summary: DashboardSummary) : DashboardUiState
}

class DashboardViewModel(
    repository: PositionRepository,
    private val reportYear: ReportYearStore,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeOpenPositions(),
        repository.observeClosedPositions(),
        reportYear.year,
    ) { open, closed, year ->
        DashboardUiState.Ready(
            buildDashboardSummary(open, closed, LocalDate.now(), selectedYear = year),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState.Loading,
    )

    fun selectYear(year: Int) {
        reportYear.select(year)
    }
}
