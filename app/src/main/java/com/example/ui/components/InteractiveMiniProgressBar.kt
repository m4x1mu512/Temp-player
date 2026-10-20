package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Interactive scrubbable progress bar for the collapsed mini-player / dynamic island.
 * - Allows tapping or dragging to rewind/fast-forward the track directly from the mini-player.
 * - Displays a sleek glowing thumb indicator that smoothly tracks playback and user finger movement.
 * - Generous 24.dp touch hit area with real-time seeking callback and scrub state feedback.
 */
@Composable
fun InteractiveMiniProgressBar(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    accentColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    onScrubbingStateChanged: ((isScrubbing: Boolean, seekPositionMs: Long) -> Unit)? = null,
    testTag: String = "mini_player_interactive_progress"
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val rawTargetFraction = if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedFraction = remember { Animatable(rawTargetFraction) }

    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnScrubbingStateChanged by rememberUpdatedState(onScrubbingStateChanged)

    LaunchedEffect(rawTargetFraction, isPlaying, isScrubbing) {
        if (!isScrubbing) {
            if (abs(animatedFraction.value - rawTargetFraction) > 0.05f || !isPlaying) {
                animatedFraction.snapTo(rawTargetFraction)
            } else {
                animatedFraction.animateTo(
                    targetValue = rawTargetFraction,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
            }
        }
    }

    val displayFraction = if (isScrubbing) scrubFraction else animatedFraction.value

    val animatedTrackHeight: Dp by animateDpAsState(
        targetValue = if (isScrubbing) 5.dp else 3.5.dp,
        animationSpec = tween(120),
        label = "mini_track_height"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp) // Accessible touch target height along top edge
            .graphicsLayer { this.alpha = alpha }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (displayFraction * duration.toFloat()),
                    range = 0f..(if (duration > 0) duration.toFloat() else 1f)
                )
            }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0) {
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val targetMs = (fraction * duration).toLong()
                        currentOnSeek(targetMs)
                    }
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isScrubbing = true
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scrubFraction = fraction
                        val targetMs = (fraction * duration).toLong()
                        currentOnScrubbingStateChanged?.invoke(true, targetMs)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scrubFraction = fraction
                        val targetMs = (fraction * duration).toLong()
                        currentOnScrubbingStateChanged?.invoke(true, targetMs)
                    },
                    onDragEnd = {
                        isScrubbing = false
                        val targetMs = (scrubFraction * duration).toLong()
                        currentOnSeek(targetMs)
                        currentOnScrubbingStateChanged?.invoke(false, targetMs)
                    },
                    onDragCancel = {
                        isScrubbing = false
                        currentOnScrubbingStateChanged?.invoke(false, position)
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerY = size.height / 2f
            val width = size.width
            val currentFraction = displayFraction.coerceIn(0f, 1f)
            val thumbX = width * currentFraction
            val trackHeightPx = animatedTrackHeight.toPx()

            // 1. Inactive Track background
            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = trackHeightPx,
                cap = StrokeCap.Round
            )

            // 2. Active Track
            if (thumbX > 0f) {
                drawLine(
                    color = accentColor,
                    start = Offset(0f, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Radial Glow behind Thumb
            val glowRadiusPx = if (isScrubbing) 16.dp.toPx() else 10.dp.toPx()
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isScrubbing) 0.65f else 0.40f),
                        accentColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(thumbX, centerY),
                    radius = glowRadiusPx
                ),
                topLeft = Offset(thumbX - glowRadiusPx, centerY - glowRadiusPx),
                size = Size(glowRadiusPx * 2f, glowRadiusPx * 2f)
            )

            // 4. Thumb Knob
            val thumbRadiusPx = if (isScrubbing) 6.dp.toPx() else 3.5.dp.toPx()
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = thumbRadiusPx + 1.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = accentColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx * 0.45f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
