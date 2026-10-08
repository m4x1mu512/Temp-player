package com.example.ui.util

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/**
 * Smoothly scrolls to the target track item with a fluid glide,
 * matching the feel of high-end music players like Rhythm.
 *
 * Eliminates jerky short jumps and prevents negative scrollOffset glitches
 * that cause layout snapping and screen flickering.
 */
suspend fun LazyListState.smoothScrollToTrackIndex(targetIndex: Int) {
    if (targetIndex < 0) return
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount == 0 || targetIndex >= totalCount) return

    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset

    // If the item is already comfortably visible within the viewport, no scrolling needed
    if (visibleItem != null) {
        val itemStart = visibleItem.offset
        val itemEnd = visibleItem.offset + visibleItem.size
        if (itemStart >= viewportStart && itemEnd <= viewportEnd) {
            return
        }
    }

    try {
        val firstVisible = firstVisibleItemIndex
        val distance = targetIndex - firstVisible

        // If very far away (> 16 items), leap to 10 items away so Compose
        // has a generous glide runway to decelerate smoothly into the target item,
        // without dropping frames composing hundreds of offscreen items.
        if (abs(distance) > 16) {
            val preJumpIndex = if (distance > 0) {
                (targetIndex - 10).coerceAtLeast(0)
            } else {
                (targetIndex + 10).coerceAtMost(totalCount - 1)
            }
            scrollToItem(preJumpIndex)
        }

        // Animate directly and smoothly to targetIndex with offset 0.
        // Never pass negative offsets as Compose will clamp and cause a 1-frame re-layout blink.
        animateScrollToItem(
            index = targetIndex,
            scrollOffset = 0
        )
    } catch (_: Exception) {
        try {
            scrollToItem(targetIndex)
        } catch (_: Exception) {}
    }
}

