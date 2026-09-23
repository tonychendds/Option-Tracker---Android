package com.optiontracker.app.ui.positions

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
import com.optiontracker.app.domain.moneyness.Moneyness
import com.optiontracker.app.domain.moneyness.MoneynessClassifier
import com.optiontracker.app.domain.quote.QuoteBoard
import com.optiontracker.app.domain.quote.YahooSparkQuotes
import com.optiontracker.app.domain.quote.underlyingQuoteLabel
import com.optiontracker.app.domain.model.OptionType
import com.optiontracker.app.domain.model.Position
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.ui.components.ColorRole
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.StatusChip
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.format.contractsLabel
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import com.optiontracker.app.ui.navigation.Routes
import java.time.LocalDate

@Composable
fun PositionsRoute(
    viewModel: PositionsViewModel,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val quotes by viewModel.quotes.collectAsStateWithLifecycle()
    PositionsScreen(
        positions = positions,
        quotes = quotes,
        onNavigate = onNavigate,
        onAdd = onAdd,
        onOpen = onOpen,
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun PositionsScreen(
    positions: List<Position>,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    quotes: QuoteBoard = QuoteBoard(),
    onRefresh: () -> Unit = {},
) {
    TrackerScaffold(
        title = "Positions",
        currentRoute = Routes.POSITIONS,
        onNavigate = onNavigate,
        actions = {
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add position")
            }
        },
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            if (positions.isEmpty()) {
                EmptyState(
                    title = "No open positions",
                    body = "Add a contract you are holding. Sort stays by expiration, soonest first. Close it later to record profit or loss.",
                    action = {
                        Button(onClick = onAdd) { Text("Add position") }
                    },
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = quotes.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(positions, key = { it.id }) { position ->
                            val quote = underlyingQuoteLabel(position.ticker, quotes)
                            PositionRow(
                                position = position,
                                quote = quote,
                                moneyness = MoneynessClassifier.fromQuote(position.type, position.strikeCents, quote),
                                onClick = { onOpen(position.id) },
                            )
                            HorizontalDivider()
                        }
                        item {
                            Text(
                                YahooSparkQuotes.DELAY_LABEL,
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
fun PositionRow(
    position: Position,
    onClick: () -> Unit,
    quote: String = "",
    moneyness: Moneyness? = null,
) {
    val pastExpiry = position.status == PositionStatus.OPEN && position.expiry.isBefore(LocalDate.now())
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
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    position.ticker,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (quote.isNotEmpty()) {
                    Text(
                        quote,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (moneyness != null) {
                    StatusChip(text = moneyness.name, container = moneynessColor(moneyness))
                }
            }
            StatusChip(text = "Open")
        }
        Text(
            buildString {
                append(typeLabel(position.type))
                append(" · ")
                append(Money.format(position.strikeCents))
                append(" · ")
                append(formatDate(position.expiry))
                if (pastExpiry) append(" · Past expiry")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (pastExpiry) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(
                text = sideLabel(position.side),
                container = if (position.side.name == "BUY") ColorRole.SECONDARY else ColorRole.TERTIARY,
            )
            StatusChip(
                text = typeLabel(position.type),
                container = if (position.type == OptionType.CALL) ColorRole.PRIMARY else ColorRole.TERTIARY,
            )
            Text(
                buildString {
                    if (position.account.isNotBlank()) {
                        append(position.account)
                        append(" · ")
                    }
                    append(contractsLabel(position.contracts))
                    append(" · ")
                    append(Money.format(position.entryPremiumCents))
                    append(" premium")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun moneynessColor(moneyness: Moneyness): ColorRole = when (moneyness) {
    Moneyness.ITM -> ColorRole.PRIMARY
    Moneyness.OTM -> ColorRole.TERTIARY
    Moneyness.ATM -> ColorRole.SECONDARY
}
