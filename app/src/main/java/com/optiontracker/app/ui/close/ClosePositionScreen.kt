package com.optiontracker.app.ui.close

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.OptionPnl
import com.optiontracker.app.domain.validation.Fields
import com.optiontracker.app.domain.validation.PositionValidator
import com.optiontracker.app.ui.components.DateField
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.format.contractsLabel
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun ClosePositionRoute(
    viewModel: ClosePositionViewModel,
    onBack: () -> Unit,
    onClosed: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is CloseEvent.Closed) onClosed()
        }
    }
    ClosePositionScreen(
        state = state,
        onBack = onBack,
        onExitDate = viewModel::onExitDate,
        onExitPremium = viewModel::onExitPremium,
        onExitFees = viewModel::onExitFees,
        onClose = viewModel::close,
    )
}

@Composable
fun ClosePositionScreen(
    state: CloseUiState,
    onBack: () -> Unit,
    onExitDate: (java.time.LocalDate) -> Unit,
    onExitPremium: (String) -> Unit,
    onExitFees: (String) -> Unit,
    onClose: () -> Unit,
) {
    TrackerScaffold(title = "Close position", onBack = onBack) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> CenteredProgress()
                state.missing -> EmptyState(
                    title = "Position not found",
                    body = "It may have been deleted.",
                    action = { TextButton(onClick = onBack) { Text("Go back") } },
                )
                state.notOpen -> EmptyState(
                    title = "Already closed",
                    body = "This trade is already in History.",
                    action = { TextButton(onClick = onBack) { Text("Go back") } },
                )
                else -> CloseForm(state, onExitDate, onExitPremium, onExitFees, onClose)
            }
        }
    }
}

@Composable
private fun CloseForm(
    state: CloseUiState,
    onExitDate: (java.time.LocalDate) -> Unit,
    onExitPremium: (String) -> Unit,
    onExitFees: (String) -> Unit,
    onClose: () -> Unit,
) {
    val position = state.position ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${position.ticker} · ${sideLabel(position.side)} ${contractsLabel(position.contracts)} ${typeLabel(position.type)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Opened ${formatDate(position.openedOn)} at ${Money.format(position.entryPremiumCents)} per share.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (position.side == OptionSide.BUY) {
                "Selling to close. Profit when the exit premium is higher than the entry premium."
            } else {
                "Buying to close. Profit when the exit premium is lower than the entry premium."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateField(label = "Exit date", date = state.exitDate, onDate = onExitDate, error = null)
        OutlinedTextField(
            value = state.exitPremium,
            onValueChange = onExitPremium,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Exit premium per share") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.EXIT_PREMIUM),
            supportingText = {
                Text(state.errors[Fields.EXIT_PREMIUM] ?: "Same quote as entry. Proceeds = premium × contracts × 100.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = state.exitFees,
            onValueChange = onExitFees,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Exit fees (optional)") },
            prefix = { Text("$") },
            singleLine = true,
            isError = state.errors.containsKey(Fields.EXIT_FEES),
            supportingText = { Text(state.errors[Fields.EXIT_FEES] ?: "Total commissions to close") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        PreviewCard(position, state.exitPremium, state.exitFees)
        Button(
            onClick = onClose,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Close position") }
    }
}

@Composable
private fun PreviewCard(position: Position, exitPremiumText: String, exitFeesText: String) {
    val premium = Money.parseCents(exitPremiumText)
    val fees = PositionValidator.parseOptionalFees(exitFeesText)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Realized P/L", style = MaterialTheme.typography.labelLarge)
            if (premium == null || fees == null) {
                Text(
                    "Enter an exit premium to preview profit or loss.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val pnl = OptionPnl.realizedPnlCents(
                    side = position.side,
                    contracts = position.contracts,
                    entryPremiumCents = position.entryPremiumCents,
                    entryFeesCents = position.entryFeesCents,
                    exitPremiumCents = premium,
                    exitFeesCents = fees,
                )
                Text(
                    Money.formatSigned(pnl),
                    style = MaterialTheme.typography.headlineLarge,
                    color = pnlColor(pnl),
                )
                Text(
                    "Includes opening fees of ${Money.format(position.entryFeesCents)} and exit fees of ${Money.format(fees)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CenteredProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}
