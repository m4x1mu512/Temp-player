package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Custom progress slider with:
 * 1. Thin sleek progress track (default 3.dp).
 * 2. Radial glow (drawRect with Brush.radialGradient) tracking the thumb in real-time.
 * 3. Smooth animation during playback (Animatable) and zero-lag user scrubbing.
 * 4. 48.dp accessible touch target with tap & drag gestures.
 */
@Composable
fun GlowProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    trackHeight: Dp = 3.dp,
    thumbRadius: Dp = 5.dp,
    glowRadius: Dp = 18.dp,
    glowAlpha: Float = 0.55f,
    testTag: String = "player_progress_slider"
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val rangeLength = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val currentTargetFraction = ((value - valueRange.start) / rangeLength).coerceIn(0f, 1f)

    // Smooth real-time animated position
    val animatedFraction = remember { Animatable(currentTargetFraction) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    LaunchedEffect(currentTargetFraction, isDragging) {
        if (!isDragging) {
            if (abs(animatedFraction.value - currentTargetFraction) > 0.05f) {
                animatedFraction.snapTo(currentTargetFraction)
            } else {
                animatedFraction.animateTo(
                    targetValue = currentTargetFraction,
                    animationSpec = tween(durationMillis = 160, easing = LinearEasing)
                )
            }
        }
    }

    val displayFraction = if (isDragging) dragProgressFraction else animatedFraction.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Accessibility guideline touch target
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value,
                    range = valueRange
                )
            }
            .pointerInput(rangeLength, valueRange.start) {
                detectTapGestures { offset ->
                    val thumbRadiusPx = thumbRadius.toPx()
                    val availableWidth = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
                    val touchX = (offset.x - thumbRadiusPx).coerceIn(0f, availableWidth)
                    val newFraction = touchX / availableWidth
                    val newValue = valueRange.start + newFraction * rangeLength
                    currentOnValueChange(newValue)
                    currentOnValueChangeFinished?.invoke()
                }
            }
            .pointerInput(rangeLength, valueRange.start) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val thumbRadiusPx = thumbRadius.toPx()
                        val availableWidth = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val touchX = (offset.x - thumbRadiusPx).coerceIn(0f, availableWidth)
                        dragProgressFraction = (touchX / availableWidth).coerceIn(0f, 1f)
                        val newValue = valueRange.start + dragProgressFraction * rangeLength
                        currentOnValueChange(newValue)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val thumbRadiusPx = thumbRadius.toPx()
                        val availableWidth = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val touchX = (change.position.x - thumbRadiusPx).coerceIn(0f, availableWidth)
                        dragProgressFraction = (touchX / availableWidth).coerceIn(0f, 1f)
                        val newValue = valueRange.start + dragProgressFraction * rangeLength
                        currentOnValueChange(newValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentOnValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        currentOnValueChangeFinished?.invoke()
                    }
                )
            }
            .testTag(testTag)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val thumbRadiusPx = thumbRadius.toPx()
            val glowRadiusPx = glowRadius.toPx()
            val trackHeightPx = trackHeight.toPx()

            val startX = thumbRadiusPx
            val endX = size.width - thumbRadiusPx
            val availableWidth = (endX - startX).coerceAtLeast(1f)

            val centerY = size.height / 2f
            val thumbX = startX + availableWidth * displayFraction.coerceIn(0f, 1f)

            // 1. Inactive Track (Thinner, with rounded caps)
            drawLine(
                color = inactiveTrackColor,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackHeightPx,
                cap = StrokeCap.Round
            )

            // 2. Active Track (Accent color, thinner, with rounded caps)
            if (thumbX > startX) {
                drawLine(
                    color = accentColor,
                    start = Offset(startX, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Technical Requirement: Soft Radial Glow behind the thumb
            // Uses drawRect with Brush.radialGradient centered at Offset(thumbX, centerY)
            // Colors transition from accentColor.copy(alpha = 0.55f) to Color.Transparent at radius ~18.dp
            val effectiveGlowRadius = if (isDragging) glowRadiusPx * 1.25f else glowRadiusPx
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        accentColor.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(thumbX, centerY),
                    radius = effectiveGlowRadius
                ),
                topLeft = Offset(thumbX - effectiveGlowRadius, centerY - effectiveGlowRadius),
                size = Size(effectiveGlowRadius * 2f, effectiveGlowRadius * 2f)
            )

            // 4. Thumb Knob (Sleek circle with subtle elevation & inner core)
            val currentThumbRadius = if (isDragging) thumbRadiusPx * 1.25f else thumbRadiusPx

            // Subtle drop shadow under thumb
            drawCircle(
                color = Color.Black.copy(alpha = 0.30f),
                radius = currentThumbRadius + 1.dp.toPx(),
                center = Offset(thumbX, centerY + 0.5.dp.toPx())
            )

            // Main thumb body
            drawCircle(
                color = accentColor,
                radius = currentThumbRadius,
                center = Offset(thumbX, centerY)
            )

            // Crisp inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = currentThumbRadius * 0.38f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
