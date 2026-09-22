package com.optiontracker.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.Position
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Missing : DetailUiState
    data class Ready(val position: Position) : DetailUiState
}

sealed interface DetailEvent {
    data object Deleted : DetailEvent
}

class PositionDetailViewModel(
    private val repository: PositionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val positionId: Long = savedStateHandle.get<Long>("positionId") ?: -1L

    val uiState: StateFlow<DetailUiState> = repository.observePosition(positionId)
        .map { position ->
            if (position == null) DetailUiState.Missing else DetailUiState.Ready(position)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private val _events = MutableSharedFlow<DetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    fun delete() {
        viewModelScope.launch {
            repository.delete(positionId)
            _events.emit(DetailEvent.Deleted)
        }
    }
}
