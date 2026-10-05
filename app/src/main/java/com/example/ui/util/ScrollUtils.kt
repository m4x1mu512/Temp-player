package com.example.ui.util

import androidx.compose.foundation.lazy.LazyListState

suspend fun LazyListState.smoothScrollToTrackIndex(targetIndex: Int) {
    if (targetIndex < 0) return
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount == 0 || targetIndex >= totalCount) return

    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset

    // If the item is already completely visible on screen, no scrolling needed (avoids screen flicker or jerk)
    if (visibleItem != null) {
        val itemStart = visibleItem.offset
        val itemEnd = visibleItem.offset + visibleItem.size
        if (itemStart >= viewportStart && itemEnd <= viewportEnd) {
            return
        }
    }

    try {
        animateScrollToItem(
            index = targetIndex,
            scrollOffset = 0
        )
    } catch (_: Exception) {
        // Animation cancelled or interrupted gracefully
    }
}
