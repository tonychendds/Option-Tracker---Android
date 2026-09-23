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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.HistoryMonth
import com.optiontracker.app.domain.pnl.MonthReportRow
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.StatusChip
import com.optiontracker.app.domain.pnl.ytdLabel
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.components.YearSelector
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.formatHitRate
import com.optiontracker.app.ui.format.formatMonth
import com.optiontracker.app.ui.format.formatMonthShort
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import com.optiontracker.app.ui.navigation.Routes
import com.optiontracker.app.ui.symbols.SymbolsPane
import com.optiontracker.app.ui.symbols.SymbolsUiState
import com.optiontracker.app.domain.pnl.SymbolPeriod
import com.optiontracker.app.domain.pnl.SymbolSort
import androidx.compose.foundation.text.KeyboardOptions
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    symbolsViewModel: com.optiontracker.app.ui.symbols.SymbolsViewModel,
    onNavigate: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onOpenTicker: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val symbols by symbolsViewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        symbols = symbols,
        onQuery = viewModel::onQuery,
        onSelectYear = viewModel::selectYear,
        onNavigate = onNavigate,
        onOpen = onOpen,
        onPeriod = symbolsViewModel::selectPeriod,
        onSort = symbolsViewModel::selectSort,
        onOpenTicker = onOpenTicker,
    )
}

private enum class HistoryPane { MONTHS, SYMBOLS }

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQuery: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onOpen: (Long) -> Unit,
    symbols: SymbolsUiState = SymbolsUiState(),
    onPeriod: (SymbolPeriod) -> Unit = {},
    onSort: (SymbolSort) -> Unit = {},
    onOpenTicker: (String) -> Unit = {},
) {
    var pane by rememberSaveable { mutableStateOf(HistoryPane.MONTHS) }
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
                    body = "When you close a position, its realized profit or loss is listed under the month you opened it.",
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        SegmentedButton(
                            selected = pane == HistoryPane.MONTHS,
                            onClick = { pane = HistoryPane.MONTHS },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("Months") }
                        SegmentedButton(
                            selected = pane == HistoryPane.SYMBOLS,
                            onClick = { pane = HistoryPane.SYMBOLS },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("Symbols") }
                    }
                    if (pane == HistoryPane.MONTHS) {
                        HistoryList(state, onQuery, onSelectYear, onOpen, Modifier.weight(1f))
                    } else {
                        SymbolsPane(
                            state = symbols,
                            onPeriod = onPeriod,
                            onSort = onSort,
                            onSelectYear = onSelectYear,
                            onOpen = onOpenTicker,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
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
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val monthIndexes = monthHeaderIndexes(state.months)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
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
                        "1 trade closed in this year"
                    } else {
                        "${state.yearTradeCount} trades closed in this year"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            MonthlyReportTable(
                rows = state.monthlyReport,
                filtered = state.query.isNotBlank(),
                onMonth = { yearMonth ->
                    val index = monthIndexes[yearMonth] ?: return@MonthlyReportTable
                    scope.launch { listState.animateScrollToItem(index) }
                },
            )
        }
        if (state.months.isEmpty()) {
            item {
                EmptyState(
                    title = if (state.query.isBlank()) {
                        "No trades opened in ${state.selectedYear}"
                    } else {
                        "No matching trades"
                    },
                    body = if (state.query.isBlank()) {
                        "Closed trades opened in another year are listed under that year."
                    } else {
                        "No closed trades use that ticker."
                    },
                )
            }
        } else {
            item {
                Text(
                    "Trades",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
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
                ClosedTradeRow(trade, onClick = { onOpen(trade.id) })
                HorizontalDivider()
            }
        }
    }
}

private fun monthHeaderIndexes(months: List<HistoryMonth>): Map<YearMonth, Int> {
    if (months.isEmpty()) return emptyMap()
    var index = 4
    return buildMap {
        months.forEach { month ->
            put(month.yearMonth, index)
            index += 1 + month.trades.size
        }
    }
}

@Composable
private fun MonthlyReportTable(
    rows: List<MonthReportRow>,
    filtered: Boolean,
    onMonth: (YearMonth) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Monthly report", style = MaterialTheme.typography.titleMedium)
        Text(
            if (filtered) {
                "January–December for this ticker, by the month the trade was opened. A later close still counts in that month. Hit rate is the share of those closes with a profit."
            } else {
                "January–December by the month the trade was opened. A later close still counts in that month. Hit rate is the share of those closes with a profit."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ReportLine(
                    month = "Month",
                    trades = "Trades",
                    hit = "Hit",
                    pnl = "P/L",
                    emphasize = false,
                    onClick = null,
                )
                HorizontalDivider()
                rows.forEach { row ->
                    ReportLine(
                        month = formatMonthShort(row.yearMonth),
                        trades = row.tradeCount.toString(),
                        hit = formatHitRate(row.hitRate),
                        pnl = Money.formatSigned(row.totalCents),
                        emphasize = true,
                        pnlTint = pnlColor(row.totalCents),
                        onClick = if (row.tradeCount > 0) {
                            { onMonth(row.yearMonth) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportLine(
    month: String,
    trades: String,
    hit: String,
    pnl: String,
    emphasize: Boolean,
    onClick: (() -> Unit)?,
    pnlTint: Color = Color.Unspecified,
) {
    val style = if (emphasize) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.labelMedium
    }
    val color = if (emphasize) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(month, modifier = Modifier.weight(1.1f), style = style, color = color)
        Text(trades, modifier = Modifier.weight(0.9f), style = style, color = color, textAlign = TextAlign.End)
        Text(hit, modifier = Modifier.weight(0.8f), style = style, color = color, textAlign = TextAlign.End)
        Text(
            pnl,
            modifier = Modifier.weight(1.5f),
            style = style,
            color = if (emphasize && pnlTint != Color.Unspecified) pnlTint else color,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun ClosedTradeRow(position: Position, onClick: () -> Unit) {
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
