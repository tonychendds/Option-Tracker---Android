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
import com.optiontracker.app.ui.close.ClosePositionRoute
import com.optiontracker.app.ui.close.ClosePositionViewModel
import com.optiontracker.app.ui.detail.PositionDetailRoute
import com.optiontracker.app.ui.detail.PositionDetailViewModel
import com.optiontracker.app.ui.editor.PositionEditorRoute
import com.optiontracker.app.ui.editor.PositionEditorViewModel
import com.optiontracker.app.ui.history.HistoryRoute
import com.optiontracker.app.ui.history.HistoryViewModel
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
        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(factory = factory)
            HistoryRoute(
                viewModel = viewModel,
                onNavigate = navigateTop,
                onOpen = { navController.navigate(Routes.historyDetail(it)) },
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
            arguments = listOf(navArgument("positionId") { type = NavType.LongType }),
        ) {
            val viewModel: ClosePositionViewModel = viewModel(factory = factory)
            ClosePositionRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onClosed = {
                    navController.popBackStack()
                    navController.popBackStack()
                },
            )
        }
    }
}
