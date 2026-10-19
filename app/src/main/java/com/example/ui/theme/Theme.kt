package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.example.data.model.AccentColor
import com.example.data.model.ThemeMode
import androidx.compose.material3.ColorScheme

private fun createDarkColorScheme(accentColor: AccentColor): ColorScheme =
  darkColorScheme(
    primary = accentColor.darkPrimary,
    onPrimary = accentColor.darkOnPrimary,
    secondary = accentColor.darkSecondary,
    onSecondary = TempDarkOnSecondary,
    tertiary = accentColor.darkTertiary,
    background = TempDarkBackground,
    onBackground = TempDarkOnBackground,
    surface = TempDarkSurface,
    onSurface = TempDarkOnSurface,
    surfaceVariant = TempDarkSurfaceVariant,
    onSurfaceVariant = TempDarkOnSurfaceVariant,
  )

private fun createLightColorScheme(accentColor: AccentColor): ColorScheme =
  lightColorScheme(
    primary = accentColor.lightPrimary,
    onPrimary = accentColor.lightOnPrimary,
    secondary = accentColor.lightSecondary,
    onSecondary = TempLightOnSecondary,
    tertiary = accentColor.lightTertiary,
    background = TempLightBackground,
    onBackground = TempLightOnBackground,
    surface = TempLightSurface,
    onSurface = TempLightOnSurface,
    surfaceVariant = TempLightSurfaceVariant,
    onSurfaceVariant = TempLightOnSurfaceVariant,
  )

@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  accentColor: AccentColor = AccentColor.TURQUOISE,
  darkTheme: Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  },
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) createDarkColorScheme(accentColor) else createLightColorScheme(accentColor)

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        // If dark theme is active in player -> status bar is white (isAppearanceLightStatusBars = false)
        // If light theme is active in player -> status bar is black (isAppearanceLightStatusBars = true)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    androidx.compose.material3.Surface(
      modifier = androidx.compose.ui.Modifier.fillMaxSize(),
      color = colorScheme.background
    ) {
      content()
    }
  }
}

