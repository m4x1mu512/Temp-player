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
import androidx.compose.ui.graphics.Brush
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

data class PlayerColorScheme(
    val backgroundColor: Color,
    val backgroundBrush: Brush? = null,
    val miniPlayerBrush: Brush? = null,
    val gradientColors: List<Color> = emptyList(),
    val onSurfaceColor: Color,
    val onSurfaceVariantColor: Color,
    val accentColor: Color,
    val onAccentColor: Color,
    val progressTrackColor: Color,
    val isDark: Boolean,
    val isGradient: Boolean = false
)

typealias MiniPlayerColorScheme = PlayerColorScheme

data class ExtractedAlbumCoverTones(
    val dominant: Color,
    val topTone: Color,
    val bottomTone: Color,
    val accent: Color
)

/**
 * Extracts multiple coherent tonal points from the album cover bitmap
 * to construct an authentic, rich gradient reflecting the artwork.
 */
fun extractCoverTonesFromBitmap(bitmap: Bitmap): ExtractedAlbumCoverTones {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) {
        val fallback = Color(0xFF232530)
        return ExtractedAlbumCoverTones(fallback, fallback, fallback, Color(0xFF5B8DEF))
    }

    val hsv = FloatArray(3)
    val halfH = (height / 2).coerceAtLeast(1)

    var topScore = -1f
    var topPixel = android.graphics.Color.DKGRAY

    var bottomScore = -1f
    var bottomPixel = android.graphics.Color.DKGRAY

    var maxVibrancyScore = -1f
    var dominantPixel = android.graphics.Color.DKGRAY

    // 1. Sample upper half
    for (y in 0 until halfH step 2) {
        for (x in 0 until width step 2) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 120) continue
            android.graphics.Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            if (value in 0.12f..0.96f) {
                val score = sat * 2.5f + (1f - abs(value - 0.5f))
                if (score > topScore) {
                    topScore = score
                    topPixel = pixel
                }
                if (score > maxVibrancyScore) {
                    maxVibrancyScore = score
                    dominantPixel = pixel
                }
            }
        }
    }

    // 2. Sample lower half
    for (y in halfH until height step 2) {
        for (x in 0 until width step 2) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 120) continue
            android.graphics.Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            if (value in 0.12f..0.96f) {
                val score = sat * 2.5f + (1f - abs(value - 0.5f))
                if (score > bottomScore) {
                    bottomScore = score
                    bottomPixel = pixel
                }
                if (score > maxVibrancyScore) {
                    maxVibrancyScore = score
                    dominantPixel = pixel
                }
            }
        }
    }

    if (dominantPixel == android.graphics.Color.DKGRAY && width > 0 && height > 0) {
        dominantPixel = bitmap.getPixel(width / 2, height / 2)
        topPixel = bitmap.getPixel(width / 2, height / 4)
        bottomPixel = bitmap.getPixel(width / 2, (height * 3) / 4)
    }

    // 3. Find distinct vibrant accent pixel
    val domHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(dominantPixel, domHsv)
    var bestAccentPixel = dominantPixel
    var bestAccentScore = -1f

    for (y in 0 until height step 3) {
        for (x in 0 until width step 3) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 120) continue
            android.graphics.Color.colorToHSV(pixel, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            if (sat > 0.35f && value in 0.35f..0.95f) {
                val diff = abs(hsv[0] - domHsv[0])
                val hueDiff = if (diff > 180f) 360f - diff else diff
                val accentScore = sat * 2f + (hueDiff / 180f) * 2.5f
                if (accentScore > bestAccentScore) {
                    bestAccentScore = accentScore
                    bestAccentPixel = pixel
                }
            }
        }
    }

    return ExtractedAlbumCoverTones(
        dominant = Color(dominantPixel),
        topTone = Color(topPixel),
        bottomTone = Color(bottomPixel),
        accent = Color(bestAccentPixel)
    )
}

suspend fun loadAlbumCoverTones(context: Context, uri: Uri?): ExtractedAlbumCoverTones? = withContext(Dispatchers.IO) {
    if (uri == null) return@withContext null
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(64, 64)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                return@withContext extractCoverTonesFromBitmap(bitmap)
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

/**
 * Generates an atmospheric 3-stop gradient in the exact tones of the album cover.
 */
data class AlbumGradientStops(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val accent: Color
)

fun generateAlbumGradientStops(tones: ExtractedAlbumCoverTones): AlbumGradientStops {
    val hsv = FloatArray(3)

    // Top stop: atmospheric tone with top hue and balanced darkness
    val topHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(tones.topTone.toArgb(), topHsv)
    val baseTop = if (topHsv[1] > 0.12f) tones.topTone else tones.dominant
    android.graphics.Color.colorToHSV(baseTop.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.72f).coerceIn(0.28f, 0.75f)
    hsv[2] = 0.28f // Deep atmospheric luminance for superior contrast with white text
    val topStop = Color(android.graphics.Color.HSVToColor(hsv))

    // Middle stop: ambient transition derived from dominant tone
    android.graphics.Color.colorToHSV(tones.dominant.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.58f).coerceIn(0.20f, 0.65f)
    hsv[2] = 0.17f
    val middleStop = Color(android.graphics.Color.HSVToColor(hsv))

    // Bottom stop: rich, deep dark foundation tone
    val bottomHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(tones.bottomTone.toArgb(), bottomHsv)
    val baseBottom = if (bottomHsv[1] > 0.12f) tones.bottomTone else tones.dominant
    android.graphics.Color.colorToHSV(baseBottom.toArgb(), hsv)
    hsv[1] = (hsv[1] * 0.42f).coerceIn(0.15f, 0.50f)
    hsv[2] = 0.09f // Sleek deep tone
    val bottomStop = Color(android.graphics.Color.HSVToColor(hsv))

    // Vibrant accent for play button, sliders, and active controls
    android.graphics.Color.colorToHSV(tones.accent.toArgb(), hsv)
    hsv[1] = (hsv[1] * 1.8f).coerceIn(0.68f, 0.96f)
    hsv[2] = 0.90f
    val accentColor = Color(android.graphics.Color.HSVToColor(hsv))

    return AlbumGradientStops(
        top = topStop,
        middle = middleStop,
        bottom = bottomStop,
        accent = accentColor
    )
}

/**
 * Returns dynamic styling for both full-screen PlayerScreen and MiniPlayer.
 * In ALBUM_ART mode, builds smooth gradient brushes from the cover's color tones.
 */
@Composable
fun rememberPlayerColors(
    bgMode: MiniPlayerBgMode,
    albumArtUri: Uri?,
    customColorLong: Long
): PlayerColorScheme {
    val context = LocalContext.current
    val systemSurface = MaterialTheme.colorScheme.surface
    val systemOnSurface = MaterialTheme.colorScheme.onSurface
    val systemOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val systemPrimary = MaterialTheme.colorScheme.primary
    val systemOnPrimary = MaterialTheme.colorScheme.onPrimary
    val systemSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var extractedTones by remember(albumArtUri) { mutableStateOf<ExtractedAlbumCoverTones?>(null) }

    LaunchedEffect(albumArtUri, bgMode) {
        if (bgMode == MiniPlayerBgMode.ALBUM_ART && albumArtUri != null) {
            extractedTones = loadAlbumCoverTones(context, albumArtUri)
        } else {
            extractedTones = null
        }
    }

    // Default neutral dark gradient stops when album art is loading or unavailable
    val defaultDarkGradient = remember {
        AlbumGradientStops(
            top = Color(0xFF262834),
            middle = Color(0xFF1B1C24),
            bottom = Color(0xFF111217),
            accent = systemPrimary
        )
    }

    val (targetTop, targetMiddle, targetBottom, targetAccent, isGradient, isDark, onSurface, onSurfaceVariant, onAccent, trackColor) =
        when (bgMode) {
            MiniPlayerBgMode.ALBUM_ART -> {
                val stops = extractedTones?.let { generateAlbumGradientStops(it) } ?: defaultDarkGradient
                TargetParams(
                    top = stops.top,
                    middle = stops.middle,
                    bottom = stops.bottom,
                    accent = stops.accent,
                    isGradient = true,
                    isDark = true,
                    onSurface = Color(0xFFF7F7FA),
                    onSurfaceVariant = Color(0xFFB4B4C0),
                    onAccent = Color(0xFF0D0E12),
                    trackColor = Color(0x38FFFFFF)
                )
            }
            MiniPlayerBgMode.LIGHT -> {
                val base = if (customColorLong != 0L) Color(customColorLong) else Color(0xFFFFFFFF)
                TargetParams(
                    top = base,
                    middle = base,
                    bottom = base,
                    accent = Color(0xFF2656D6),
                    isGradient = false,
                    isDark = false,
                    onSurface = Color(0xFF18191E),
                    onSurfaceVariant = Color(0xFF555660),
                    onAccent = Color.White,
                    trackColor = Color(0xFFE2E2EA)
                )
            }
            MiniPlayerBgMode.DARK -> {
                val base = if (customColorLong != 0L) Color(customColorLong) else Color(0xFF17171A)
                TargetParams(
                    top = base,
                    middle = base,
                    bottom = base,
                    accent = Color(0xFF6B9BFA),
                    isGradient = false,
                    isDark = true,
                    onSurface = Color(0xFFF3F3F7),
                    onSurfaceVariant = Color(0xFFA0A0AA),
                    onAccent = Color(0xFF0F1014),
                    trackColor = Color(0xFF27282F)
                )
            }
            MiniPlayerBgMode.CUSTOM -> {
                val base = if (customColorLong != 0L) Color(customColorLong) else Color(0xFF2C3E50)
                val isCustomDark = (0.299f * base.red + 0.587f * base.green + 0.114f * base.blue) < 0.55f
                if (isCustomDark) {
                    TargetParams(
                        top = base,
                        middle = base,
                        bottom = base,
                        accent = Color(0xFF80D8FF),
                        isGradient = false,
                        isDark = true,
                        onSurface = Color(0xFFFAFAFC),
                        onSurfaceVariant = Color(0xFFCCCCCC),
                        onAccent = Color(0xFF002233),
                        trackColor = Color(0x33FFFFFF)
                    )
                } else {
                    TargetParams(
                        top = base,
                        middle = base,
                        bottom = base,
                        accent = Color(0xFF1976D2),
                        isGradient = false,
                        isDark = false,
                        onSurface = Color(0xFF151518),
                        onSurfaceVariant = Color(0xFF4A4A52),
                        onAccent = Color.White,
                        trackColor = Color(0x28000000)
                    )
                }
            }
            MiniPlayerBgMode.SYSTEM -> {
                TargetParams(
                    top = systemSurface,
                    middle = systemSurface,
                    bottom = systemSurface,
                    accent = systemPrimary,
                    isGradient = false,
                    isDark = false,
                    onSurface = systemOnSurface,
                    onSurfaceVariant = systemOnSurfaceVariant,
                    onAccent = systemOnPrimary,
                    trackColor = systemSurfaceVariant
                )
            }
        }

    // Smoothly animate each color stop
    val animTop by animateColorAsState(targetTop, animationSpec = tween(450), label = "grad_top")
    val animMiddle by animateColorAsState(targetMiddle, animationSpec = tween(450), label = "grad_middle")
    val animBottom by animateColorAsState(targetBottom, animationSpec = tween(450), label = "grad_bottom")
    val animAccent by animateColorAsState(targetAccent, animationSpec = tween(450), label = "grad_accent")
    val animOnSurface by animateColorAsState(onSurface, animationSpec = tween(450), label = "grad_on_surface")
    val animOnSurfaceVariant by animateColorAsState(onSurfaceVariant, animationSpec = tween(450), label = "grad_on_var")
    val animOnAccent by animateColorAsState(onAccent, animationSpec = tween(450), label = "grad_on_acc")
    val animTrack by animateColorAsState(trackColor, animationSpec = tween(450), label = "grad_track")

    val gradientStopsList = remember(animTop, animMiddle, animBottom) {
        listOf(animTop, animMiddle, animBottom)
    }

    // Vertical gradient for full expanded PlayerScreen
    val playerScreenBrush = if (isGradient) {
        Brush.verticalGradient(colors = gradientStopsList)
    } else null

    // Linear gradient for collapsed MiniPlayer
    val miniPlayerBrush = if (isGradient) {
        Brush.linearGradient(colors = gradientStopsList)
    } else null

    return PlayerColorScheme(
        backgroundColor = animTop,
        backgroundBrush = playerScreenBrush,
        miniPlayerBrush = miniPlayerBrush,
        gradientColors = gradientStopsList,
        onSurfaceColor = animOnSurface,
        onSurfaceVariantColor = animOnSurfaceVariant,
        accentColor = animAccent,
        onAccentColor = animOnAccent,
        progressTrackColor = animTrack,
        isDark = isDark,
        isGradient = isGradient
    )
}

@Composable
fun rememberMiniPlayerColors(
    bgMode: MiniPlayerBgMode,
    albumArtUri: Uri?,
    customColorLong: Long
): PlayerColorScheme = rememberPlayerColors(bgMode, albumArtUri, customColorLong)

private data class TargetParams(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val accent: Color,
    val isGradient: Boolean,
    val isDark: Boolean,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onAccent: Color,
    val trackColor: Color
)
