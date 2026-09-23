package com.optiontracker.app

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.optiontracker.app.data.AssignedLotRepository
import com.optiontracker.app.data.PositionRepository
import com.optiontracker.app.data.SettingsRepository
import com.optiontracker.app.data.quote.YahooDelayedQuoteClient
import androidx.room.withTransaction
import com.optiontracker.app.data.local.OptionDatabase
import com.optiontracker.app.ui.ReportYearStore
import com.optiontracker.app.ui.ocr.MlKitScreenshotReader
import com.optiontracker.app.ui.ocr.ScreenshotDraftStore
import com.optiontracker.app.ui.assigned.AssignedEditorViewModel
import com.optiontracker.app.ui.assigned.AssignedViewModel
import com.optiontracker.app.ui.close.ClosePositionViewModel
import com.optiontracker.app.ui.detail.PositionDetailViewModel
import com.optiontracker.app.ui.editor.PositionEditorViewModel
import com.optiontracker.app.ui.history.HistoryViewModel
import com.optiontracker.app.ui.home.DashboardViewModel
import com.optiontracker.app.ui.positions.PositionsViewModel
import com.optiontracker.app.ui.settings.SettingsViewModel

class AppContainer(context: Context) {
    private val database = OptionDatabase.create(context)
    private val transact: suspend (suspend () -> Unit) -> Unit = { block -> database.withTransaction { block() } }
    val repository = PositionRepository(
        dao = database.positionDao(),
        transact = transact,
    )
    val assignedLots = AssignedLotRepository(
        lots = database.assignedLotDao(),
        positions = database.positionDao(),
        transact = transact,
    )
    private val quotes = YahooDelayedQuoteClient()
    val settingsRepository = SettingsRepository(context)
    private val reportYear = ReportYearStore()
    val screenshotDrafts = ScreenshotDraftStore()
    val screenshotReader = MlKitScreenshotReader(context)

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(repository, reportYear) }
        initializer { PositionsViewModel(repository, quotes) }
        initializer { AssignedViewModel(assignedLots, quotes) }
        initializer { HistoryViewModel(repository, reportYear) }
        initializer { SettingsViewModel(settingsRepository, repository) }
        initializer { PositionDetailViewModel(repository, createSavedStateHandle()) }
        initializer { PositionEditorViewModel(repository, createSavedStateHandle(), screenshotDrafts) }
        initializer { ClosePositionViewModel(repository, assignedLots, createSavedStateHandle()) }
        initializer { AssignedEditorViewModel(assignedLots, createSavedStateHandle()) }
    }
}
