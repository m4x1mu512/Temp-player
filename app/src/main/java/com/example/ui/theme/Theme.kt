package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = TempDarkPrimary,
    onPrimary = TempDarkOnPrimary,
    secondary = TempDarkSecondary,
    onSecondary = TempDarkOnSecondary,
    tertiary = TempDarkTertiary,
    background = TempDarkBackground,
    onBackground = TempDarkOnBackground,
    surface = TempDarkSurface,
    onSurface = TempDarkOnSurface,
    surfaceVariant = TempDarkSurfaceVariant,
    onSurfaceVariant = TempDarkOnSurfaceVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TempLightPrimary,
    onPrimary = TempLightOnPrimary,
    secondary = TempLightSecondary,
    onSecondary = TempLightOnSecondary,
    tertiary = TempLightTertiary,
    background = TempLightBackground,
    onBackground = TempLightOnBackground,
    surface = TempLightSurface,
    onSurface = TempLightOnSurface,
    surfaceVariant = TempLightSurfaceVariant,
    onSurfaceVariant = TempLightOnSurfaceVariant,
  )

@Composable
fun MyApplicationTheme(
  themeMode: com.example.data.model.ThemeMode = com.example.data.model.ThemeMode.LIGHT,
  darkTheme: Boolean = when (themeMode) {
    com.example.data.model.ThemeMode.LIGHT -> false
    com.example.data.model.ThemeMode.DARK -> true
    com.example.data.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
  },
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

