package com.optiontracker.app.ui.assigned

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.validation.Fields
import com.optiontracker.app.domain.validation.ShareEntry
import com.optiontracker.app.ui.components.DateField
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import java.time.LocalDate

@Composable
fun AssignedEditorRoute(
    viewModel: AssignedEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is AssignedEditorEvent.Saved || event is AssignedEditorEvent.Deleted) onBack()
        }
    }
    AssignedEditorScreen(
        state = state,
        onBack = onBack,
        onTicker = viewModel::onTicker,
        onCostBasis = viewModel::onCostBasis,
        onQuantity = viewModel::onQuantity,
        onEntry = viewModel::onEntry,
        onDate = viewModel::onDate,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
fun AssignedEditorScreen(
    state: AssignedEditorState,
    onBack: () -> Unit,
    onTicker: (String) -> Unit,
    onCostBasis: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onEntry: (ShareEntry) -> Unit,
    onDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    TrackerScaffold(
        title = if (state.editing) "Edit assignment" else "Add assignment",
        onBack = onBack,
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
                state.missing -> EmptyState(
                    title = "Assignment not found",
                    body = "It may have been deleted.",
                    action = { TextButton(onClick = onBack) { Text("Go back") } },
                )
                else -> AssignedForm(state, onTicker, onCostBasis, onQuantity, onEntry, onDate, onSave, onDelete)
            }
        }
    }
}

@Composable
private fun AssignedForm(
    state: AssignedEditorState,
    onTicker: (String) -> Unit,
    onCostBasis: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onEntry: (ShareEntry) -> Unit,
    onDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Shares from an assigned short put. Cost basis is the strike per share.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.ticker,
            onValueChange = onTicker,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ticker") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.TICKER),
            supportingText = { Text(state.errors[Fields.TICKER] ?: "Underlying symbol") },
        )
        OutlinedTextField(
            value = state.costBasis,
            onValueChange = onCostBasis,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cost basis per share") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.STRIKE),
            supportingText = { Text(state.errors[Fields.STRIKE] ?: "The assigned put’s strike") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ShareEntry.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = state.entry == entry,
                    onClick = { onEntry(entry) },
                    shape = SegmentedButtonDefaults.itemShape(index, ShareEntry.entries.size),
                ) {
                    Text(if (entry == ShareEntry.SHARES) "Shares" else "Contracts")
                }
            }
        }
        OutlinedTextField(
            value = state.quantity,
            onValueChange = onQuantity,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (state.entry == ShareEntry.SHARES) "Shares" else "Contracts") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.CONTRACTS),
            supportingText = {
                Text(
                    state.errors[Fields.CONTRACTS]
                        ?: if (state.entry == ShareEntry.CONTRACTS) {
                            "One contract is 100 shares"
                        } else {
                            "Number of shares you were assigned"
                        },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        DateField(
            label = "Assigned date",
            date = state.assignedOn,
            onDate = onDate,
            error = state.errors[Fields.CLOSED_ON],
        )
        Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.editing) "Save" else "Add assignment")
        }
        if (state.editing) {
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete assignment")
            }
            Text(
                "Deleting removes this stock lot only. It does not reopen the option.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this assignment?") },
            text = { Text("This removes the stock lot. The closed option stays in History.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
