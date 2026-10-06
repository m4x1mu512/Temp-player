package com.example.ui.util

import androidx.compose.foundation.lazy.LazyListState

suspend fun LazyListState.smoothScrollToTrackIndex(targetIndex: Int) {
    if (targetIndex < 0) return
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount == 0 || targetIndex >= totalCount) return

    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    val viewportHeight = viewportEnd - viewportStart

    // If the item is already completely and comfortably visible on screen, no scrolling needed
    if (visibleItem != null) {
        val itemStart = visibleItem.offset
        val itemEnd = visibleItem.offset + visibleItem.size
        if (itemStart >= viewportStart + 16 && itemEnd <= viewportEnd - 16) {
            return
        }
    }

    try {
        val estimatedItemSize = visibleItem?.size ?: 180
        val centerOffset = -((viewportHeight - estimatedItemSize) / 2).coerceAtLeast(0)
        animateScrollToItem(
            index = targetIndex,
            scrollOffset = centerOffset
        )
    } catch (_: Exception) {
        // Animation cancelled or interrupted gracefully
    }
}
