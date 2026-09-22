package com.optiontracker.app.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.validation.PositionValidator
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loading: Boolean = false,
    val missing: Boolean = false,
    val editing: Boolean = false,
    val ticker: String = "",
    val side: OptionSide = OptionSide.BUY,
    val type: OptionType = OptionType.CALL,
    val strike: String = "",
    val expiry: LocalDate? = null,
    val contracts: String = "1",
    val premium: String = "",
    val fees: String = "",
    val openedOn: LocalDate = LocalDate.now(),
    val notes: String = "",
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
)

sealed interface EditorEvent {
    data object Saved : EditorEvent
}

class PositionEditorViewModel(
    private val repository: PositionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val positionId: Long? = savedStateHandle.get<Long>("positionId")?.takeIf { it > 0L }

    private val _state = MutableStateFlow(
        EditorUiState(loading = positionId != null, editing = positionId != null),
    )
    val state: StateFlow<EditorUiState> = _state

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        val id = positionId
        if (id != null) {
            viewModelScope.launch {
                val position = repository.getPosition(id)
                if (position == null || position.status != PositionStatus.OPEN) {
                    _state.update { it.copy(loading = false, missing = true) }
                } else {
                    _state.update { it.from(position) }
                }
            }
        }
    }

    fun onTicker(value: String) = update { copy(ticker = value.uppercase(Locale.US).take(10)) }
    fun onSide(value: OptionSide) = update { copy(side = value) }
    fun onType(value: OptionType) = update { copy(type = value) }
    fun onStrike(value: String) = update { copy(strike = value) }
    fun onExpiry(value: LocalDate) = update { copy(expiry = value) }
    fun onContracts(value: String) = update { copy(contracts = value.filter(Char::isDigit).take(6)) }
    fun onPremium(value: String) = update { copy(premium = value) }
    fun onFees(value: String) = update { copy(fees = value) }
    fun onOpenedOn(value: LocalDate) = update { copy(openedOn = value) }
    fun onNotes(value: String) = update { copy(notes = value.take(PositionValidator.MAX_NOTES)) }

    fun save() {
        val current = _state.value
        val errors = PositionValidator.validateEntry(
            ticker = current.ticker,
            strikeText = current.strike,
            expiry = current.expiry,
            contractsText = current.contracts,
            premiumText = current.premium,
            feesText = current.fees,
            notes = current.notes,
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        val expiry = current.expiry ?: return
        val strike = Money.parseCents(current.strike) ?: return
        val contracts = current.contracts.toIntOrNull() ?: return
        val premium = Money.parseCents(current.premium) ?: return
        val fees = PositionValidator.parseOptionalFees(current.fees) ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errors = emptyMap()) }
            repository.save(
                Position(
                    id = positionId ?: 0L,
                    ticker = current.ticker,
                    side = current.side,
                    type = current.type,
                    strikeCents = strike,
                    expiry = expiry,
                    contracts = contracts,
                    entryPremiumCents = premium,
                    entryFeesCents = fees,
                    openedOn = current.openedOn,
                    notes = current.notes,
                    status = PositionStatus.OPEN,
                    exitPremiumCents = null,
                    exitFeesCents = null,
                    closedOn = null,
                    createdAtEpochMillis = 0L,
                    updatedAtEpochMillis = 0L,
                ),
            )
            _events.emit(EditorEvent.Saved)
        }
    }

    private fun update(transform: EditorUiState.() -> EditorUiState) {
        _state.update { current ->
            transform(current).copy(errors = emptyMap())
        }
    }

    private fun EditorUiState.from(position: Position) = copy(
        loading = false,
        missing = false,
        editing = true,
        ticker = position.ticker,
        side = position.side,
        type = position.type,
        strike = Money.toInput(position.strikeCents),
        expiry = position.expiry,
        contracts = position.contracts.toString(),
        premium = Money.toInput(position.entryPremiumCents),
        fees = if (position.entryFeesCents == 0L) "" else Money.toInput(position.entryFeesCents),
        openedOn = position.openedOn,
        notes = position.notes,
        errors = emptyMap(),
        saving = false,
    )
}
