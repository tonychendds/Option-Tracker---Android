package com.optiontracker.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.StatusChip
import com.optiontracker.app.domain.pnl.ytdLabel
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.components.YearSelector
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.formatMonth
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import com.optiontracker.app.ui.navigation.Routes
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onNavigate: (String) -> Unit,
    onOpen: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, viewModel::onQuery, viewModel::selectYear, onNavigate, onOpen)
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQuery: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onOpen: (Long) -> Unit,
) {
    TrackerScaffold(
        title = "History",
        currentRoute = Routes.HISTORY,
        onNavigate = onNavigate,
        showAd = true,
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            if (!state.hasAnyClosed) {
                EmptyState(
                    title = "No closed trades",
                    body = "When you close a position, the realized profit or loss is listed here by month.",
                )
            } else {
                HistoryList(state, onQuery, onSelectYear, onOpen)
            }
        }
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState,
    onQuery: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onOpen: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Filter by ticker") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                YearSelector(
                    years = state.availableYears,
                    selectedYear = state.selectedYear,
                    onSelect = onSelectYear,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Year total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            ytdLabel(state.selectedYear),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        Money.formatSigned(state.yearTotalCents),
                        color = pnlColor(state.yearTotalCents),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Text(
                    if (state.yearTradeCount == 1) {
                        "1 closed trade"
                    } else {
                        "${state.yearTradeCount} closed trades"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.months.isEmpty()) {
            item {
                EmptyState(
                    title = if (state.query.isBlank()) {
                        "No closed trades in ${state.selectedYear}"
                    } else {
                        "No matching trades"
                    },
                    body = if (state.query.isBlank()) {
                        "Closed trades from other years stay on their own year."
                    } else {
                        "No closed trades use that ticker."
                    },
                )
            }
        }
        state.months.forEach { month ->
            item(key = month.yearMonth.toString()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(formatMonth(month.yearMonth), style = MaterialTheme.typography.titleMedium)
                    Text(
                        Money.formatSigned(month.totalPnlCents),
                        color = pnlColor(month.totalPnlCents),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(month.trades, key = { it.id }) { trade ->
                HistoryRow(trade, onClick = { onOpen(trade.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(position: Position, onClick: () -> Unit) {
    val pnl = position.realizedPnlCents() ?: 0L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(position.ticker, style = MaterialTheme.typography.titleMedium)
                StatusChip(text = "Closed")
            }
            Text(
                "${sideLabel(position.side)} ${typeLabel(position.type)} ${Money.format(position.strikeCents)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                position.closedOn?.let(::formatDate) ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(Money.formatSigned(pnl), color = pnlColor(pnl), style = MaterialTheme.typography.titleMedium)
    }
}
