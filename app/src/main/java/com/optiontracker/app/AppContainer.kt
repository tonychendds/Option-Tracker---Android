package com.optiontracker.app

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.data.SettingsRepository
import com.optiontracker.app.data.local.OptionDatabase
import com.optiontracker.app.ui.close.ClosePositionViewModel
import com.optiontracker.app.ui.detail.PositionDetailViewModel
import com.optiontracker.app.ui.editor.PositionEditorViewModel
import com.optiontracker.app.ui.history.HistoryViewModel
import com.optiontracker.app.ui.home.DashboardViewModel
import com.optiontracker.app.ui.positions.PositionsViewModel
import com.optiontracker.app.ui.settings.SettingsViewModel

class AppContainer(context: Context) {
    private val database = OptionDatabase.create(context)
    val repository = PositionRepository(database.positionDao())
    val settingsRepository = SettingsRepository(context)

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(repository) }
        initializer { PositionsViewModel(repository) }
        initializer { HistoryViewModel(repository) }
        initializer { SettingsViewModel(settingsRepository) }
        initializer { PositionDetailViewModel(repository, createSavedStateHandle()) }
        initializer { PositionEditorViewModel(repository, createSavedStateHandle()) }
        initializer { ClosePositionViewModel(repository, createSavedStateHandle()) }
    }
}
