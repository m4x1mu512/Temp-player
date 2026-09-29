package com.example.ui.util

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

suspend fun LazyListState.smoothScrollToTrackIndex(targetIndex: Int) {
    if (targetIndex < 0) return
    val currentFirst = firstVisibleItemIndex
    val distance = abs(currentFirst - targetIndex)
    if (distance > 10) {
        val preIndex = if (targetIndex > currentFirst) targetIndex - 5 else targetIndex + 5
        scrollToItem(preIndex.coerceAtLeast(0))
    }
    animateScrollToItem(
        index = targetIndex,
        scrollOffset = -40
    )
}
