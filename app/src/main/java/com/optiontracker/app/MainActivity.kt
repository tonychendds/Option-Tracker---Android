package com.optiontracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.optiontracker.app.data.ThemeMode
import com.optiontracker.app.ui.navigation.OptionTrackerNavHost
import com.optiontracker.app.ui.theme.OptionTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OptionTrackerApp
        setContent {
            val themeMode by app.container.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            OptionTrackerTheme(themeMode = themeMode) {
                OptionTrackerNavHost(factory = app.container.viewModelFactory)
            }
        }
    }
}
