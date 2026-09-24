package com.optiontracker.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.data.SettingsRepository
import com.optiontracker.app.data.ThemeMode
import com.optiontracker.app.domain.csv.CsvImportResult
import com.optiontracker.app.domain.csv.CsvTradeParser
import com.optiontracker.app.domain.csv.CsvTradeWriter
import com.optiontracker.app.domain.model.Position
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repository: PositionRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.SYSTEM,
    )

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importSummary = MutableStateFlow<CsvImportResult?>(null)
    val importSummary: StateFlow<CsvImportResult?> = _importSummary.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun importCsv(readText: () -> String) {
        if (_importing.value) return
        viewModelScope.launch {
            _importing.value = true
            val summary = withContext(Dispatchers.IO) {
                try {
                    val result = CsvTradeParser.parse(readText())
                    if (result.replacedExisting) {
                        repository.replaceAll(result.positions)
                    }
                    result
                } catch (error: Exception) {
                    CsvImportResult.unreadable(error.message ?: "Could not read that file")
                }
            }
            _importSummary.value = summary
            _importing.value = false
        }
    }

    fun dismissImportSummary() {
        _importSummary.value = null
    }

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    /**
     * Builds the trade CSV. [write] runs on a background thread when the caller
     * is saving into a document the user picked. Share leaves [write] null.
     */
    fun prepareExport(write: ((String) -> Unit)? = null, onResult: (ExportResult) -> Unit) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            val result = withContext(Dispatchers.IO) {
                try {
                    val positions = repository.listPositions()
                        .sortedWith(compareBy<Position>({ it.openedOn }, { it.ticker }, { it.id }))
                    val csv = CsvTradeWriter.write(positions)
                    write?.invoke(csv)
                    ExportResult.Ready(
                        count = positions.size,
                        csv = csv,
                        fileName = CsvTradeWriter.suggestedFileName(LocalDate.now()),
                    )
                } catch (error: Exception) {
                    val verb = if (write == null) "share" else "save"
                    val detail = error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
                    ExportResult.Failed("Could not $verb trades. $detail")
                }
            }
            _exporting.value = false
            onResult(result)
        }
    }
}

sealed interface ExportResult {
    data class Ready(val count: Int, val csv: String, val fileName: String) : ExportResult
    data class Failed(val message: String) : ExportResult
}
