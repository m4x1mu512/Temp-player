package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Track
import kotlin.math.roundToInt
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
    var draggingTrack by remember { mutableStateOf<Track?>(null) }
    var initialIndex by remember { mutableStateOf<Int?>(null) }
    var floatingCardY by remember { mutableFloatStateOf(0f) }
    var grabOffsetY by remember { mutableFloatStateOf(0f) }
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
    val measuredItemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key != draggingTrackId }?.size?.toFloat()
    val itemHeightPx = measuredItemHeight ?: with(density) { 70.dp.toPx() }
    val itemHeightDp = with(density) { itemHeightPx.toDp() }

    // Controlled, comfortable auto-scroll when finger reaches or passes edges
    LaunchedEffect(draggingTrackId, isDropping) {
        if (draggingTrackId == null || isDropping) return@LaunchedEffect
        val scrollThresholdPx = with(density) { 80.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 8.dp.toPx() } // Smooth, readable, controlled speed (~480dp/s)

        while (isActive && draggingTrackId != null && !isDropping) {
            val touchY = currentTouchYInList
            val lc = listCoordinates
            if (touchY != null && lc != null && lc.isAttached) {
                val listHeight = lc.size.height.toFloat()

                val scrollDelta = when {
                    touchY < scrollThresholdPx -> {
                        // Finger is near or above the top edge
                        val factor = if (touchY <= 0f) 1.0f else ((scrollThresholdPx - touchY) / scrollThresholdPx).coerceIn(0.15f, 1f)
                        -maxScrollSpeedPx * factor
                    }
                    touchY > listHeight - scrollThresholdPx -> {
                        // Finger is near or below the bottom edge
                        val distFromEdge = touchY - (listHeight - scrollThresholdPx)
                        val factor = if (touchY >= listHeight) 1.0f else (distFromEdge / scrollThresholdPx).coerceIn(0.15f, 1f)
                        maxScrollSpeedPx * factor
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    listState.scrollBy(scrollDelta)

                    // Re-check target slot as items scroll under the floating card
                    val currentIdx = currentList.indexOfFirst { it.id == draggingTrackId }
                    if (currentIdx != -1) {
                        val cardCenterY = floatingCardY + itemHeightPx / 2f
                        val hoveredItem = listState.layoutInfo.visibleItemsInfo.find { info ->
                            cardCenterY >= info.offset && cardCenterY <= (info.offset + info.size)
                        }
                        if (hoveredItem != null) {
                            val targetIdx = hoveredItem.index.coerceIn(0, currentList.lastIndex)
                            if (targetIdx != -1 && currentIdx != targetIdx) {
                                val item = currentList.removeAt(currentIdx)
                                currentList.add(targetIdx, item)
                            }
                        } else if (scrollDelta < 0 && touchY <= 0f && currentIdx > 0) {
                            val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                            if (currentIdx > firstVisible) {
                                val item = currentList.removeAt(currentIdx)
                                currentList.add(firstVisible, item)
                            }
                        } else if (scrollDelta > 0 && touchY >= listHeight && currentIdx < currentList.lastIndex) {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: currentList.lastIndex
                            if (currentIdx < lastVisible) {
                                val item = currentList.removeAt(currentIdx)
                                currentList.add(lastVisible, item)
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { listCoordinates = it }
        ) {
            itemsIndexed(
                items = currentList,
                key = { _, track -> track.id },
                contentType = { _, _ -> "track" }
            ) { index, track ->
                val isBeingDragged = draggingTrackId == track.id

                if (isBeingDragged) {
                    // Slot placeholder in list: stays empty while item floats in overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    )
                } else {
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
                                        draggingTrack = track
                                        initialIndex = idx

                                        val lc = listCoordinates
                                        val hc = handleCoordinates
                                        val touchY = if (lc != null && hc != null && hc.isAttached && lc.isAttached) {
                                            lc.localPositionOf(hc, offset).y
                                        } else {
                                            0f
                                        }
                                        currentTouchYInList = touchY

                                        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == track.id }
                                        val itemTop = itemInfo?.offset?.toFloat() ?: (touchY - itemHeightPx / 2f)
                                        grabOffsetY = touchY - itemTop
                                        floatingCardY = itemTop
                                    }
                                },
                                onDragEnd = {
                                    currentTouchYInList = null
                                    if (draggingTrackId == track.id) {
                                        isDropping = true
                                        coroutineScope.launch {
                                            try {
                                                val targetItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == track.id }
                                                val targetY = targetItemInfo?.offset?.toFloat() ?: floatingCardY
                                                Animatable(floatingCardY).animateTo(
                                                    targetValue = targetY,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                ) {
                                                    floatingCardY = value
                                                }
                                            } finally {
                                                val from = initialIndex
                                                val to = currentList.indexOfFirst { it.id == track.id }
                                                if (from != null && to != -1 && from != to) {
                                                    onMoveTrack(from, to)
                                                }
                                                draggingTrackId = null
                                                draggingTrack = null
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
                                                val targetItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == track.id }
                                                val targetY = targetItemInfo?.offset?.toFloat() ?: floatingCardY
                                                Animatable(floatingCardY).animateTo(
                                                    targetValue = targetY,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                ) {
                                                    floatingCardY = value
                                                }
                                            } finally {
                                                val from = initialIndex
                                                val to = currentList.indexOfFirst { it.id == track.id }
                                                if (from != null && to != -1 && from != to) {
                                                    onMoveTrack(from, to)
                                                }
                                                draggingTrackId = null
                                                draggingTrack = null
                                                initialIndex = null
                                                isDropping = false
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    if (draggingTrackId != track.id || isDropping) return@detectDragGestures
                                    change.consume()

                                    val lc = listCoordinates
                                    val hc = handleCoordinates
                                    if (lc != null && hc != null && hc.isAttached && lc.isAttached) {
                                        val touchY = lc.localPositionOf(hc, change.position).y
                                        currentTouchYInList = touchY
                                        val listHeight = lc.size.height.toFloat()
                                        // Keep floating card always 100% visible on screen, even if finger leaves screen
                                        floatingCardY = (touchY - grabOffsetY).coerceIn(0f, (listHeight - itemHeightPx).coerceAtLeast(0f))
                                    } else {
                                        floatingCardY += dragAmount.y
                                    }

                                    // Check hovered item in list to update target slot
                                    val currentIdx = currentList.indexOfFirst { it.id == track.id }
                                    if (currentIdx != -1) {
                                        val cardCenterY = floatingCardY + itemHeightPx / 2f
                                        val hoveredItem = listState.layoutInfo.visibleItemsInfo.find { info ->
                                            cardCenterY >= info.offset && cardCenterY <= (info.offset + info.size)
                                        }
                                        if (hoveredItem != null) {
                                            val targetIdx = hoveredItem.index.coerceIn(0, currentList.lastIndex)
                                            if (targetIdx != -1 && currentIdx != targetIdx) {
                                                val item = currentList.removeAt(currentIdx)
                                                currentList.add(targetIdx, item)
                                            }
                                        }
                                    }
                                }
                            )
                        }

                    val itemModifier = if (hasCompletedInitialLayout) {
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
                        isDragging = false,
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

        // Overlay Floating Card: stays 100% visible on screen at all times, never disposed or clipped
        if (draggingTrackId != null && draggingTrack != null) {
            val track = draggingTrack!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, floatingCardY.roundToInt()) }
                    .zIndex(100f)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                TrackListItem(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isFavorite = favoriteIds.contains(track.id),
                    onClick = {},
                    onToggleFavorite = {},
                    onAddToPlaylist = {},
                    showReorderHandle = true,
                    isDragging = true,
                    reorderModifier = Modifier
                )
            }
        }
    }
}
