package com.optiontracker.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.BuildConfig
import com.optiontracker.app.data.ThemeMode
import com.optiontracker.app.domain.csv.CsvImportResult
import com.optiontracker.app.ui.components.ScreenColumn
import com.optiontracker.app.ui.components.TrackerScaffold
import com.optiontracker.app.ui.navigation.Routes
import java.nio.charset.Charset

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit,
) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val importSummary by viewModel.importSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importCsv {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charset.forName("UTF-8")).readText()
            } ?: error("Could not open that file")
        }
    }
    SettingsScreen(
        themeMode = theme,
        onTheme = viewModel::setTheme,
        onNavigate = onNavigate,
        importing = importing,
        importSummary = importSummary,
        onPickCsv = {
            picker.launch(arrayOf("text/*", "application/*", "*/*"))
        },
        onDismissImport = viewModel::dismissImportSummary,
    )
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    onNavigate: (String) -> Unit,
    importing: Boolean = false,
    importSummary: CsvImportResult? = null,
    onPickCsv: () -> Unit = {},
    onDismissImport: () -> Unit = {},
) {
    var showRemoveAds by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Import CSV", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Load trades from a spreadsheet export. This replaces every trade stored on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { showImportConfirm = true },
                            enabled = !importing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (importing) "Importing…" else "Import CSV")
                        }
                    }
                }
                OutlinedButton(onClick = { showExport = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export trades")
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Not financial advice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Option Tracker is a personal record-keeping tool. It does not provide recommendations or brokerage services. Stock prices on Positions are delayed 15+ minutes and are not option prices. You are responsible for your own trading decisions.",
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
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Replace all local trades?") },
            text = {
                Text(
                    "This replaces all local trades with the rows in the file. Rows that cannot be read are skipped. If every row fails, your current trades stay.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        onPickCsv()
                    },
                ) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            },
        )
    }
    importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = onDismissImport,
            title = { Text(if (summary.replacedExisting) "Import finished" else "Import did not change your trades") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(importSummaryText(summary))
                    summary.errors.take(8).forEach { error ->
                        Text(error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (summary.errors.size > 8) {
                        Text(
                            "And ${summary.errors.size - 8} more.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissImport) { Text("OK") }
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

private fun importSummaryText(summary: CsvImportResult): String {
    summary.fileError?.let { return it }
    val counts = "${summary.openCount} open, ${summary.closedCount} closed, ${summary.skippedCount} skipped."
    return if (summary.replacedExisting) {
        "Replaced local trades. $counts"
    } else {
        "Nothing was imported, so your existing trades were kept. $counts"
    }
}
