package com.optiontracker.app.ui.close

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.AssignOutcome
import com.optiontracker.app.data.AssignedLotRepository
import com.optiontracker.app.data.CloseOutcome
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CloseMode { CASH, ASSIGNED }

data class CloseUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val notOpen: Boolean = false,
    val position: Position? = null,
    val canAssign: Boolean = false,
    val mode: CloseMode = CloseMode.CASH,
    val exitDate: LocalDate = LocalDate.now(),
    val exitPremium: String = "",
    val exitFees: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
)

sealed interface CloseEvent {
    data object Closed : CloseEvent
    data object Assigned : CloseEvent
}

class ClosePositionViewModel(
    private val repository: PositionRepository,
    private val assignedLots: AssignedLotRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val positionId: Long = savedStateHandle.get<Long>("positionId") ?: -1L
    private val openOnAssigned: Boolean = savedStateHandle.get<Boolean>("assigned") ?: false

    private val _state = MutableStateFlow(CloseUiState())
    val state: StateFlow<CloseUiState> = _state

    private val _events = MutableSharedFlow<CloseEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CloseEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val position = repository.getPosition(positionId)
            _state.update {
                when {
                    position == null -> it.copy(loading = false, missing = true)
                    position.status != PositionStatus.OPEN -> it.copy(loading = false, notOpen = true, position = position)
                    else -> {
                        val canAssign = position.side == OptionSide.SELL && position.type == OptionType.PUT
                        it.copy(
                            loading = false,
                            position = position,
                            canAssign = canAssign,
                            mode = if (canAssign && openOnAssigned) CloseMode.ASSIGNED else CloseMode.CASH,
                        )
                    }
                }
            }
        }
    }

    fun onExitDate(value: LocalDate) = _state.update { it.copy(exitDate = value, errors = emptyMap()) }
    fun onExitPremium(value: String) = _state.update { it.copy(exitPremium = value, errors = emptyMap()) }
    fun onExitFees(value: String) = _state.update { it.copy(exitFees = value, errors = emptyMap()) }
    fun onMode(value: CloseMode) = _state.update { it.copy(mode = value, errors = emptyMap()) }

    fun close() {
        val current = _state.value
        val position = current.position ?: return
        if (current.mode == CloseMode.ASSIGNED) {
            assign(position)
            return
        }
        val errors = PositionValidator.validateClose(current.exitPremium, current.exitFees)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val premium = Money.parseCents(current.exitPremium) ?: return
        val fees = PositionValidator.parseOptionalFees(current.exitFees) ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (repository.closePosition(position.id, premium, fees, current.exitDate)) {
                CloseOutcome.Closed -> _events.emit(CloseEvent.Closed)
                CloseOutcome.NotFound -> _state.update { it.copy(saving = false, missing = true) }
                CloseOutcome.NotOpen -> _state.update { it.copy(saving = false, notOpen = true) }
            }
        }
    }

    private fun assign(position: Position) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap()) }
            when (assignedLots.assignShortPut(position.id, _state.value.exitDate)) {
                is AssignOutcome.Assigned -> _events.emit(CloseEvent.Assigned)
                AssignOutcome.NotFound -> _state.update { it.copy(saving = false, missing = true) }
                AssignOutcome.NotOpen -> _state.update { it.copy(saving = false, notOpen = true) }
                AssignOutcome.NotShortPut -> _state.update {
                    it.copy(saving = false, mode = CloseMode.CASH, canAssign = false)
                }
            }
        }
    }
}
