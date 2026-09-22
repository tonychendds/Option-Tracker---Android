package com.optiontracker.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optiontracker.app.data.SettingsRepository
import com.optiontracker.app.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.SYSTEM,
    )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
