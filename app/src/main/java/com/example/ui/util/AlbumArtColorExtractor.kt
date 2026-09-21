package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.MiniPlayerBgMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class MiniPlayerColorScheme(
    val backgroundColor: Color,
    val onSurfaceColor: Color,
    val onSurfaceVariantColor: Color,
    val accentColor: Color,
    val onAccentColor: Color,
    val progressTrackColor: Color,
    val isDark: Boolean
)

/**
 * Calculates a coherent color scheme for the mini player given a base or extracted color.
 */
fun generateMiniPlayerColors(baseColor: Color, forceDark: Boolean? = null): MiniPlayerColorScheme {
    val r = baseColor.red
    val g = baseColor.green
    val b = baseColor.blue
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
    val isDark = forceDark ?: (luminance < 0.5f)

    return if (isDark) {
        // Dark theme surface: rich dark base with subtle hue tint
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.45f).coerceIn(0.15f, 0.55f) // Subtle saturation
        hsv[2] = 0.14f // Deep atmospheric darkness
        val bgArgb = android.graphics.Color.HSVToColor(hsv)
        val bgColor = Color(bgArgb)

        // Accent for play button: vibrant version of base hue
        hsv[1] = (hsv[1] * 2.0f).coerceIn(0.6f, 0.95f)
        hsv[2] = 0.88f
        val accentArgb = android.graphics.Color.HSVToColor(hsv)
        val accentColor = Color(accentArgb)

        MiniPlayerColorScheme(
            backgroundColor = bgColor,
            onSurfaceColor = Color(0xFFF4F4F6),
            onSurfaceVariantColor = Color(0xFFB0B0B8),
            accentColor = accentColor,
            onAccentColor = Color(0xFF0F1012),
            progressTrackColor = Color(0xFF2C2D35),
            isDark = true
        )
    } else {
        // Light theme surface: soft elegant tint
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.25f).coerceIn(0.08f, 0.30f) // Very gentle pastel tint
        hsv[2] = 0.97f // Bright clean surface
        val bgArgb = android.graphics.Color.HSVToColor(hsv)
        val bgColor = Color(bgArgb)

        // Accent for play button
        hsv[1] = (hsv[1] * 3.0f).coerceIn(0.7f, 0.95f)
        hsv[2] = 0.45f
        val accentArgb = android.graphics.Color.HSVToColor(hsv)
        val accentColor = Color(accentArgb)

        MiniPlayerColorScheme(
            backgroundColor = bgColor,
            onSurfaceColor = Color(0xFF191A1E),
            onSurfaceVariantColor = Color(0xFF53555D),
            accentColor = accentColor,
            onAccentColor = Color.White,
            progressTrackColor = Color(0xFFE4E4EB),
            isDark = false
        )
    }
}

/**
 * Extracts dominant vibrant color from bitmap
 */
fun extractDominantColorFromBitmap(bitmap: Bitmap): Color {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) return Color(0xFF323440)

    var maxScore = -1f
    var bestColorInt = android.graphics.Color.DKGRAY
    val hsv = FloatArray(3)

    for (y in 0 until height step 2) {
        for (x in 0 until width step 2) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha < 100) continue

            android.graphics.Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            // Prefer colorful pixels, avoid pure blacks or washed-out pure whites
            if (value in 0.15f..0.95f) {
                val score = sat * 2.5f + (1f - abs(value - 0.5f))
                if (score > maxScore) {
                    maxScore = score
                    bestColorInt = pixel
                }
            }
        }
    }

    if (maxScore < 0f && width > 0 && height > 0) {
        bestColorInt = bitmap.getPixel(width / 2, height / 2)
    }

    return Color(bestColorInt)
}

suspend fun loadDominantAlbumArtColor(context: Context, uri: Uri?): Color? = withContext(Dispatchers.IO) {
    if (uri == null) return@withContext null
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(48, 48)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                return@withContext extractDominantColorFromBitmap(bitmap)
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun rememberMiniPlayerColors(
    bgMode: MiniPlayerBgMode,
    albumArtUri: Uri?,
    customColorLong: Long
): MiniPlayerColorScheme {
    val context = LocalContext.current
    val systemSurface = MaterialTheme.colorScheme.surface
    val systemOnSurface = MaterialTheme.colorScheme.onSurface
    val systemOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val systemPrimary = MaterialTheme.colorScheme.primary
    val systemOnPrimary = MaterialTheme.colorScheme.onPrimary
    val systemSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var extractedAlbumColor by remember(albumArtUri) { mutableStateOf<Color?>(null) }

    LaunchedEffect(albumArtUri, bgMode) {
        if (bgMode == MiniPlayerBgMode.ALBUM_ART && albumArtUri != null) {
            extractedAlbumColor = loadDominantAlbumArtColor(context, albumArtUri)
        } else {
            extractedAlbumColor = null
        }
    }

    val targetScheme = when (bgMode) {
        MiniPlayerBgMode.ALBUM_ART -> {
            val color = extractedAlbumColor
            if (color != null) {
                generateMiniPlayerColors(color, forceDark = true)
            } else {
                // Neutral atmospheric dark fallback while loading or without art
                MiniPlayerColorScheme(
                    backgroundColor = Color(0xFF1E1F24),
                    onSurfaceColor = Color(0xFFF2F2F5),
                    onSurfaceVariantColor = Color(0xFFA8A8B2),
                    accentColor = systemPrimary,
                    onAccentColor = systemOnPrimary,
                    progressTrackColor = Color(0xFF2E2F38),
                    isDark = true
                )
            }
        }
        MiniPlayerBgMode.LIGHT -> {
            MiniPlayerColorScheme(
                backgroundColor = Color(0xFFFFFFFF),
                onSurfaceColor = Color(0xFF1B1B1F),
                onSurfaceVariantColor = Color(0xFF5A5A64),
                accentColor = Color(0xFF2656D6),
                onAccentColor = Color.White,
                progressTrackColor = Color(0xFFE6E6ED),
                isDark = false
            )
        }
        MiniPlayerBgMode.DARK -> {
            MiniPlayerColorScheme(
                backgroundColor = Color(0xFF17171A),
                onSurfaceColor = Color(0xFFF3F3F7),
                onSurfaceVariantColor = Color(0xFFA0A0AA),
                accentColor = Color(0xFF6B9BFA),
                onAccentColor = Color(0xFF0F1014),
                progressTrackColor = Color(0xFF27282F),
                isDark = true
            )
        }
        MiniPlayerBgMode.CUSTOM -> {
            val baseColor = if (customColorLong != 0L) Color(customColorLong) else Color(0xFF2C3E50)
            val isDark = (0.299f * baseColor.red + 0.587f * baseColor.green + 0.114f * baseColor.blue) < 0.55f
            if (isDark) {
                MiniPlayerColorScheme(
                    backgroundColor = baseColor,
                    onSurfaceColor = Color(0xFFFAFAFC),
                    onSurfaceVariantColor = Color(0xFFCCCCCC),
                    accentColor = Color(0xFF80D8FF),
                    onAccentColor = Color(0xFF002233),
                    progressTrackColor = baseColor.copy(alpha = 0.35f),
                    isDark = true
                )
            } else {
                MiniPlayerColorScheme(
                    backgroundColor = baseColor,
                    onSurfaceColor = Color(0xFF151518),
                    onSurfaceVariantColor = Color(0xFF4A4A52),
                    accentColor = Color(0xFF1976D2),
                    onAccentColor = Color.White,
                    progressTrackColor = Color(0x33000000),
                    isDark = false
                )
            }
        }
        MiniPlayerBgMode.SYSTEM -> {
            MiniPlayerColorScheme(
                backgroundColor = systemSurface,
                onSurfaceColor = systemOnSurface,
                onSurfaceVariantColor = systemOnSurfaceVariant,
                accentColor = systemPrimary,
                onAccentColor = systemOnPrimary,
                progressTrackColor = systemSurfaceVariant,
                isDark = false
            )
        }
    }

    // Animate smoothly between color transitions
    val animatedBg by animateColorAsState(targetScheme.backgroundColor, animationSpec = tween(400), label = "mp_bg")
    val animatedOnSurface by animateColorAsState(targetScheme.onSurfaceColor, animationSpec = tween(400), label = "mp_onSurface")
    val animatedOnSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariantColor, animationSpec = tween(400), label = "mp_onSurfaceVar")
    val animatedAccent by animateColorAsState(targetScheme.accentColor, animationSpec = tween(400), label = "mp_accent")
    val animatedOnAccent by animateColorAsState(targetScheme.onAccentColor, animationSpec = tween(400), label = "mp_onAccent")
    val animatedTrack by animateColorAsState(targetScheme.progressTrackColor, animationSpec = tween(400), label = "mp_track")

    return MiniPlayerColorScheme(
        backgroundColor = animatedBg,
        onSurfaceColor = animatedOnSurface,
        onSurfaceVariantColor = animatedOnSurfaceVariant,
        accentColor = animatedAccent,
        onAccentColor = animatedOnAccent,
        progressTrackColor = animatedTrack,
        isDark = targetScheme.isDark
    )
}
