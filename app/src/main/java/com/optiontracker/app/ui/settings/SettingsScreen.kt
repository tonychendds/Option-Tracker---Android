package com.optiontracker.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.BuildConfig
import com.optiontracker.app.data.ThemeMode
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.navigation.Routes

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit,
) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsScreen(theme, viewModel::setTheme, onNavigate)
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    onNavigate: (String) -> Unit,
) {
    var showRemoveAds by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    TrackerScaffold(
        title = "Settings",
        currentRoute = Routes.SETTINGS,
        onNavigate = onNavigate,
    ) { padding ->
        ScreenColumn(padding, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Currency", style = MaterialTheme.typography.titleMedium)
                        Text("USD", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Version 1 records money in US dollars.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Remove ads", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A one-time purchase to hide ads will be offered later through Google Play Billing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { showRemoveAds = true }, enabled = false) {
                            Text("Coming soon")
                        }
                        TextButton(onClick = { showRemoveAds = true }) { Text("Learn more") }
                    }
                }
                OutlinedButton(onClick = { showExport = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export trades")
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Not financial advice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Option Tracker is a personal record-keeping tool. It does not provide recommendations, brokerage services, or live quotes. You are responsible for your own trading decisions.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    "Option Tracker ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showRemoveAds) {
        AlertDialog(
            onDismissRequest = { showRemoveAds = false },
            title = { Text("Remove ads") },
            text = {
                Text(
                    "This will be a one-time in-app purchase with Google Play Billing. This version does not include the Play Billing library or a product id, and nothing can be purchased.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showRemoveAds = false }) { Text("OK") }
            },
        )
    }
    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Export trades") },
            text = {
                Text(
                    "Export is not available in this version. A file of your positions and closed trades will be added later. Nothing was written to storage.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showExport = false }) { Text("OK") }
            },
        )
    }
}
