package com.optiontracker.app.ui.positions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.Position
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PositionsViewModel(
    repository: PositionRepository,
) : ViewModel() {
    val positions: StateFlow<List<Position>> = repository.observeOpenPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
