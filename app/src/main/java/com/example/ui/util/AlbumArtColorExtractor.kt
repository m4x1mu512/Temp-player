package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.MiniPlayerBgMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlayerColorScheme(
    val backgroundColor: Color,
    val backgroundBrush: Brush? = null,
    val miniPlayerBrush: Brush? = null,
    val gradientColors: List<Color> = emptyList(),
    val vibrantColor: Color = Color.Unspecified,
    val darkVibrantColor: Color = Color.Unspecified,
    val mutedColor: Color = Color.Unspecified,
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
    val vibrant: Color,
    val darkVibrant: Color,
    val muted: Color,
    val dominant: Color,
    val accent: Color
)

/**
 * Extracts VibrantColor, DarkVibrantColor, and MutedColor from the album cover bitmap
 * using AndroidX Palette API (androidx.palette:palette-ktx).
 */
fun extractCoverTonesFromBitmap(bitmap: Bitmap): ExtractedAlbumCoverTones {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) {
        val fallback = Color(0xFF151A11)
        return ExtractedAlbumCoverTones(fallback, fallback, fallback, fallback, Color(0xFFC5F45B))
    }

    // Analyze a small, software bitmap: Palette is inexpensive and the result is cached
    // by Coil + rememberPlayerColors for the current track.
    val palette = Palette.from(bitmap)
        .maximumColorCount(32)
        .resizeBitmapArea(16_384)
        .clearFilters()
        .generate()

    // Prefer colors with enough chroma and usable luminance; extreme black/white
    // swatches remain available as fallbacks but don't dominate the gradient.

    val fallbackVibrant = android.graphics.Color.parseColor("#6B7D3A")
    val fallbackDarkVibrant = android.graphics.Color.parseColor("#20271A")
    val fallbackMuted = android.graphics.Color.parseColor("#293020")
    val fallbackDominant = android.graphics.Color.parseColor("#11150D")

    fun isAestheticSwatch(swatch: Palette.Swatch?): Boolean {
        if (swatch == null) return false
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
        return hsv[1] in 0.18f..0.88f && hsv[2] in 0.16f..0.94f
    }

    val swatches = palette.swatches
    val aestheticSwatches = swatches.filter { isAestheticSwatch(it) }
    val vibrantRgb = (palette.vibrantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: palette.lightVibrantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: aestheticSwatches.maxByOrNull { it.population * (0.6f + it.hsl[1]) }
        )?.rgb ?: fallbackVibrant
    val darkVibrantRgb = (palette.darkVibrantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: palette.darkMutedSwatch?.takeIf { isAestheticSwatch(it) }
        ?: aestheticSwatches.minByOrNull { it.hsl[2] })?.rgb ?: fallbackDarkVibrant
    val mutedRgb = (palette.mutedSwatch?.takeIf { isAestheticSwatch(it) }
        ?: palette.lightMutedSwatch?.takeIf { isAestheticSwatch(it) }
        ?: aestheticSwatches.minByOrNull { kotlin.math.abs(it.hsl[1] - 0.42f) })?.rgb ?: fallbackMuted
    val dominantRgb = (palette.dominantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: aestheticSwatches.maxByOrNull { it.population })?.rgb ?: fallbackDominant
    val accentRgb = (palette.vibrantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: palette.lightVibrantSwatch?.takeIf { isAestheticSwatch(it) }
        ?: aestheticSwatches.maxByOrNull { it.hsl[1] })?.rgb ?: fallbackVibrant

    return ExtractedAlbumCoverTones(
        vibrant = Color(vibrantRgb),
        darkVibrant = Color(darkVibrantRgb),
        muted = Color(mutedRgb),
        dominant = Color(dominantRgb),
        accent = Color(accentRgb)
    )
}

fun generateDeterministicTones(seed: String): ExtractedAlbumCoverTones {
    val hash = kotlin.math.abs(seed.hashCode())
    val hue = (hash % 360).toFloat()
    val vibrant = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.72f, 0.70f)))
    val darkVibrant = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 15f) % 360f, 0.65f, 0.35f)))
    val muted = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 30f) % 360f, 0.40f, 0.20f)))
    val dominant = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.50f, 0.25f)))
    val accent = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 180f) % 360f, 0.85f, 0.95f)))
    return ExtractedAlbumCoverTones(vibrant, darkVibrant, muted, dominant, accent)
}

suspend fun loadAlbumCoverTones(
    context: Context,
    uri: Uri?,
    fallbackSeed: String = ""
): ExtractedAlbumCoverTones = withContext(Dispatchers.IO) {
    if (uri != null) {
        try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(128, 128)
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
        } catch (_: Exception) {
            // ignore and fallback
        }
    }
    try {
        val defaultDrawable = androidx.core.content.ContextCompat.getDrawable(context, com.example.R.drawable.ic_default_art)
        if (defaultDrawable != null && fallbackSeed.isBlank()) {
            val bitmap = (defaultDrawable as? BitmapDrawable)?.bitmap
                ?: Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = android.graphics.Canvas(bmp)
                    defaultDrawable.setBounds(0, 0, 128, 128)
                    defaultDrawable.draw(canvas)
                }
            return@withContext extractCoverTonesFromBitmap(bitmap)
        }
    } catch (_: Exception) {
        // ignore
    }
    generateDeterministicTones(fallbackSeed.ifEmpty { "DefaultAlbumCoverTones" })
}

/**
 * Generates vertical gradient stops using VibrantColor, DarkVibrantColor, and MutedColor.
 */
data class AlbumGradientStops(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val accent: Color
)

fun generateAlbumGradientStops(
    tones: ExtractedAlbumCoverTones,
    isLightTheme: Boolean = false
): AlbumGradientStops {
    fun tunedColor(source: Color, saturationScale: Float, value: Float, hueShift: Float = 0f): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(source.toArgb(), hsv)
        hsv[0] = (hsv[0] + hueShift + 360f) % 360f
        // Avoid muddy greys and neon saturation while retaining the cover's color identity.
        hsv[1] = (hsv[1] * saturationScale).coerceIn(0.18f, if (isLightTheme) 0.52f else 0.72f)
        hsv[2] = value
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    val top = tunedColor(
        tones.vibrant,
        saturationScale = if (isLightTheme) 0.48f else 0.72f,
        value = if (isLightTheme) 0.96f else 0.28f,
        hueShift = -5f
    )
    val middle = tunedColor(
        tones.dominant,
        saturationScale = if (isLightTheme) 0.38f else 0.62f,
        value = if (isLightTheme) 0.91f else 0.19f,
        hueShift = 8f
    )
    val bottom = tunedColor(
        tones.muted,
        saturationScale = if (isLightTheme) 0.30f else 0.48f,
        value = if (isLightTheme) 0.985f else 0.105f,
        hueShift = -9f
    )
    val accent = tunedColor(
        tones.accent,
        saturationScale = if (isLightTheme) 0.72f else 0.88f,
        value = if (isLightTheme) 0.48f else 0.78f
    )

    return AlbumGradientStops(top = top, middle = middle, bottom = bottom, accent = accent)
}
/**
 * Returns dynamic styling for both full-screen PlayerScreen and MiniPlayer.
 * In ALBUM_ART mode, builds smooth gradient brushes from the cover's color tones.
 */
@Composable
fun rememberPlayerColors(
    bgMode: MiniPlayerBgMode,
    albumArtUri: Uri?,
    customColorLong: Long,
    trackKey: String = albumArtUri?.toString() ?: ""
): PlayerColorScheme {
    val context = LocalContext.current
    val systemSurface = MaterialTheme.colorScheme.surface
    val systemOnSurface = MaterialTheme.colorScheme.onSurface
    val systemOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val systemPrimary = MaterialTheme.colorScheme.primary
    val systemOnPrimary = MaterialTheme.colorScheme.onPrimary
    val systemSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var extractedTones by remember(albumArtUri, trackKey) { mutableStateOf<ExtractedAlbumCoverTones?>(null) }

    LaunchedEffect(albumArtUri, bgMode, trackKey) {
        if (bgMode == MiniPlayerBgMode.ALBUM_ART) {
            extractedTones = loadAlbumCoverTones(context, albumArtUri, trackKey)
        } else {
            extractedTones = null
        }
    }

    // Resolve explicit light/dark/system themes from the active Material surface.
    // This also follows Android's system theme when the app theme is set to System.
    val isLightAppTheme = systemSurface.luminance() > 0.5f

    val defaultGradient = remember(isLightAppTheme, systemPrimary) {
        if (isLightAppTheme) {
            AlbumGradientStops(
                top = Color(0xFFF0EAF5),
                middle = Color(0xFFF8EEE7),
                bottom = Color(0xFFEAF1F4),
                accent = systemPrimary
            )
        } else {
            AlbumGradientStops(
                top = Color(0xFF252330),
                middle = Color(0xFF191A25),
                bottom = Color(0xFF0D1018),
                accent = systemPrimary
            )
        }
    }

    // Convert the extracted palette only when artwork or the active theme changes.
    val albumArtStops = remember(extractedTones, defaultGradient, isLightAppTheme) {
        extractedTones?.let { generateAlbumGradientStops(it, isLightAppTheme) } ?: defaultGradient
    }

    val (targetTop, targetMiddle, targetBottom, targetAccent, isGradient, isDark, onSurface, onSurfaceVariant, onAccent, trackColor) =
        when (bgMode) {
            MiniPlayerBgMode.ALBUM_ART -> {
                val stops = albumArtStops
                TargetParams(
                    top = stops.top,
                    middle = stops.middle,
                    bottom = stops.bottom,
                    accent = stops.accent,
                    isGradient = true,
                    isDark = !isLightAppTheme,
                    onSurface = if (isLightAppTheme) Color(0xFF20212A) else Color(0xFFF5F4FA),
                    onSurfaceVariant = if (isLightAppTheme) Color(0xFF555563) else Color(0xFFC0BECC),
                    onAccent = if (isLightAppTheme) Color.White else Color(0xFF15131B),
                    trackColor = if (isLightAppTheme) Color(0x33232630) else Color(0x38FFFFFF)
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
                // Match the current-track row highlight in the playback list:
                // TrackListItem uses primary at 9% alpha over the normal surface.
                val selectedTrackBackground = systemPrimary
                    .copy(alpha = 0.09f)
                    .compositeOver(systemSurface)
                TargetParams(
                    top = selectedTrackBackground,
                    middle = selectedTrackBackground,
                    bottom = selectedTrackBackground,
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

    // Smoothly animate each color stop over 800–1000 ms (~900 ms) using FastOutSlowInEasing
    val colorAnimSpec = tween<Color>(durationMillis = 900, easing = FastOutSlowInEasing)
    val animTop by animateColorAsState(targetTop, animationSpec = colorAnimSpec, label = "grad_vibrant")
    val animMiddle by animateColorAsState(targetMiddle, animationSpec = colorAnimSpec, label = "grad_dark_vibrant")
    val animBottom by animateColorAsState(targetBottom, animationSpec = colorAnimSpec, label = "grad_muted")
    val animAccent by animateColorAsState(targetAccent, animationSpec = colorAnimSpec, label = "grad_accent")
    val animOnSurface by animateColorAsState(onSurface, animationSpec = colorAnimSpec, label = "grad_on_surface")
    val animOnSurfaceVariant by animateColorAsState(onSurfaceVariant, animationSpec = colorAnimSpec, label = "grad_on_var")
    val animOnAccent by animateColorAsState(onAccent, animationSpec = colorAnimSpec, label = "grad_on_acc")
    val animTrack by animateColorAsState(trackColor, animationSpec = colorAnimSpec, label = "grad_track")

    val gradientStopsList = remember(animTop, animMiddle, animBottom) {
        listOf(animTop, animMiddle, animBottom)
    }

    // Vertical gradient for full expanded PlayerScreen occupying Modifier.fillMaxSize()
    val playerScreenBrush = remember(isGradient, gradientStopsList) {
        if (isGradient) Brush.verticalGradient(colors = gradientStopsList) else null
    }

    // In a light app theme, keep the album-art palette but lift the mini-player
    // colors so the collapsed/expanded mini-player does not look unnecessarily dark.
    // The full-screen player keeps its original album-art gradient.
    fun liftForLightTheme(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        // Stronger lift: make album-derived colors visibly lighter in light mode.
        hsv[2] = (hsv[2] + (1f - hsv[2]) * 0.68f).coerceIn(0f, 1f)
        hsv[1] = (hsv[1] * 0.72f).coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(color.alpha.let { (it * 255).toInt() }, hsv))
    }

    // Cache HSV conversions until the animated color or theme actually changes.
    // These values are read during color animation, so avoiding repeated allocations
    // here helps older devices keep up with the same visual transition.
    val miniTop = remember(animTop, isLightAppTheme, isGradient) {
        if (isLightAppTheme && isGradient) liftForLightTheme(animTop) else animTop
    }
    val miniMiddle = remember(animMiddle, isLightAppTheme, isGradient) {
        if (isLightAppTheme && isGradient) liftForLightTheme(animMiddle) else animMiddle
    }
    val miniBottom = remember(animBottom, isLightAppTheme, isGradient) {
        if (isLightAppTheme && isGradient) liftForLightTheme(animBottom) else animBottom
    }

    // Keep the brush stable between frames when its actual colors have not changed.
    val miniPlayerHorizontalBrush = remember(
        isGradient, miniTop, miniMiddle, miniBottom, isLightAppTheme
    ) {
        if (isGradient) {
            Brush.horizontalGradient(
                colors = listOf(
                    miniTop.copy(alpha = if (isLightAppTheme) 0.72f else 0.35f),
                    miniMiddle.copy(alpha = if (isLightAppTheme) 0.62f else 0.20f),
                    miniBottom.copy(alpha = if (isLightAppTheme) 0.72f else 0.35f)
                )
            )
        } else null
    }

    return PlayerColorScheme(
        backgroundColor = animTop,
        backgroundBrush = playerScreenBrush,
        miniPlayerBrush = miniPlayerHorizontalBrush,
        gradientColors = gradientStopsList,
        vibrantColor = animTop,
        darkVibrantColor = animMiddle,
        mutedColor = animBottom,
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
    customColorLong: Long,
    trackKey: String = albumArtUri?.toString() ?: ""
): PlayerColorScheme = rememberPlayerColors(bgMode, albumArtUri, customColorLong, trackKey)

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
