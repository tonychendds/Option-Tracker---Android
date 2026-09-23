package com.optiontracker.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.optiontracker.app.ui.ocr.ScreenshotDraftStore
import com.optiontracker.app.ui.assigned.AssignedEditorRoute
import com.optiontracker.app.ui.assigned.AssignedEditorViewModel
import com.optiontracker.app.ui.assigned.AssignedRoute
import com.optiontracker.app.ui.assigned.AssignedViewModel
import com.optiontracker.app.ui.close.ClosePositionRoute
import com.optiontracker.app.ui.close.ClosePositionViewModel
import com.optiontracker.app.ui.detail.PositionDetailRoute
import com.optiontracker.app.ui.detail.PositionDetailViewModel
import com.optiontracker.app.ui.editor.PositionEditorRoute
import com.optiontracker.app.ui.editor.PositionEditorViewModel
import com.optiontracker.app.domain.pnl.SymbolPeriod
import com.optiontracker.app.ui.history.HistoryRoute
import com.optiontracker.app.ui.history.HistoryViewModel
import com.optiontracker.app.ui.symbols.SymbolTradesRoute
import com.optiontracker.app.ui.symbols.SymbolTradesViewModel
import com.optiontracker.app.ui.symbols.SymbolsViewModel
import com.optiontracker.app.ui.home.DashboardRoute
import com.optiontracker.app.ui.home.DashboardViewModel
import com.optiontracker.app.ui.positions.PositionsRoute
import com.optiontracker.app.ui.positions.PositionsViewModel
import com.optiontracker.app.ui.settings.SettingsRoute
import com.optiontracker.app.ui.settings.SettingsViewModel

@Composable
fun OptionTrackerNavHost(
    factory: ViewModelProvider.Factory,
    recognizeScreenshot: suspend (Uri) -> String,
    screenshotDrafts: ScreenshotDraftStore,
) {
    val navController = rememberNavController()
    val navigateTop: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            DashboardRoute(
                viewModel = viewModel,
                onNavigate = navigateTop,
                onAdd = { navController.navigate(Routes.editor()) },
                onOpenPosition = { navController.navigate(Routes.detail(it)) },
                onOpenHistory = { navController.navigate(Routes.historyDetail(it)) },
                recognizeScreenshot = recognizeScreenshot,
                onScreenshot = { result ->
                    screenshotDrafts.offer(result)
                    navController.navigate(Routes.editor())
                },
            )
        }
        composable(Routes.POSITIONS) {
            val viewModel: PositionsViewModel = viewModel(factory = factory)
            PositionsRoute(
                viewModel = viewModel,
                onNavigate = navigateTop,
                onAdd = { navController.navigate(Routes.editor()) },
                onOpen = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(Routes.ASSIGNED) {
            val viewModel: AssignedViewModel = viewModel(factory = factory)
            AssignedRoute(
                viewModel = viewModel,
                onNavigate = navigateTop,
                onAdd = { navController.navigate(Routes.assignedEditor()) },
                onOpen = { navController.navigate(Routes.assignedEditor(it)) },
            )
        }
        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(factory = factory)
            val symbolsViewModel: SymbolsViewModel = viewModel(factory = factory)
            HistoryRoute(
                viewModel = viewModel,
                symbolsViewModel = symbolsViewModel,
                onNavigate = navigateTop,
                onOpen = { navController.navigate(Routes.historyDetail(it)) },
                onOpenTicker = { ticker ->
                    val symbols = symbolsViewModel.uiState.value
                    navController.navigate(
                        Routes.symbolTrades(
                            ticker = ticker,
                            year = symbols.year,
                            allTime = symbols.period == SymbolPeriod.ALL,
                        ),
                    )
                },
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsRoute(viewModel = viewModel, onNavigate = navigateTop)
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("positionId") { type = NavType.LongType }),
        ) {
            val viewModel: PositionDetailViewModel = viewModel(factory = factory)
            PositionDetailRoute(
                viewModel = viewModel,
                readOnly = false,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(it)) },
                onClose = { navController.navigate(Routes.close(it)) },
                onAssign = { navController.navigate(Routes.close(it, assigned = true)) },
            )
        }
        composable(
            route = Routes.HISTORY_DETAIL,
            arguments = listOf(navArgument("positionId") { type = NavType.LongType }),
        ) {
            val viewModel: PositionDetailViewModel = viewModel(factory = factory)
            PositionDetailRoute(
                viewModel = viewModel,
                readOnly = true,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(it)) },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("positionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            val viewModel: PositionEditorViewModel = viewModel(factory = factory)
            PositionEditorRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                recognizeScreenshot = recognizeScreenshot,
            )
        }
        composable(
            route = Routes.CLOSE,
            arguments = listOf(
                navArgument("positionId") { type = NavType.LongType },
                navArgument("assigned") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            val viewModel: ClosePositionViewModel = viewModel(factory = factory)
            ClosePositionRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onClosed = {
                    navController.popBackStack()
                    navController.popBackStack()
                },
                onAssigned = {
                    navController.popBackStack()
                    navController.popBackStack()
                    navController.navigate(Routes.ASSIGNED) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(
            route = Routes.SYMBOL_TRADES,
            arguments = listOf(
                navArgument("ticker") { type = NavType.StringType },
                navArgument("year") { type = NavType.IntType },
                navArgument("allTime") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            val viewModel: SymbolTradesViewModel = viewModel(factory = factory)
            SymbolTradesRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(Routes.historyDetail(it)) },
            )
        }
        composable(
            route = Routes.ASSIGNED_EDITOR,
            arguments = listOf(
                navArgument("lotId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            val viewModel: AssignedEditorViewModel = viewModel(factory = factory)
            AssignedEditorRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
