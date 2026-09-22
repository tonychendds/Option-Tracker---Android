package com.optiontracker.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.data.SettingsRepository
import com.optiontracker.app.data.ThemeMode
import com.optiontracker.app.domain.csv.CsvImportResult
import com.optiontracker.app.domain.csv.CsvTradeParser
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
}
