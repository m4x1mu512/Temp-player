package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.VisualizerMode
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTurquoise
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizerView(
    fftData: FloatArray,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    neonColorStart: Color = NeonCyan,
    neonColorMiddle: Color = NeonTurquoise,
    neonColorEnd: Color = NeonPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Box(modifier = modifier.testTag("audio_visualizer_view")) {
        when (mode) {
            VisualizerMode.SPECTRUM -> {
                SpectrumVisualizer(
                    data = fftData,
                    isPlaying = isPlaying,
                    colorStart = neonColorStart,
                    colorMiddle = neonColorMiddle,
                    colorEnd = neonColorEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
            VisualizerMode.WAVE -> {
                WaveVisualizer(
                    data = fftData,
                    isPlaying = isPlaying,
                    colorStart = neonColorStart,
                    colorMiddle = neonColorMiddle,
                    colorEnd = neonColorEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
            VisualizerMode.CIRCLE -> {
                CircleVisualizer(
                    data = fftData,
                    isPlaying = isPlaying,
                    pulse = pulseGlow,
                    colorStart = neonColorStart,
                    colorMiddle = neonColorMiddle,
                    colorEnd = neonColorEnd,
                    modifier = Modifier.fillMaxSize()
                )
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
    modifier: Modifier = Modifier
) {
    val barCount = data.size.coerceAtLeast(16)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.25f
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2f)
        val spacing = totalSpacing / (barCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            colors = listOf(colorEnd, colorMiddle, colorStart),
            startY = 0f,
            endY = height
        )

        for (i in 0 until barCount) {
            val rawVal = if (i < data.size) data[i] else 0.05f
            val value = if (isPlaying) rawVal.coerceIn(0.04f, 1.0f) else 0.03f
            val barHeight = (height * value).coerceAtLeast(4f)
            val left = i * (barWidth + spacing)
            val top = height - barHeight

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )

            // Neon tip highlight
            if (isPlaying && value > 0.2f) {
                drawCircle(
                    color = colorStart,
                    radius = (barWidth / 2) * 1.1f,
                    center = Offset(left + barWidth / 2, top + barWidth / 2)
                )
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
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val points = data.size.coerceAtLeast(16)
        val stepX = width / (points - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()
        path.moveTo(0f, midY)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, midY)

        for (i in 0 until points) {
            val v = if (i < data.size) data[i] else 0.05f
            val amp = if (isPlaying) v * (height * 0.42f) else 4f
            val sign = if (i % 2 == 0) 1f else -1f
            val x = i * stepX
            val y = midY + (amp * sign)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevV = if (i - 1 < data.size) data[i - 1] else 0.05f
                val prevAmp = if (isPlaying) prevV * (height * 0.42f) else 4f
                val prevSign = if ((i - 1) % 2 == 0) 1f else -1f
                val prevY = midY + (prevAmp * prevSign)

                val cx = (prevX + x) / 2f
                path.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw soft ambient gradient below wave
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(colorMiddle.copy(alpha = 0.25f), Color.Transparent),
                startY = midY,
                endY = height
            )
        )

        // Draw glowing neon line
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(colorStart, colorMiddle, colorEnd)),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CircleVisualizer(
    data: FloatArray,
    isPlaying: Boolean,
    pulse: Float,
    colorStart: Color,
    colorMiddle: Color,
    colorEnd: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val minDim = minOf(size.width, size.height)
        val baseRadius = (minDim * 0.22f) * (if (isPlaying) pulse else 1.0f)
        val maxBarLen = minDim * 0.22f

        val bands = data.size.coerceAtLeast(24)
        val angleStep = (2 * Math.PI) / bands

        // Center ambient orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorStart.copy(alpha = 0.35f), colorMiddle.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = baseRadius * 1.3f
            ),
            radius = baseRadius * 1.3f,
            center = Offset(centerX, centerY)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorMiddle, colorEnd),
                center = Offset(centerX, centerY),
                radius = baseRadius
            ),
            radius = baseRadius * 0.75f,
            center = Offset(centerX, centerY)
        )

        // Radial bars
        for (i in 0 until bands) {
            val angle = i * angleStep
            val raw = if (i < data.size) data[i] else 0.05f
            val v = if (isPlaying) raw.coerceIn(0.05f, 1.0f) else 0.04f
            val barLen = (maxBarLen * v).coerceAtLeast(6f)

            val startX = centerX + (baseRadius * cos(angle)).toFloat()
            val startY = centerY + (baseRadius * sin(angle)).toFloat()
            val endX = centerX + ((baseRadius + barLen) * cos(angle)).toFloat()
            val endY = centerY + ((baseRadius + barLen) * sin(angle)).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(colorStart, colorEnd),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }
}
