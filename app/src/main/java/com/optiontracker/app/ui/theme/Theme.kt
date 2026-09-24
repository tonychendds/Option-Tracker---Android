package com.optiontracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.optiontracker.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0D8),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF1F4B73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4F8),
    onSecondaryContainer = Color(0xFF001C38),
    tertiary = Color(0xFF8A4E2B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCB),
    onTertiaryContainer = Color(0xFF341100),
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF161D1A),
    surface = Color(0xFFF4F7F5),
    onSurface = Color(0xFF161D1A),
    surfaceContainerLow = Color(0xFFEEF2EF),
    surfaceContainer = Color(0xFFE7ECE9),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED9B6),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF0E6B4F),
    onPrimaryContainer = Color(0xFFB7F0D8),
    secondary = Color(0xFFA9C9E8),
    onSecondary = Color(0xFF003256),
    secondaryContainer = Color(0xFF1F4B73),
    onSecondaryContainer = Color(0xFFD3E4F8),
    tertiary = Color(0xFFF2B692),
    onTertiary = Color(0xFF4A2612),
    tertiaryContainer = Color(0xFF6C3A1E),
    onTertiaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceContainerLow = Color(0xFF191D1B),
    surfaceContainer = Color(0xFF1D2220),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val TrackerTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
)

@Composable
fun OptionTrackerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = TrackerTypography,
        content = content,
    )
}
