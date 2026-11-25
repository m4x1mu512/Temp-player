package com.example.ui.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * True when the device can blur on the GPU.
 *
 * RenderEffect arrived in Android 12 (API 31). The Backdrop library, which draws the glass
 * surfaces, is built on it and silently draws the content unblurred below that level.
 */
val gpuBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * A CPU blurred copy of [painter], used as the glass background where there is no GPU blur.
 *
 * The picture is rasterised into a small bitmap, blurred and then scaled up by the drawing
 * code. That is fast enough to do once per cover and looks close to the GPU result.
 *
 * @param key identifies the content of the painter. The painter itself cannot be used for
 * that: `painterResource` creates a new instance on every recomposition, which would restart
 * the blur over and over.
 * @param enabled the blur is skipped while this is false, so the work is not done on the
 * devices that do not need it.
 */
@Composable
fun rememberBlurredArtwork(
    painter: Painter?,
    key: Any?,
    enabled: Boolean = true,
    size: Int = 192,
    radius: Int = 8
): ImageBitmap? {
    val latestPainter by rememberUpdatedState(painter)
    var blurred by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(key, enabled, size, radius) {
        blurred = if (enabled) {
            withContext(Dispatchers.Default) { latestPainter?.let { blurArtwork(it, size, radius) } }
        } else {
            null
        }
    }
    return blurred
}

/**
 * Draws the picture the way ContentScale.Crop does: it fills the target and keeps the aspect
 * ratio of the picture.
 */
internal fun DrawScope.drawCroppedImage(image: ImageBitmap) {
    val targetWidth = size.width
    val targetHeight = size.height
    if (targetWidth <= 0f || targetHeight <= 0f) return
    val scale = maxOf(targetWidth / image.width, targetHeight / image.height)
    if (scale <= 0f) return
    val srcWidth = (targetWidth / scale).coerceAtMost(image.width.toFloat()).roundToInt()
    val srcHeight = (targetHeight / scale).coerceAtMost(image.height.toFloat()).roundToInt()
    drawImage(
        image = image,
        srcOffset = IntOffset(
            ((image.width - srcWidth) / 2).roundToInt(),
            ((image.height - srcHeight) / 2).roundToInt()
        ),
        srcSize = IntSize(srcWidth, srcHeight),
        dstSize = IntSize(targetWidth.roundToInt(), targetHeight.roundToInt())
    )
}

/**
 * The background of a glass surface on a device without a GPU blur.
 *
 * The Backdrop library draws the captured layer through a RenderEffect, which needs Android 12.
 * Below that the cover is blurred on the CPU and drawn directly, with the same surface
 * overlays on top.
 */
internal fun Modifier.blurredGlassBackground(
    artwork: ImageBitmap?,
    shape: Shape,
    onDrawSurface: DrawScope.() -> Unit = {}
): Modifier = this
    .clip(shape)
    .drawBehind {
        if (artwork != null) drawCroppedImage(artwork)
        onDrawSurface()
    }

/**
 * Rasterises the painter into a small bitmap and blurs it.
 *
 * The previous result stays visible while the next one is computed, so a track change does not
 * flash an empty background.
 */
private fun blurArtwork(painter: Painter, size: Int, radius: Int): ImageBitmap? {
    return try {
        val image = ImageBitmap(size, size)
        val canvas = Canvas(image)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(size.toFloat(), size.toFloat())
        ) {
            with(painter) { draw(size) }
        }
        val bitmap = image.asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val blurred = CpuBlur.blur(pixels, bitmap.width, bitmap.height, radius)
        bitmap.setPixels(blurred, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        image
    } catch (_: Exception) {
        // A cover that cannot be rasterised simply leaves the background empty.
        null
    }
}
