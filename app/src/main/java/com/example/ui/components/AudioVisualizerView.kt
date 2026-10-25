package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VisualizerMode
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTurquoise
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val NeonMagenta = Color(0xFFFF35E8)
private val NeonBlue = Color(0xFF397BFF)

@Composable
fun AudioVisualizerView(
    fftDataFlow: StateFlow<FloatArray>,
    waveformDataFlow: StateFlow<FloatArray>,
    amplitudeFlow: StateFlow<Float>,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    neonColorStart: Color = NeonCyan,
    neonColorMiddle: Color = NeonTurquoise,
    neonColorEnd: Color = NeonPurple
) {
    // Subscribe only to the signals used by the selected visualizer. The audio controller
    // publishes these flows frequently; collecting all three here made every mode recompose
    // for data it never draws.
    when (mode) {
        VisualizerMode.AMPLITUDE -> {
            val waveformData by waveformDataFlow.collectAsStateWithLifecycle()
            val amplitude by amplitudeFlow.collectAsStateWithLifecycle()
            AudioVisualizerView(
                fftData = waveformData,
                waveformData = waveformData,
                amplitude = amplitude,
                mode = mode,
                isPlaying = isPlaying,
                modifier = modifier,
                neonColorStart = neonColorStart,
                neonColorMiddle = neonColorMiddle,
                neonColorEnd = neonColorEnd
            )
        }
        VisualizerMode.WAVE -> {
            val waveformData by waveformDataFlow.collectAsStateWithLifecycle()
            AudioVisualizerView(
                fftData = waveformData,
                waveformData = waveformData,
                mode = mode,
                isPlaying = isPlaying,
                modifier = modifier,
                neonColorStart = neonColorStart,
                neonColorMiddle = neonColorMiddle,
                neonColorEnd = neonColorEnd
            )
        }
        VisualizerMode.SPECTRUM, VisualizerMode.CIRCLE -> {
            val fftData by fftDataFlow.collectAsStateWithLifecycle()
            AudioVisualizerView(
                fftData = fftData,
                mode = mode,
                isPlaying = isPlaying,
                modifier = modifier,
                neonColorStart = neonColorStart,
                neonColorMiddle = neonColorMiddle,
                neonColorEnd = neonColorEnd
            )
        }
    }
}

@Composable
fun AudioVisualizerView(
    fftData: FloatArray,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveformData: FloatArray = fftData,
    amplitude: Float = 0f,
    neonColorStart: Color = NeonCyan,
    neonColorMiddle: Color = NeonTurquoise,
    neonColorEnd: Color = NeonPurple
) {
    Box(modifier = modifier.testTag("audio_visualizer_view")) {
        when (mode) {
            VisualizerMode.AMPLITUDE -> RibbonVisualizer(
                data = waveformData, amplitude = amplitude, isPlaying = isPlaying,
                colorStart = neonColorStart, colorMiddle = neonColorMiddle,
                colorEnd = neonColorEnd, modifier = Modifier.fillMaxSize()
            )
            VisualizerMode.SPECTRUM -> SpectrumVisualizer(
                data = fftData, isPlaying = isPlaying, colorStart = neonColorStart,
                colorMiddle = neonColorMiddle, colorEnd = neonColorEnd,
                modifier = Modifier.fillMaxSize()
            )
            VisualizerMode.WAVE -> WaveVisualizer(
                data = waveformData, isPlaying = isPlaying, colorStart = neonColorStart,
                colorMiddle = neonColorMiddle, colorEnd = neonColorEnd,
                modifier = Modifier.fillMaxSize()
            )
            VisualizerMode.CIRCLE -> EqualizerVisualizer(
                data = fftData, isPlaying = isPlaying, colorStart = neonColorStart,
                colorMiddle = neonColorMiddle, colorEnd = neonColorEnd,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RibbonVisualizer(
    data: FloatArray,
    amplitude: Float,
    isPlaying: Boolean,
    colorStart: Color,
    colorMiddle: Color,
    colorEnd: Color,
    modifier: Modifier
) {
    val lineBrush = remember(colorStart, colorMiddle, colorEnd) {
        Brush.horizontalGradient(listOf(NeonMagenta, colorEnd, NeonBlue, colorStart))
    }
    val glowBrush = remember(colorStart, colorMiddle, colorEnd) {
        Brush.verticalGradient(
            listOf(Color.Transparent, colorMiddle.copy(alpha = 0.18f), colorStart.copy(alpha = 0.32f), Color.Transparent)
        )
    }
    val ribbonFillBrush = remember(colorMiddle) {
        Brush.verticalGradient(listOf(Color.Transparent, colorMiddle.copy(alpha = 0.12f), Color.Transparent))
    }
    // Reuse draw geometry between audio frames instead of allocating nine Paths per frame.
    val path = remember { Path() }
    val fill = remember { Path() }
    val ribbonPaths = remember { List(7) { Path() } }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val mid = h * 0.5f
        val count = data.size.coerceIn(16, 64)
        val step = w / (count - 1).coerceAtLeast(1)
        path.reset()
        fill.reset()
        path.moveTo(0f, mid)
        fill.moveTo(0f, h)
        fill.lineTo(0f, mid)
        for (i in 0 until count) {
            val x = i * step
            val raw = if (isPlaying && i < data.size) data[i].coerceIn(0.04f, 1f) else 0.035f
            val envelope = (0.32f + 0.68f * sin(PI * i / (count - 1)).toFloat())
            val y = mid - (raw * envelope * (0.44f + amplitude.coerceIn(0f, 1f) * 0.08f) * h)
            if (i == 0) {
                path.moveTo(x, y)
                fill.lineTo(x, y)
            } else {
                val px = (i - 1) * step
                val prevRaw = if (isPlaying && i - 1 < data.size) data[i - 1].coerceIn(0.04f, 1f) else 0.035f
                val prevEnv = 0.32f + 0.68f * sin(PI * (i - 1) / (count - 1)).toFloat()
                val py = mid - (prevRaw * prevEnv * (0.44f + amplitude.coerceIn(0f, 1f) * 0.08f) * h)
                val cx = (px + x) * 0.5f
                path.cubicTo(cx, py, cx, y, x, y)
                fill.cubicTo(cx, py, cx, y, x, y)
            }
        }
        fill.lineTo(w, h)
        fill.close()
        drawRect(brush = glowBrush, alpha = if (isPlaying) 0.8f else 0.25f)
        drawPath(fill, ribbonFillBrush)
        // Multiple fine ribbons create the luminous, layered silk-wave look without blur layers.
        for (ribbon in 0 until ribbonPaths.size) {
            val ribbonPath = ribbonPaths[ribbon]
            ribbonPath.reset()
            val phase = ribbon * 0.48f
            for (i in 0 until count) {
                val x = i * step
                val raw = if (isPlaying && i < data.size) data[i].coerceIn(0.04f, 1f) else 0.035f
                val envelope = 0.25f + 0.75f * sin(PI * i / (count - 1)).toFloat()
                val y = mid + sin(i * 0.55f + phase).toFloat() * raw * envelope * h * 0.32f
                if (i == 0) ribbonPath.moveTo(x, y) else {
                    val px = (i - 1) * step
                    val prevRaw = if (isPlaying && i - 1 < data.size) data[i - 1].coerceIn(0.04f, 1f) else 0.035f
                    val prevEnv = 0.25f + 0.75f * sin(PI * (i - 1) / (count - 1)).toFloat()
                    val py = mid + sin((i - 1) * 0.55f + phase).toFloat() * prevRaw * prevEnv * h * 0.32f
                    ribbonPath.cubicTo((px + x) * 0.5f, py, (px + x) * 0.5f, y, x, y)
                }
            }
            drawPath(ribbonPath, lineBrush, style = Stroke(width = if (ribbon == 3) 1.8f else 0.8f, cap = StrokeCap.Round), alpha = if (isPlaying) 0.22f + ribbon * 0.055f else 0.18f)
        }
        drawPath(path, lineBrush, style = Stroke(width = 1.8f, cap = StrokeCap.Round), alpha = if (isPlaying) 1f else 0.4f)
        drawLine(colorMiddle.copy(alpha = if (isPlaying) 0.8f else 0.22f), Offset(0f, mid), Offset(w, mid), strokeWidth = 0.7f)
        if (isPlaying) {
            for (i in 2 until count step 5) {
                val x = i * step
                val raw = if (i < data.size) data[i].coerceIn(0f, 1f) else 0.1f
                val y = mid - raw * h * 0.32f
                drawCircle(color = colorStart.copy(alpha = 0.3f), radius = 2.1f, center = Offset(x, y))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 0.7f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun SpectrumVisualizer(
    data: FloatArray,
    isPlaying: Boolean,
    colorStart: Color,
    colorMiddle: Color,
    colorEnd: Color,
    modifier: Modifier
) {
    val barBrush = remember(colorStart, colorMiddle, colorEnd) {
        Brush.verticalGradient(listOf(Color(0xFF8A32FF), colorEnd, colorStart, Color(0xFF28E8FF)))
    }
    val centerGlowBrush = remember(colorMiddle) {
        Brush.horizontalGradient(listOf(Color.Transparent, NeonMagenta.copy(alpha = 0.7f), colorMiddle.copy(alpha = 0.9f), Color.Transparent))
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val mid = h * 0.52f
        val count = data.size.coerceIn(24, 64)
        val step = w / count
        val centerWidth = (step * 0.12f).coerceAtLeast(0.8f)
        // Continuous center glow keeps the entire frequency strip visually alive.
        drawLine(centerGlowBrush, Offset(0f, mid), Offset(w, mid), strokeWidth = (h * 0.07f).coerceAtLeast(1f))
        for (i in 0 until count) {
            val raw = if (isPlaying && i < data.size) data[i].coerceIn(0f, 1f) else 0.035f
            val floor = if (isPlaying) 0.16f else 0.035f
            val value = (floor + raw * 0.84f).coerceIn(floor, 1f)
            val barH = (h * 0.48f * value).coerceAtLeast(1.5f)
            val x = (i + 0.5f) * step
            drawLine(colorMiddle.copy(alpha = if (isPlaying) 0.16f else 0.05f), Offset(x, mid - barH), Offset(x, mid + barH), strokeWidth = (centerWidth * 3.4f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
            drawLine(brush = barBrush, start = Offset(x, mid - barH), end = Offset(x, mid + barH), strokeWidth = centerWidth, cap = StrokeCap.Round, alpha = if (isPlaying) 0.95f else 0.3f)
            if (isPlaying && value > 0.32f) {
                drawCircle(color = Color.White.copy(alpha = 0.65f), radius = (centerWidth * 0.45f).coerceAtLeast(0.6f), center = Offset(x, mid - barH))
                drawCircle(color = colorStart.copy(alpha = 0.6f), radius = (centerWidth * 0.35f).coerceAtLeast(0.5f), center = Offset(x, mid + barH))
            }
        }
    }
}

@Composable
private fun WaveVisualizer(
    data: FloatArray,
    isPlaying: Boolean,
    colorStart: Color,
    colorMiddle: Color,
    colorEnd: Color,
    modifier: Modifier
) {
    val waveBrush = remember(colorStart, colorMiddle, colorEnd) {
        Brush.horizontalGradient(listOf(NeonMagenta, colorEnd, NeonBlue, colorStart))
    }
    val fillBrush = remember(colorMiddle, colorStart) {
        Brush.verticalGradient(listOf(colorMiddle.copy(alpha = 0.28f), colorStart.copy(alpha = 0.06f), Color.Transparent))
    }
    val path = remember { Path() }
    val fill = remember { Path() }
    val secondary = remember { Path() }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val mid = h * 0.5f
        val count = data.size.coerceIn(16, 64)
        val step = w / (count - 1).coerceAtLeast(1)
        path.reset()
        fill.reset()
        for (i in 0 until count) {
            val raw = if (isPlaying && i < data.size) data[i].coerceIn(0f, 1f) else 0.025f
            val envelope = (0.25f + 0.75f * sin(PI * i / (count - 1)).toFloat())
            val x = i * step
            val y = mid + (if (i % 2 == 0) -1f else 1f) * raw * envelope * h * 0.42f
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, mid); fill.lineTo(x, y) }
            else {
                val prevRaw = if (isPlaying && i - 1 < data.size) data[i - 1].coerceIn(0f, 1f) else 0.025f
                val prevEnv = 0.25f + 0.75f * sin(PI * (i - 1) / (count - 1)).toFloat()
                val px = (i - 1) * step
                val py = mid + (if ((i - 1) % 2 == 0) -1f else 1f) * prevRaw * prevEnv * h * 0.42f
                val cx = (px + x) * 0.5f
                path.cubicTo(cx, py, cx, y, x, y)
                fill.cubicTo(cx, py, cx, y, x, y)
            }
        }
        fill.lineTo(w, mid)
        fill.close()
        drawPath(fill, fillBrush)
        // Subtle offset trace produces depth with inexpensive vector strokes.
        if (isPlaying) {
            secondary.reset()
            for (i in 0 until count) {
                val raw = if (i < data.size) data[i].coerceIn(0f, 1f) else 0.025f
                val env = 0.25f + 0.75f * sin(PI * i / (count - 1)).toFloat()
                val x = i * step
                val y = mid + (if (i % 2 == 0) 1f else -1f) * raw * env * h * 0.22f
                if (i == 0) secondary.moveTo(x, y) else {
                    val px = (i - 1) * step
                    val pr = if (i - 1 < data.size) data[i - 1].coerceIn(0f, 1f) else 0.025f
                    val pe = 0.25f + 0.75f * sin(PI * (i - 1) / (count - 1)).toFloat()
                    val py = mid + (if ((i - 1) % 2 == 0) 1f else -1f) * pr * pe * h * 0.22f
                    secondary.cubicTo((px + x) * 0.5f, py, (px + x) * 0.5f, y, x, y)
                }
            }
            drawPath(secondary, waveBrush, style = Stroke(width = 0.8f, cap = StrokeCap.Round), alpha = 0.5f)
        }
        drawPath(path, waveBrush, style = Stroke(width = 2.1f, cap = StrokeCap.Round), alpha = if (isPlaying) 1f else 0.4f)
        drawLine(colorMiddle.copy(alpha = if (isPlaying) 0.5f else 0.16f), Offset(0f, mid), Offset(w, mid), strokeWidth = 0.7f)
    }
}

@Composable
private fun EqualizerVisualizer(
    data: FloatArray,
    isPlaying: Boolean,
    colorStart: Color,
    colorMiddle: Color,
    colorEnd: Color,
    modifier: Modifier
) {
    val blockBrush = remember(colorStart, colorMiddle, colorEnd) {
        Brush.horizontalGradient(listOf(NeonMagenta, NeonPurple, NeonBlue, colorStart))
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val count = data.size.coerceIn(20, 56)
        val gap = (w / count * 0.2f).coerceAtLeast(0.7f)
        val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(1f)
        val mid = h * 0.72f
        val maxHeight = h * 0.68f
        val blockGap = (h * 0.012f).coerceAtLeast(0.7f)
        val blockH = ((maxHeight - blockGap * 9f) / 10f).coerceAtLeast(1f)
        for (i in 0 until count) {
            val raw = if (isPlaying && i < data.size) data[i].coerceIn(0f, 1f) else 0.025f
            // A small floor prevents silent-looking gaps while keeping bass peaks reactive.
            val level = (0.12f + raw * 0.88f).coerceIn(0.12f, 1f)
            val blocks = (level * 10f).toInt().coerceIn(1, 10)
            val x = i * (barW + gap)
            for (b in 0 until 10) {
                val y = mid - (b + 1) * (blockH + blockGap)
                val lit = b < blocks
                val alpha = if (lit) (0.52f + b * 0.045f).coerceAtMost(1f) else 0.10f
                drawRoundRect(
                    brush = blockBrush,
                    topLeft = Offset(x, y),
                    size = Size(barW, blockH),
                    cornerRadius = CornerRadius((barW * 0.12f).coerceAtLeast(0.5f)),
                    alpha = alpha * if (isPlaying) 1f else 0.45f
                )
                if (lit && isPlaying && b == blocks - 1) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.75f),
                        topLeft = Offset(x, y),
                        size = Size(barW, (blockH * 0.3f).coerceAtLeast(0.7f)),
                        cornerRadius = CornerRadius(1f)
                    )
                }
                // Low-opacity mirrored reflection below the baseline.
                if (b < blocks && b < 5) {
                    val reflectY = mid + b * (blockH * 0.62f)
                    drawRoundRect(
                        brush = blockBrush,
                        topLeft = Offset(x, reflectY),
                        size = Size(barW, blockH * 0.48f),
                        cornerRadius = CornerRadius(1f),
                        alpha = (0.13f - b * 0.018f).coerceAtLeast(0.025f) * if (isPlaying) 1f else 0.35f
                    )
                }
            }
        }
    }
}
