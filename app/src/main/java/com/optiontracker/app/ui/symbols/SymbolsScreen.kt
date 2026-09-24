package com.optiontracker.app.ui.symbols

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.SymbolPeriod
import com.optiontracker.app.domain.pnl.SymbolSort
import com.optiontracker.app.domain.pnl.SymbolTotal
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.components.YearSelector
import com.optiontracker.app.ui.format.formatHitRate
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.history.ClosedTradeRow

@Composable
fun SymbolsPane(
    state: SymbolsUiState,
    onPeriod: (SymbolPeriod) -> Unit,
    onSort: (SymbolSort) -> Unit,
    onSelectYear: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Option realized P/L by ticker. Close date, same as Home’s year total. Stock lots are not included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.period == SymbolPeriod.YTD,
                        onClick = { onPeriod(SymbolPeriod.YTD) },
                        label = { Text("${state.year} YTD") },
                    )
                    FilterChip(
                        selected = state.period == SymbolPeriod.ALL,
                        onClick = { onPeriod(SymbolPeriod.ALL) },
                        label = { Text("All") },
                    )
                }
                if (state.period == SymbolPeriod.YTD) {
                    YearSelector(state.availableYears, state.year, onSelectYear)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sort == SymbolSort.PNL,
                        onClick = { onSort(SymbolSort.PNL) },
                        label = { Text("Largest") },
                    )
                    FilterChip(
                        selected = state.sort == SymbolSort.NAME,
                        onClick = { onSort(SymbolSort.NAME) },
                        label = { Text("A–Z") },
                    )
                }
            }
        }
        if (state.rows.isEmpty()) {
            item {
                EmptyState(
                    title = "No closed trades",
                    body = if (state.period == SymbolPeriod.YTD) {
                        "No options closed in ${state.year}. Switch to All to see every year."
                    } else {
                        "Closed option profit and loss will show here, one row per ticker."
                    },
                )
            }
        } else {
            items(state.rows, key = { it.ticker }) { row ->
                SymbolRow(row, onClick = { onOpen(row.ticker) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SymbolRow(row: SymbolTotal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.ticker, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(if (row.tradeCount == 1) "1 trade" else "${row.tradeCount} trades")
                    append(" · ")
                    append("${row.winningCount} wins")
                    if (row.losingCount > 0) {
                        append(", ")
                        append("${row.losingCount} losses")
                    }
                    append(" · ")
                    append(formatHitRate(row.hitRate))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            Money.formatSigned(row.totalCents),
            color = pnlColor(row.totalCents),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun SymbolTradesRoute(
    viewModel: SymbolTradesViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SymbolTradesScreen(state, onBack, onOpen)
}

@Composable
fun SymbolTradesScreen(
    state: SymbolTradesUiState,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    TrackerScaffold(title = state.ticker.ifBlank { "Symbol" }, onBack = onBack) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            if (state.trades.isEmpty()) {
                EmptyState(
                    title = "No closed trades",
                    body = "Nothing for ${state.ticker} in this period.",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(state.periodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Money.formatSigned(state.totalCents),
                                style = MaterialTheme.typography.headlineSmall,
                                color = pnlColor(state.totalCents),
                            )
                        }
                    }
                    items(state.trades, key = { it.id }) { position ->
                        ClosedTradeRow(position, onClick = { onOpen(position.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
