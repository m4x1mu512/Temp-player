package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val id: String,
    val displayName: String,
    val previewColor: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val lightSecondary: Color,
    val darkSecondary: Color,
    val lightTertiary: Color,
    val darkTertiary: Color
) {
    TURQUOISE(
        id = "turquoise",
        displayName = "Бирюзовый",
        previewColor = Color(0xFF00E5BC),
        lightPrimary = Color(0xFF008F84),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFF00E5BC),
        darkOnPrimary = Color(0xFF00382E),
        lightSecondary = Color(0xFF6C4AB6),
        darkSecondary = Color(0xFFBD7BFF),
        lightTertiary = Color(0xFF0288D1),
        darkTertiary = Color(0xFF38D9F9)
    ),
    PURPLE(
        id = "purple",
        displayName = "Фиолетовый",
        previewColor = Color(0xFFBD7BFF),
        lightPrimary = Color(0xFF7E3AF2),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFBD7BFF),
        darkOnPrimary = Color(0xFF320068),
        lightSecondary = Color(0xFF008F84),
        darkSecondary = Color(0xFF00E5BC),
        lightTertiary = Color(0xFFFF007F),
        darkTertiary = Color(0xFFFF52A2)
    ),
    CYBER_YELLOW(
        id = "yellow",
        displayName = "Жёлтый",
        previewColor = Color(0xFFFFD600),
        lightPrimary = Color(0xFFCC9A00),
        lightOnPrimary = Color(0xFF1F1800),
        darkPrimary = Color(0xFFFFD600),
        darkOnPrimary = Color(0xFF3D2E00),
        lightSecondary = Color(0xFF008F84),
        darkSecondary = Color(0xFF00E5BC),
        lightTertiary = Color(0xFFFF6B00),
        darkTertiary = Color(0xFFFF9100)
    ),
    NEON_PINK(
        id = "pink",
        displayName = "Розовый",
        previewColor = Color(0xFFFF007F),
        lightPrimary = Color(0xFFD80064),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFFF007F),
        darkOnPrimary = Color(0xFF4A0022),
        lightSecondary = Color(0xFF7E3AF2),
        darkSecondary = Color(0xFFBD7BFF),
        lightTertiary = Color(0xFF00E5BC),
        darkTertiary = Color(0xFF00F0FF)
    ),
    MATRIX_GREEN(
        id = "green",
        displayName = "Зелёный",
        previewColor = Color(0xFF00FF66),
        lightPrimary = Color(0xFF00A33C),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFF00FF66),
        darkOnPrimary = Color(0xFF003814),
        lightSecondary = Color(0xFF008F84),
        darkSecondary = Color(0xFF00E5BC),
        lightTertiary = Color(0xFF38D9F9),
        darkTertiary = Color(0xFF00F0FF)
    ),
    NEON_ORANGE(
        id = "orange",
        displayName = "Оранжевый",
        previewColor = Color(0xFFFF6B00),
        lightPrimary = Color(0xFFE65100),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFFF6B00),
        darkOnPrimary = Color(0xFF4A1A00),
        lightSecondary = Color(0xFFBD7BFF),
        darkSecondary = Color(0xFFFFD600),
        lightTertiary = Color(0xFFFF007F),
        darkTertiary = Color(0xFFFF52A2)
    ),
    ELECTRIC_BLUE(
        id = "blue",
        displayName = "Синий",
        previewColor = Color(0xFF00B4D8),
        lightPrimary = Color(0xFF0077B6),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFF00B4D8),
        darkOnPrimary = Color(0xFF002840),
        lightSecondary = Color(0xFFBD7BFF),
        darkSecondary = Color(0xFF00E5BC),
        lightTertiary = Color(0xFF00FF66),
        darkTertiary = Color(0xFF00F0FF)
    );

    companion object {
        fun fromId(id: String?): AccentColor {
            return values().find { it.id.equals(id, ignoreCase = true) } ?: TURQUOISE
        }
    }
}
