package com.optiontracker.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.optiontracker.app.domain.ocr.BrokerParseResult
import com.optiontracker.app.ui.ocr.rememberScreenshotImport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.domain.model.PositionStatus
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.ActivityKind
import com.optiontracker.app.domain.pnl.RecentActivity
import com.optiontracker.app.domain.pnl.realizedPnlCents
import com.optiontracker.app.domain.pnl.ytdLabel
import com.optiontracker.app.ui.components.EmptyState
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.components.YearSelector
import com.optiontracker.app.ui.format.contractsLabel
import com.optiontracker.app.ui.format.formatDate
import com.optiontracker.app.ui.format.pnlColor
import com.optiontracker.app.ui.format.sideLabel
import com.optiontracker.app.ui.format.typeLabel
import com.optiontracker.app.ui.navigation.Routes

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenPosition: (Long) -> Unit,
    onOpenHistory: (Long) -> Unit,
    recognizeScreenshot: suspend (Uri) -> String,
    onScreenshot: (BrokerParseResult) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var reading by remember { mutableStateOf(false) }
    val pickScreenshot = rememberScreenshotImport(
        recognize = recognizeScreenshot,
        onResult = onScreenshot,
        onReading = { reading = it },
    )
    DashboardScreen(
        state = state,
        onNavigate = onNavigate,
        onAdd = onAdd,
        onAddFromScreenshot = pickScreenshot,
        readingScreenshot = reading,
        onOpenActivity = { activity ->
            if (activity.position.status == PositionStatus.OPEN) {
                onOpenPosition(activity.position.id)
            } else {
                onOpenHistory(activity.position.id)
            }
        },
        onSelectYear = viewModel::selectYear,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenActivity: (RecentActivity) -> Unit,
    onSelectYear: (Int) -> Unit = {},
    onAddFromScreenshot: () -> Unit = {},
    readingScreenshot: Boolean = false,
) {
    var showAddMenu by remember { mutableStateOf(false) }
    TrackerScaffold(
        title = "Home",
        currentRoute = Routes.HOME,
        onNavigate = onNavigate,
        showAd = true,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMenu = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add position")
            }
        },
    ) { padding ->
        when (state) {
            DashboardUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            is DashboardUiState.Ready -> DashboardContent(padding, state, onOpenActivity, onSelectYear)
        }
    }
    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text("Add a trade") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Enter the trade yourself, or read a broker screenshot on this device. Nothing is saved until you tap Save.")
                    TextButton(
                        onClick = {
                            showAddMenu = false
                            onAdd()
                        },
                    ) { Text("Enter manually") }
                    TextButton(
                        onClick = {
                            showAddMenu = false
                            onAddFromScreenshot()
                        },
                    ) { Text("Add from screenshot") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text("Cancel") }
            },
        )
    }
    if (readingScreenshot) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Reading screenshot") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Looking for a trade on this device.")
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun DashboardContent(
    padding: PaddingValues,
    state: DashboardUiState.Ready,
    onOpenActivity: (RecentActivity) -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val summary = state.summary
    ScreenColumn(padding) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Open premium", style = MaterialTheme.typography.labelLarge)
                    Text(
                        Money.formatSigned(summary.netPremiumCents),
                        style = MaterialTheme.typography.headlineLarge,
                        color = pnlColor(summary.netPremiumCents),
                    )
                    Text(
                        "Net cash from premiums still open. Credits are positive and debits are negative. This is not a live mark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Open", summary.openCount.toString(), Modifier.weight(1f))
                StatCard("Contracts", summary.openContracts.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Calls", summary.callContracts.toString(), Modifier.weight(1f))
                StatCard("Puts", summary.putContracts.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Long", summary.longContracts.toString(), Modifier.weight(1f))
                StatCard("Short", summary.shortContracts.toString(), Modifier.weight(1f))
            }
            YearSelector(
                years = summary.availableYears,
                selectedYear = summary.selectedYear,
                onSelect = onSelectYear,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("YTD realized P/L", style = MaterialTheme.typography.labelLarge)
                    Text(
                        Money.formatSigned(summary.realizedYearCents),
                        style = MaterialTheme.typography.headlineLarge,
                        color = pnlColor(summary.realizedYearCents),
                    )
                    Text(
                        ytdLabel(summary.selectedYear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (summary.closedYearCount == 1) {
                            "1 trade closed in this year"
                        } else {
                            "${summary.closedYearCount} trades closed in this year"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Realized P/L this month", style = MaterialTheme.typography.labelLarge)
                    Text(
                        Money.formatSigned(summary.realizedThisMonthCents),
                        style = MaterialTheme.typography.headlineLarge,
                        color = pnlColor(summary.realizedThisMonthCents),
                    )
                    Text(
                        if (summary.closedThisMonthCount == 1) {
                            "1 closed trade opened this month"
                        } else {
                            "${summary.closedThisMonthCount} closed trades opened this month"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("Recent activity", style = MaterialTheme.typography.titleMedium)
            if (summary.recent.isEmpty()) {
                EmptyState(
                    title = "No trades yet",
                    body = "Add an open option position. When you close it, realized profit or loss shows up here and in History.",
                )
            } else {
                summary.recent.forEach { activity ->
                    ActivityRow(activity, onClick = { onOpenActivity(activity) })
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ActivityRow(activity: RecentActivity, onClick: () -> Unit) {
    val position = activity.position
    val kind = if (activity.kind == ActivityKind.OPENED) "Opened" else "Closed"
    val pnl = if (activity.kind == ActivityKind.CLOSED) position.realizedPnlCents() else null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(position.ticker, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$kind · ${sideLabel(position.side)} ${contractsLabel(position.contracts)} ${typeLabel(position.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    formatDate(activity.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pnl != null) {
                Text(Money.formatSigned(pnl), color = pnlColor(pnl), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
