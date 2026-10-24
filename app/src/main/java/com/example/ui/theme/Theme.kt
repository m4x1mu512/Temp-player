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
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

import com.example.data.model.AccentColor
import com.example.data.model.ThemeMode
import androidx.compose.material3.ColorScheme

private fun createDarkColorScheme(accentColor: AccentColor): ColorScheme {
  val (background, surface, surfaceVariant) = when (accentColor) {
    AccentColor.TURQUOISE -> Triple(Color(0xFF041311), Color(0xFF0B211E), Color(0xFF12332E))
    AccentColor.PURPLE -> Triple(Color(0xFF080414), Color(0xFF150923), Color(0xFF28113D))
    AccentColor.CYBER_YELLOW -> Triple(Color(0xFF151104), Color(0xFF241D08), Color(0xFF3B2E0C))
    AccentColor.NEON_PINK -> Triple(Color(0xFF160611), Color(0xFF260A1D), Color(0xFF40102F))
    AccentColor.MATRIX_GREEN -> Triple(Color(0xFF031108), Color(0xFF0A1F11), Color(0xFF12351D))
    AccentColor.NEON_ORANGE -> Triple(Color(0xFF170A03), Color(0xFF281206), Color(0xFF421E0A))
    AccentColor.ELECTRIC_BLUE -> Triple(Color(0xFF030D19), Color(0xFF091A2B), Color(0xFF102A42))
  }
  return darkColorScheme(
    primary = accentColor.darkPrimary,
    onPrimary = accentColor.darkOnPrimary,
    secondary = accentColor.darkSecondary,
    onSecondary = TempDarkOnSecondary,
    tertiary = accentColor.darkTertiary,
    background = background,
    onBackground = TempDarkOnBackground,
    surface = surface,
    onSurface = TempDarkOnSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = TempDarkOnSurfaceVariant,
  )
}

private fun createLightColorScheme(accentColor: AccentColor): ColorScheme {
  val (background, surface, surfaceVariant) = when (accentColor) {
    AccentColor.TURQUOISE -> Triple(Color(0xFFF0FBF8), Color(0xFFFAFFFD), Color(0xFFDDF4EE))
    AccentColor.PURPLE -> Triple(Color(0xFFF7F2FF), Color(0xFFFFFBFF), Color(0xFFEDE1FF))
    AccentColor.CYBER_YELLOW -> Triple(Color(0xFFFFFAE8), Color(0xFFFFFDF5), Color(0xFFF8EDC7))
    AccentColor.NEON_PINK -> Triple(Color(0xFFFFF0F7), Color(0xFFFFFAFC), Color(0xFFF8DCEB))
    AccentColor.MATRIX_GREEN -> Triple(Color(0xFFF0FAF1), Color(0xFFFAFFFA), Color(0xFFDDEFE0))
    AccentColor.NEON_ORANGE -> Triple(Color(0xFFFFF4EC), Color(0xFFFFFBF7), Color(0xFFF8E2D0))
    AccentColor.ELECTRIC_BLUE -> Triple(Color(0xFFEEF7FF), Color(0xFFFAFCFF), Color(0xFFDDEBFA))
  }
  return lightColorScheme(
    primary = accentColor.lightPrimary,
    onPrimary = accentColor.lightOnPrimary,
    secondary = accentColor.lightSecondary,
    onSecondary = TempLightOnSecondary,
    tertiary = accentColor.lightTertiary,
    background = background,
    onBackground = TempLightOnBackground,
    surface = surface,
    onSurface = TempLightOnSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = TempLightOnSurfaceVariant,
  )
}

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

