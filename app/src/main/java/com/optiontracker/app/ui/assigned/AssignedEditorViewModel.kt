package com.optiontracker.app.ui.assigned

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.AssignedLotRepository
import com.optiontracker.app.domain.model.AssignedLot
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.validation.AssignedLotValidator
import com.optiontracker.app.domain.validation.Fields
import com.optiontracker.app.domain.validation.ShareEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignedEditorState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val editing: Boolean = false,
    val ticker: String = "",
    val costBasis: String = "",
    val quantity: String = "",
    val entry: ShareEntry = ShareEntry.SHARES,
    val assignedOn: LocalDate = LocalDate.now(),
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
)

sealed interface AssignedEditorEvent {
    data object Saved : AssignedEditorEvent
    data object Deleted : AssignedEditorEvent
}

class AssignedEditorViewModel(
    private val repository: AssignedLotRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val lotId: Long = savedStateHandle.get<Long>("lotId") ?: -1L

    private val _state = MutableStateFlow(AssignedEditorState(editing = lotId > 0L))
    val state: StateFlow<AssignedEditorState> = _state

    private val _events = MutableSharedFlow<AssignedEditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AssignedEditorEvent> = _events.asSharedFlow()

    init {
        if (lotId <= 0L) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                val lot = repository.get(lotId)
                _state.update {
                    if (lot == null) {
                        it.copy(loading = false, missing = true)
                    } else {
                        it.copy(
                            loading = false,
                            editing = true,
                            ticker = lot.ticker,
                            costBasis = Money.toInput(lot.costBasisCents),
                            quantity = lot.shares.toString(),
                            entry = ShareEntry.SHARES,
                            assignedOn = lot.assignedOn,
                        )
                    }
                }
            }
        }
    }

    fun onTicker(value: String) = _state.update { it.copy(ticker = value, errors = emptyMap()) }
    fun onCostBasis(value: String) = _state.update { it.copy(costBasis = value, errors = emptyMap()) }
    fun onQuantity(value: String) = _state.update { it.copy(quantity = value, errors = emptyMap()) }
    fun onEntry(value: ShareEntry) = _state.update { it.copy(entry = value, errors = emptyMap()) }
    fun onDate(value: LocalDate) = _state.update { it.copy(assignedOn = value, errors = emptyMap()) }

    fun save() {
        val current = _state.value
        val errors = AssignedLotValidator.validate(
            ticker = current.ticker,
            costBasisText = current.costBasis,
            quantityText = current.quantity,
            entry = current.entry,
            assignedOn = current.assignedOn,
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val shares = AssignedLotValidator.sharesFrom(current.quantity.trim().toIntOrNull(), current.entry) ?: return
        val cost = Money.parseCents(current.costBasis) ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val existing = if (lotId > 0L) repository.get(lotId) else null
            if (lotId > 0L && existing == null) {
                _state.update { it.copy(saving = false, missing = true) }
                return@launch
            }
            repository.save(
                AssignedLot(
                    id = existing?.id ?: 0L,
                    ticker = current.ticker,
                    costBasisCents = cost,
                    shares = shares,
                    assignedOn = current.assignedOn,
                    sourcePositionId = existing?.sourcePositionId,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: 0L,
                    updatedAtEpochMillis = existing?.updatedAtEpochMillis ?: 0L,
                ),
            )
            _events.emit(AssignedEditorEvent.Saved)
        }
    }

    fun delete() {
        if (lotId <= 0L) return
        viewModelScope.launch {
            repository.delete(lotId)
            _events.emit(AssignedEditorEvent.Deleted)
        }
    }
}
