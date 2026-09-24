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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.OptionSide
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.AssignedPnl
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
import com.optiontracker.app.ui.format.exitPremiumCashFlowHint
import com.optiontracker.app.ui.format.exitPremiumCashFlowLabel
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun ClosePositionRoute(
    viewModel: ClosePositionViewModel,
    onBack: () -> Unit,
    onClosed: () -> Unit,
    onAssigned: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CloseEvent.Closed -> onClosed()
                CloseEvent.Assigned -> onAssigned()
            }
        }
    }
    ClosePositionScreen(
        state = state,
        onBack = onBack,
        onMode = viewModel::onMode,
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
    onMode: (CloseMode) -> Unit = {},
) {
    val title = if (state.mode == CloseMode.ASSIGNED) "Assign put" else "Close position"
    TrackerScaffold(title = title, onBack = onBack) { padding ->
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
                else -> CloseForm(state, onMode, onExitDate, onExitPremium, onExitFees, onClose)
            }
        }
    }
}

@Composable
private fun CloseForm(
    state: CloseUiState,
    onMode: (CloseMode) -> Unit,
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
        if (state.canAssign) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(CloseMode.CASH, CloseMode.ASSIGNED).forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { onMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) {
                        Text(if (mode == CloseMode.CASH) "Cash close" else "Assigned")
                    }
                }
            }
        }
        Text(
            when {
                state.mode == CloseMode.ASSIGNED ->
                    "You keep the opening credit. The option closes at a $0 exit premium. Stock profit stays on Assigned."
                position.side == OptionSide.BUY ->
                    "Selling to close. Profit when the exit premium is higher than the entry premium."
                else ->
                    "Buying to close. Profit when the exit premium is lower than the entry premium."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateField(
            label = if (state.mode == CloseMode.ASSIGNED) "Assigned date" else "Exit date",
            date = state.exitDate,
            onDate = onExitDate,
            error = null,
        )
        if (state.mode == CloseMode.ASSIGNED) {
            AssignmentPreview(position)
            Button(
                onClick = onClose,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Assign put") }
        } else {
            Text(
                exitPremiumCashFlowLabel(position.side),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.exitPremium,
                onValueChange = onExitPremium,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exit premium per share") },
                prefix = { Text("$") },
                singleLine = true,
                isError = state.errors.containsKey(Fields.EXIT_PREMIUM),
                supportingText = {
                    Text(state.errors[Fields.EXIT_PREMIUM] ?: exitPremiumCashFlowHint(position.side))
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
}

@Composable
private fun AssignmentPreview(position: Position) {
    val shares = AssignedPnl.sharesForContracts(position.contracts)
    val optionPnl = OptionPnl.realizedPnlCents(
        side = position.side,
        contracts = position.contracts,
        entryPremiumCents = position.entryPremiumCents,
        entryFeesCents = position.entryFeesCents,
        exitPremiumCents = 0L,
        exitFeesCents = 0L,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Stock lot", style = MaterialTheme.typography.labelLarge)
            Text(
                "$shares shares of ${position.ticker} at ${Money.format(position.strikeCents)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Option realized P/L ${Money.formatSigned(optionPnl)}. That is the opening credit minus fees. It does not include the stock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
