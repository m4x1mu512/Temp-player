package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ReorderableTrackList(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    listState: LazyListState,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp)
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentList = remember { mutableStateListOf<Track>().apply { addAll(tracks) } }

    var draggingTrackId by remember { mutableStateOf<Long?>(null) }
    var initialIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDropping by remember { mutableStateOf(false) }

    // Keep currentList in sync with external tracks when not dragging
    LaunchedEffect(tracks) {
        if (draggingTrackId == null && !isDropping) {
            if (currentList.map { it.id } != tracks.map { it.id }) {
                currentList.clear()
                currentList.addAll(tracks)
            }
        }
    }

    // Dynamic item height estimation from measured layout info or fallback
    val measuredItemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat()
    val itemHeightPx = measuredItemHeight ?: with(density) { 72.dp.toPx() }

    // Auto-scroll when dragged item reaches near the viewport top or bottom edge
    LaunchedEffect(draggingTrackId) {
        if (draggingTrackId == null) return@LaunchedEffect
        val scrollThresholdPx = with(density) { 56.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 14.dp.toPx() }

        while (isActive && draggingTrackId != null && !isDropping) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val draggedItem = visibleItems.find { it.key == draggingTrackId }

            if (draggedItem != null) {
                val viewportHeight = listState.layoutInfo.viewportSize.height
                val itemCenter = draggedItem.offset + draggedItem.size / 2f + dragOffsetY

                val scrollDelta = when {
                    itemCenter < scrollThresholdPx -> {
                        val ratio = ((scrollThresholdPx - itemCenter) / scrollThresholdPx).coerceIn(0f, 1f)
                        -maxScrollSpeedPx * ratio
                    }
                    itemCenter > viewportHeight - scrollThresholdPx -> {
                        val ratio = ((itemCenter - (viewportHeight - scrollThresholdPx)) / scrollThresholdPx).coerceIn(0f, 1f)
                        maxScrollSpeedPx * ratio
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    val consumed = listState.scrollBy(scrollDelta)
                    dragOffsetY -= consumed

                    val currentIdx = currentList.indexOfFirst { it.id == draggingTrackId }
                    if (currentIdx != -1) {
                        val h = draggedItem.size.toFloat().coerceAtLeast(1f)
                        while (dragOffsetY > h * 0.5f && currentIdx < currentList.lastIndex) {
                            val nextIdx = currentList.indexOfFirst { it.id == draggingTrackId }
                            if (nextIdx != -1 && nextIdx < currentList.lastIndex) {
                                val item = currentList.removeAt(nextIdx)
                                currentList.add(nextIdx + 1, item)
                                dragOffsetY -= h
                            } else break
                        }
                        while (dragOffsetY < -h * 0.5f && currentIdx > 0) {
                            val prevIdx = currentList.indexOfFirst { it.id == draggingTrackId }
                            if (prevIdx != -1 && prevIdx > 0) {
                                val item = currentList.removeAt(prevIdx)
                                currentList.add(prevIdx - 1, item)
                                dragOffsetY += h
                            } else break
                        }
                    }
                }
            }
            delay(16)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = currentList,
            key = { _, track -> track.id },
            contentType = { _, _ -> "track" }
        ) { index, track ->
            val isCurrentDragging = draggingTrackId == track.id

            // Stable gesture detector keyed on track.id alone to prevent gesture resets on reorder
            val reorderModifier = Modifier.pointerInput(track.id) {
                detectDragGestures(
                    onDragStart = {
                        if (isDropping) return@detectDragGestures
                        val idx = currentList.indexOfFirst { it.id == track.id }
                        if (idx != -1) {
                            draggingTrackId = track.id
                            initialIndex = idx
                            dragOffsetY = 0f
                        }
                    },
                    onDragEnd = {
                        if (draggingTrackId == track.id) {
                            isDropping = true
                            coroutineScope.launch {
                                try {
                                    Animatable(dragOffsetY).animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) {
                                        dragOffsetY = value
                                    }
                                } finally {
                                    dragOffsetY = 0f
                                    val from = initialIndex
                                    val to = currentList.indexOfFirst { it.id == track.id }
                                    if (from != null && to != -1 && from != to) {
                                        onMoveTrack(from, to)
                                    }
                                    draggingTrackId = null
                                    initialIndex = null
                                    isDropping = false
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        if (draggingTrackId == track.id) {
                            isDropping = true
                            coroutineScope.launch {
                                try {
                                    Animatable(dragOffsetY).animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) {
                                        dragOffsetY = value
                                    }
                                } finally {
                                    dragOffsetY = 0f
                                    val from = initialIndex
                                    val to = currentList.indexOfFirst { it.id == track.id }
                                    if (from != null && to != -1 && from != to) {
                                        onMoveTrack(from, to)
                                    }
                                    draggingTrackId = null
                                    initialIndex = null
                                    isDropping = false
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (draggingTrackId != track.id || isDropping) return@detectDragGestures
                        change.consume()
                        dragOffsetY += dragAmount.y

                        val currentIdx = currentList.indexOfFirst { it.id == track.id }
                        if (currentIdx != -1) {
                            val h = itemHeightPx.coerceAtLeast(1f)
                            while (dragOffsetY > h * 0.5f && currentIdx < currentList.lastIndex) {
                                val idx = currentList.indexOfFirst { it.id == track.id }
                                if (idx != -1 && idx < currentList.lastIndex) {
                                    val item = currentList.removeAt(idx)
                                    currentList.add(idx + 1, item)
                                    dragOffsetY -= h
                                } else break
                            }
                            while (dragOffsetY < -h * 0.5f && currentIdx > 0) {
                                val idx = currentList.indexOfFirst { it.id == track.id }
                                if (idx != -1 && idx > 0) {
                                    val item = currentList.removeAt(idx)
                                    currentList.add(idx - 1, item)
                                    dragOffsetY += h
                                } else break
                            }
                        }
                    }
                )
            }

            val itemModifier = if (isCurrentDragging) {
                Modifier
                    .zIndex(10f)
                    .graphicsLayer {
                        translationY = dragOffsetY
                        shadowElevation = if (isDropping) 6.dp.toPx() else 14.dp.toPx()
                        scaleX = if (isDropping) 1.0f else 1.03f
                        scaleY = if (isDropping) 1.0f else 1.03f
                    }
            } else {
                Modifier
                    .zIndex(1f)
                    .animateItem()
            }

            TrackListItem(
                track = track,
                isCurrent = currentTrack?.id == track.id,
                isPlaying = isPlaying && currentTrack?.id == track.id,
                isFavorite = favoriteIds.contains(track.id),
                onClick = { onPlayTrack(track) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                showReorderHandle = true,
                isDragging = isCurrentDragging,
                reorderModifier = reorderModifier,
                onMoveUp = if (index > 0) {
                    {
                        val from = index
                        val to = index - 1
                        val item = currentList.removeAt(from)
                        currentList.add(to, item)
                        onMoveTrack(from, to)
                    }
                } else null,
                onMoveDown = if (index < currentList.lastIndex) {
                    {
                        val from = index
                        val to = index + 1
                        val item = currentList.removeAt(from)
                        currentList.add(to, item)
                        onMoveTrack(from, to)
                    }
                } else null,
                onMoveToTop = if (index > 0) {
                    {
                        val from = index
                        val item = currentList.removeAt(from)
                        currentList.add(0, item)
                        onMoveTrack(from, 0)
                    }
                } else null,
                onMoveToBottom = if (index < currentList.lastIndex) {
                    {
                        val from = index
                        val to = currentList.lastIndex
                        val item = currentList.removeAt(from)
                        currentList.add(to, item)
                        onMoveTrack(from, to)
                    }
                } else null,
                modifier = itemModifier
            )
        }
    }
}
