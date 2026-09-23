package com.optiontracker.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.OptionPnl
import com.optiontracker.app.domain.pnl.entryCashFlowCents
import com.optiontracker.app.domain.pnl.entryNotionalCents
import com.optiontracker.app.domain.pnl.exitNotionalCents
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.ui.components.DetailRow
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.format.contractsLabel
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel

@Composable
fun PositionDetailRoute(
    viewModel: PositionDetailViewModel,
    readOnly: Boolean,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    onClose: (Long) -> Unit = {},
    onAssign: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is DetailEvent.Deleted) onBack()
        }
    }
    PositionDetailScreen(
        state = state,
        readOnly = readOnly,
        onBack = onBack,
        onEdit = onEdit,
        onClose = onClose,
        onAssign = onAssign,
        onDelete = viewModel::delete,
    )
}

@Composable
fun PositionDetailScreen(
    state: DetailUiState,
    readOnly: Boolean,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onAssign: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    val title = when {
        readOnly -> "Closed trade"
        else -> "Position"
    }
    TrackerScaffold(title = title, onBack = onBack) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            when (state) {
                DetailUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator() }
                }
                DetailUiState.Missing -> {
                    EmptyState(
                        title = "Position not found",
                        body = "It may have been deleted.",
                        action = { TextButton(onClick = onBack) { Text("Go back") } },
                    )
                }
                is DetailUiState.Ready -> PositionBody(
                    position = state.position,
                    readOnly = readOnly,
                    onEdit = { onEdit(state.position.id) },
                    onClose = { onClose(state.position.id) },
                    onAssign = { onAssign(state.position.id) },
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun PositionBody(
    position: Position,
    readOnly: Boolean,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val open = position.status == PositionStatus.OPEN
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(position.ticker, style = MaterialTheme.typography.headlineLarge)
        Text(
            "${sideLabel(position.side)} ${contractsLabel(position.contracts)} ${typeLabel(position.type)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!open) {
            val pnl = position.realizedPnlCents() ?: 0L
            Text("Realized P/L", style = MaterialTheme.typography.labelLarge)
            Text(
                Money.formatSigned(pnl),
                style = MaterialTheme.typography.headlineLarge,
                color = pnlColor(pnl),
            )
            Text(
                if (position.realizedOverrideCents != null) {
                    "This is the realized P/L override. Entry and exit premiums are kept, but they are not used for this result."
                } else {
                    "Entry versus exit premium, times contracts, times 100, minus fees."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Premium cash flow", style = MaterialTheme.typography.labelLarge)
            Text(
                Money.formatSigned(position.entryCashFlowCents()),
                style = MaterialTheme.typography.headlineLarge,
                color = pnlColor(position.entryCashFlowCents()),
            )
            Text(
                "Cash when you opened this trade. Credits are positive. This is not a live profit or loss.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DetailRow("Status", if (open) "Open" else "Closed")
        if (position.account.isNotBlank()) {
            DetailRow("Account", position.account)
        }
        DetailRow("Type", typeLabel(position.type))
        DetailRow("Side", sideLabel(position.side))
        DetailRow("Strike", Money.format(position.strikeCents))
        DetailRow("Expiration", formatDate(position.expiry))
        DetailRow("Contracts", position.contracts.toString())
        DetailRow("Premium per share", Money.format(position.entryPremiumCents))
        DetailRow("Multiplier", "× ${OptionPnl.EQUITY_CONTRACT_MULTIPLIER}")
        DetailRow("Notional premium", Money.format(position.entryNotionalCents()))
        DetailRow("Opening fees", Money.format(position.entryFeesCents))
        DetailRow("Opened", formatDate(position.openedOn))
        if (!open) {
            DetailRow("Closed", position.closedOn?.let(::formatDate) ?: "—")
            DetailRow(
                "Exit premium per share",
                position.exitPremiumCents?.let(Money::format) ?: "—",
            )
            DetailRow("Exit fees", Money.format(position.exitFeesCents ?: 0L))
            DetailRow("Exit notional", Money.format(position.exitNotionalCents() ?: 0L))
        }
        Text("Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            position.notes.ifBlank { "No notes" },
            style = MaterialTheme.typography.bodyLarge,
            color = if (position.notes.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (open && !readOnly) {
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit") }
            if (position.side == OptionSide.SELL && position.type == OptionType.PUT) {
                Button(onClick = onAssign, modifier = Modifier.fillMaxWidth()) { Text("Assigned") }
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close position") }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete")
            }
        } else if (!open) {
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit") }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete")
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this position?") },
            text = { Text("This removes it from your tracker. This cannot be undone.") },
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
