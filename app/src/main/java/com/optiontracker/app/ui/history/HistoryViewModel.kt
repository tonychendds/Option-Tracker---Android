package com.optiontracker.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.pnl.HistoryMonth
import com.optiontracker.app.domain.pnl.groupClosedTrades
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
)

class HistoryViewModel(
    repository: PositionRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeClosedPositions(),
        query,
    ) { closed, ticker ->
        HistoryUiState(
            query = ticker,
            months = groupClosedTrades(closed, ticker),
            hasAnyClosed = closed.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQuery(value: String) {
        query.value = value.uppercase(Locale.US).take(10)
    }
}
