package com.optiontracker.app.ui.positions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.quote.ContractQuoteSource
import com.optiontracker.app.domain.quote.OccSymbol
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
    contracts: ContractQuoteSource,
) : ViewModel() {
    val positions: StateFlow<List<Position>> = repository.observeOpenPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val loader = UnderlyingQuoteLoader(quotes)
    private val contractLoader = UnderlyingQuoteLoader(
        source = { symbols -> contracts.fetchPremiums(symbols) },
    )
    private val _quotes = MutableStateFlow(QuoteBoard())
    val quotes: StateFlow<QuoteBoard> = _quotes
    private val _contracts = MutableStateFlow(QuoteBoard())
    val contracts: StateFlow<QuoteBoard> = _contracts

    init {
        viewModelScope.launch {
            positions.collect { rows ->
                refresh(rows, force = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refresh(positions.value, force = true)
        }
    }

    private suspend fun refresh(rows: List<Position>, force: Boolean) {
        if (rows.isEmpty() && !force) return
        _quotes.value = _quotes.value.copy(refreshing = true)
        _contracts.value = _contracts.value.copy(refreshing = true)
        _quotes.value = loader.refresh(rows.map { it.ticker }, force)
        _contracts.value = contractLoader.refresh(rows.mapNotNull { contractSymbol(it) }, force)
    }
}

fun contractSymbol(position: Position): String? =
    OccSymbol.yahoo(position.ticker, position.type, position.strikeCents, position.expiry)
