package com.optiontracker.app.ui.assigned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.AssignedLotRepository
import com.optiontracker.app.domain.model.AssignedLot
import com.optiontracker.app.domain.quote.QuoteBoard
import com.optiontracker.app.domain.quote.UnderlyingQuoteLoader
import com.optiontracker.app.domain.quote.UnderlyingQuoteSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssignedViewModel(
    repository: AssignedLotRepository,
    quotes: UnderlyingQuoteSource,
) : ViewModel() {
    val lots: StateFlow<List<AssignedLot>> = repository.observeLots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val loader = UnderlyingQuoteLoader(quotes)
    private val _quotes = MutableStateFlow(QuoteBoard())
    val quotes: StateFlow<QuoteBoard> = _quotes

    init {
        viewModelScope.launch {
            lots.collect { rows ->
                refresh(rows.map { it.ticker }, force = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refresh(lots.value.map { it.ticker }, force = true)
        }
    }

    private suspend fun refresh(tickers: List<String>, force: Boolean) {
        if (tickers.isEmpty() && !force) return
        _quotes.value = _quotes.value.copy(refreshing = true)
        _quotes.value = loader.refresh(tickers, force)
    }
}
