package com.optiontracker.app.ui.positions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.quote.QuoteBoard
import com.optiontracker.app.domain.quote.UnderlyingQuoteLoader
import com.optiontracker.app.domain.quote.UnderlyingQuoteSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PositionsViewModel(
    repository: PositionRepository,
    quotes: UnderlyingQuoteSource,
) : ViewModel() {
    val positions: StateFlow<List<Position>> = repository.observeOpenPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val loader = UnderlyingQuoteLoader(quotes)
    private val _quotes = MutableStateFlow(QuoteBoard())
    val quotes: StateFlow<QuoteBoard> = _quotes

    init {
        viewModelScope.launch {
            positions.collect { rows ->
                refresh(rows.map { it.ticker }, force = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refresh(positions.value.map { it.ticker }, force = true)
        }
    }

    private suspend fun refresh(tickers: List<String>, force: Boolean) {
        if (tickers.isEmpty() && !force) return
        _quotes.value = _quotes.value.copy(refreshing = true)
        _quotes.value = loader.refresh(tickers, force)
    }
}
