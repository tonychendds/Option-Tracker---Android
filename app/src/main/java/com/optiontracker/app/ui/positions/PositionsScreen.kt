package com.optiontracker.app.ui.positions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.dte.DteTone
import com.optiontracker.app.domain.dte.daysToExpiration
import com.optiontracker.app.domain.dte.dteDescription
import com.optiontracker.app.domain.dte.dteLabel
import com.optiontracker.app.domain.dte.dteTone
import com.optiontracker.app.domain.moneyness.Moneyness
import com.optiontracker.app.domain.moneyness.MoneynessClassifier
import com.optiontracker.app.domain.pnl.OptionPnl
import com.optiontracker.app.domain.quote.OccSymbol
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
import com.optiontracker.app.ui.format.pnlColor
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
    val contracts by viewModel.contracts.collectAsStateWithLifecycle()
    PositionsScreen(
        positions = positions,
        quotes = quotes,
        contracts = contracts,
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
    contracts: QuoteBoard = QuoteBoard(),
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
                    isRefreshing = quotes.refreshing || contracts.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(positions, key = { it.id }) { position ->
                            val quote = underlyingQuoteLabel(position.ticker, quotes)
                            PositionRow(
                                position = position,
                                quote = quote,
                                premiumLine = OccSymbol.premiumLine(position, contracts),
                                moneyness = MoneynessClassifier.fromQuote(position.type, position.strikeCents, quote),
                                unrealizedPnlCents = unrealizedPremiumCents(position, contracts),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionRow(
    position: Position,
    onClick: () -> Unit,
    quote: String = "",
    premiumLine: String = "entry ${Money.format(position.entryPremiumCents)}",
    moneyness: Moneyness? = null,
    unrealizedPnlCents: Long? = null,
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
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    position.ticker,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (quote.isNotEmpty()) {
                    Text(
                        quote,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (moneyness == Moneyness.ITM) {
                    StatusChip(text = "ITM", container = ColorRole.DANGER)
                }
                DaysRemainingChip(position.expiry)
            }
            StatusChip(text = "Open", modifier = Modifier.padding(start = 8.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    append(premiumLine)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unrealizedPnlCents != null) {
                Text(
                    Money.formatSigned(unrealizedPnlCents),
                    color = pnlColor(unrealizedPnlCents),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun unrealizedPremiumCents(position: Position, contracts: QuoteBoard): Long? {
    val current = OccSymbol.currentPremiumCents(position, contracts) ?: return null
    return try {
        OptionPnl.unrealizedPremiumCents(
            side = position.side,
            contracts = position.contracts,
            entryPremiumCents = position.entryPremiumCents,
            currentPremiumCents = current,
        )
    } catch (_: ArithmeticException) {
        null
    }
}

@Composable
private fun DaysRemainingChip(expiry: LocalDate) {
    val days = daysToExpiration(LocalDate.now(), expiry)
    val role = when (dteTone(days)) {
        DteTone.URGENT -> ColorRole.DANGER
        DteTone.SOON -> ColorRole.AMBER
        DteTone.NEUTRAL -> ColorRole.NEUTRAL
    }
    StatusChip(
        text = dteLabel(days),
        container = role,
        modifier = Modifier.semantics { contentDescription = dteDescription(days) },
    )
}
