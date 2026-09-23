package com.optiontracker.app.ui.assigned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.AssignedLot
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.AssignedPnl
import com.optiontracker.app.domain.quote.QuoteBoard
import com.optiontracker.app.domain.quote.underlyingQuoteLabel
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.navigation.Routes

const val ASSIGNED_QUOTE_LABEL = "Stock quotes delayed 15+ min. Unrealized P/L uses that price."

@Composable
fun AssignedRoute(
    viewModel: AssignedViewModel,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val lots by viewModel.lots.collectAsStateWithLifecycle()
    val quotes by viewModel.quotes.collectAsStateWithLifecycle()
    AssignedScreen(
        lots = lots,
        quotes = quotes,
        onNavigate = onNavigate,
        onAdd = onAdd,
        onOpen = onOpen,
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun AssignedScreen(
    lots: List<AssignedLot>,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    quotes: QuoteBoard = QuoteBoard(),
    onRefresh: () -> Unit = {},
) {
    TrackerScaffold(
        title = "Assigned",
        currentRoute = Routes.ASSIGNED,
        onNavigate = onNavigate,
        actions = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add assignment")
            }
        },
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            if (lots.isEmpty()) {
                EmptyState(
                    title = "No assigned shares",
                    body = "Short puts that get assigned land here as shares bought at the strike. You can also add a past assignment.",
                    action = {
                        Button(onClick = onAdd) { Text("Add assignment") }
                    },
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = quotes.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(lots, key = { it.id }) { lot ->
                            val quote = underlyingQuoteLabel(lot.ticker, quotes)
                            AssignedRow(lot, quote, onClick = { onOpen(lot.id) })
                            HorizontalDivider()
                        }
                        item {
                            Text(
                                ASSIGNED_QUOTE_LABEL,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignedRow(lot: AssignedLot, quote: String, onClick: () -> Unit) {
    val pnl = AssignedPnl.unrealizedFromQuote(quote, lot.costBasisCents, lot.shares)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(lot.ticker, style = MaterialTheme.typography.titleMedium)
            Text(
                pnl?.let(Money::formatSigned) ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = if (pnl == null) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(pnl),
            )
        }
        Text(
            "Cost basis ${Money.format(lot.costBasisCents)} · ${quote.ifBlank { "—" }}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${sharesLabel(lot.shares)} · Assigned ${formatDate(lot.assignedOn)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun sharesLabel(shares: Int): String = if (shares == 1) "1 share" else "$shares shares"
