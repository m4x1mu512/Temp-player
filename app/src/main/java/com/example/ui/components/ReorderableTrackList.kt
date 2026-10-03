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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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

    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var currentTouchYInList by remember { mutableStateOf<Float?>(null) }

    // Suppress animation during initial layout and restoration on startup
    var hasCompletedInitialLayout by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(400)
        hasCompletedInitialLayout = true
    }

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
    val itemHeightPx = measuredItemHeight ?: with(density) { 70.dp.toPx() }

    // Helper functions for atomic, boundary-safe item swapping
    fun tryMoveDown() {
        val currentId = draggingTrackId ?: return
        val idx = currentList.indexOfFirst { it.id == currentId }
        if (idx != -1 && idx < currentList.lastIndex) {
            val item = currentList.removeAt(idx)
            currentList.add(idx + 1, item)
            dragOffsetY -= itemHeightPx
        }
    }

    fun tryMoveUp() {
        val currentId = draggingTrackId ?: return
        val idx = currentList.indexOfFirst { it.id == currentId }
        if (idx > 0) {
            val item = currentList.removeAt(idx)
            currentList.add(idx - 1, item)
            dragOffsetY += itemHeightPx
        }
    }

    // Auto-scroll loop when finger is held near, at, or past the screen edges
    LaunchedEffect(draggingTrackId, isDropping) {
        if (draggingTrackId == null || isDropping) return@LaunchedEffect
        val scrollThresholdPx = with(density) { 80.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 6.dp.toPx() } // Smooth, calm, readable speed (~360dp/sec)

        while (isActive && draggingTrackId != null && !isDropping) {
            val touchY = currentTouchYInList
            val lc = listCoordinates
            if (touchY != null && lc != null && lc.isAttached) {
                val listHeight = lc.size.height.toFloat()

                val scrollDelta = when {
                    touchY < scrollThresholdPx -> {
                        val factor = if (touchY <= 0f) 1.2f else ((scrollThresholdPx - touchY) / scrollThresholdPx).coerceIn(0.15f, 1f)
                        -maxScrollSpeedPx * factor
                    }
                    touchY > listHeight - scrollThresholdPx -> {
                        val distFromEdge = touchY - (listHeight - scrollThresholdPx)
                        val factor = if (touchY >= listHeight) 1.2f else (distFromEdge / scrollThresholdPx).coerceIn(0.15f, 1f)
                        maxScrollSpeedPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    val consumed = listState.scrollBy(scrollDelta)
                    // Correct physics: as list scrolls under stationary finger, relative offset shifts by consumed
                    dragOffsetY += consumed

                    // Continuously swap item slots as other tracks scroll past
                    while (dragOffsetY > itemHeightPx * 0.5f) {
                        val idx = currentList.indexOfFirst { it.id == draggingTrackId }
                        if (idx != -1 && idx < currentList.lastIndex) {
                            tryMoveDown()
                        } else break
                    }
                    while (dragOffsetY < -itemHeightPx * 0.5f) {
                        val idx = currentList.indexOfFirst { it.id == draggingTrackId }
                        if (idx > 0) {
                            tryMoveUp()
                        } else break
                    }
                }
            }
            delay(16)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates = it }
    ) {
        itemsIndexed(
            items = currentList,
            key = { _, track -> track.id },
            contentType = { _, _ -> "track" }
        ) { index, track ->
            val isCurrentDragging = draggingTrackId == track.id

            var handleCoordinates by remember(track.id) { mutableStateOf<LayoutCoordinates?>(null) }

            val reorderModifier = Modifier
                .onGloballyPositioned { handleCoordinates = it }
                .pointerInput(track.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (isDropping) return@detectDragGestures
                            val idx = currentList.indexOfFirst { it.id == track.id }
                            if (idx != -1) {
                                draggingTrackId = track.id
                                initialIndex = idx
                                dragOffsetY = 0f

                                val lc = listCoordinates
                                val hc = handleCoordinates
                                if (lc != null && hc != null && hc.isAttached && lc.isAttached) {
                                    currentTouchYInList = lc.localPositionOf(hc, offset).y
                                }
                            }
                        },
                        onDragEnd = {
                            currentTouchYInList = null
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
                            currentTouchYInList = null
                            if (draggingTrackId == track.id) {
                                isDropping = true
                                coroutineScope.launch {
                                    try {
                                        Animatable(dragOffsetY).animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
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

                            val lc = listCoordinates
                            val hc = handleCoordinates
                            if (lc != null && hc != null && hc.isAttached && lc.isAttached) {
                                currentTouchYInList = lc.localPositionOf(hc, change.position).y
                            }

                            // Dynamic multi-position swap while finger is moving
                            while (dragOffsetY > itemHeightPx * 0.5f) {
                                val idx = currentList.indexOfFirst { it.id == track.id }
                                if (idx != -1 && idx < currentList.lastIndex) {
                                    tryMoveDown()
                                } else break
                            }
                            while (dragOffsetY < -itemHeightPx * 0.5f) {
                                val idx = currentList.indexOfFirst { it.id == track.id }
                                if (idx > 0) {
                                    tryMoveUp()
                                } else break
                            }
                        }
                    )
                }

            val itemModifier = if (isCurrentDragging) {
                Modifier
                    .zIndex(10f)
                    .graphicsLayer {
                        translationY = dragOffsetY
                        shadowElevation = if (isDropping) 4.dp.toPx() else 10.dp.toPx()
                        scaleX = if (isDropping) 1.0f else 1.02f
                        scaleY = if (isDropping) 1.0f else 1.02f
                    }
            } else if (hasCompletedInitialLayout) {
                Modifier
                    .zIndex(1f)
                    .animateItem()
            } else {
                Modifier.zIndex(1f)
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
