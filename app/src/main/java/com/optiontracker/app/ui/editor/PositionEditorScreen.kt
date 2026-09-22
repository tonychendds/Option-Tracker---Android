package com.optiontracker.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.validation.Fields
import com.optiontracker.app.ui.components.DateField
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun PositionEditorRoute(
    viewModel: PositionEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is EditorEvent.Saved) onBack()
        }
    }
    PositionEditorScreen(
        state = state,
        onBack = onBack,
        onTicker = viewModel::onTicker,
        onSide = viewModel::onSide,
        onType = viewModel::onType,
        onStrike = viewModel::onStrike,
        onExpiry = viewModel::onExpiry,
        onContracts = viewModel::onContracts,
        onPremium = viewModel::onPremium,
        onFees = viewModel::onFees,
        onOpenedOn = viewModel::onOpenedOn,
        onNotes = viewModel::onNotes,
        onSave = viewModel::save,
    )
}

@Composable
fun PositionEditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onTicker: (String) -> Unit,
    onSide: (OptionSide) -> Unit,
    onType: (OptionType) -> Unit,
    onStrike: (String) -> Unit,
    onExpiry: (java.time.LocalDate) -> Unit,
    onContracts: (String) -> Unit,
    onPremium: (String) -> Unit,
    onFees: (String) -> Unit,
    onOpenedOn: (java.time.LocalDate) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
) {
    TrackerScaffold(
        title = if (state.editing) "Edit position" else "Add position",
        onBack = onBack,
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.missing -> {
                    EmptyState(
                        title = "Position unavailable",
                        body = "This open position is no longer on the device.",
                        action = { TextButton(onClick = onBack) { Text("Go back") } },
                    )
                }
                else -> EditorForm(
                    state = state,
                    onTicker = onTicker,
                    onSide = onSide,
                    onType = onType,
                    onStrike = onStrike,
                    onExpiry = onExpiry,
                    onContracts = onContracts,
                    onPremium = onPremium,
                    onFees = onFees,
                    onOpenedOn = onOpenedOn,
                    onNotes = onNotes,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun EditorForm(
    state: EditorUiState,
    onTicker: (String) -> Unit,
    onSide: (OptionSide) -> Unit,
    onType: (OptionType) -> Unit,
    onStrike: (String) -> Unit,
    onExpiry: (java.time.LocalDate) -> Unit,
    onContracts: (String) -> Unit,
    onPremium: (String) -> Unit,
    onFees: (String) -> Unit,
    onOpenedOn: (java.time.LocalDate) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Equity options use a 100 share multiplier. A \$1.50 premium on 1 contract is \$150 before fees.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.ticker,
            onValueChange = onTicker,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ticker") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.TICKER),
            supportingText = { Text(state.errors[Fields.TICKER] ?: "Example: AAPL or BRK.B") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        Text("Side", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            OptionSide.entries.forEachIndexed { index, side ->
                SegmentedButton(
                    selected = state.side == side,
                    onClick = { onSide(side) },
                    shape = SegmentedButtonDefaults.itemShape(index, OptionSide.entries.size),
                ) { Text(sideLabel(side)) }
            }
        }
        Text("Type", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            OptionType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.type == type,
                    onClick = { onType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, OptionType.entries.size),
                ) { Text(typeLabel(type)) }
            }
        }
        OutlinedTextField(
            value = state.strike,
            onValueChange = onStrike,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Strike") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.STRIKE),
            supportingText = { Text(state.errors[Fields.STRIKE] ?: "Strike price in USD") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        DateField(
            label = "Expiration",
            date = state.expiry,
            onDate = onExpiry,
            error = state.errors[Fields.EXPIRY],
        )
        OutlinedTextField(
            value = state.contracts,
            onValueChange = onContracts,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contracts") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.CONTRACTS),
            supportingText = { Text(state.errors[Fields.CONTRACTS] ?: "Number of contracts, not shares") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = state.premium,
            onValueChange = onPremium,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Premium per contract") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.PREMIUM),
            supportingText = {
                Text(state.errors[Fields.PREMIUM] ?: "Quoted per share. Total = premium × contracts × 100.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = state.fees,
            onValueChange = onFees,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Fees (optional)") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.FEES),
            supportingText = { Text(state.errors[Fields.FEES] ?: "Total commissions for this opening trade") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        DateField(
            label = "Opened on",
            date = state.openedOn,
            onDate = onOpenedOn,
            error = null,
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotes,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes") },
            minLines = 3,
            supportingText = { Text(state.errors[Fields.NOTES] ?: "Optional") },
        )
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.editing) "Save changes" else "Save position")
        }
    }
}
