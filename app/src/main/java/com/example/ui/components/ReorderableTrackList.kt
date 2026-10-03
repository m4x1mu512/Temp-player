package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Track

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
    val itemHeightPx = with(density) { 70.dp.toPx() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id },
            contentType = { _, _ -> "track" }
        ) { index, track ->
            val isCurrentDragging = draggingIndex == index

            val reorderModifier = Modifier.pointerInput(track.id, index, tracks.size) {
                detectDragGestures(
                    onDragStart = {
                        draggingIndex = index
                        dragAccumulatedY = 0f
                    },
                    onDragEnd = {
                        draggingIndex = null
                        dragAccumulatedY = 0f
                    },
                    onDragCancel = {
                        draggingIndex = null
                        dragAccumulatedY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedY += dragAmount.y
                        val threshold = itemHeightPx * 0.65f
                        val currentIdx = draggingIndex ?: index
                        if (dragAccumulatedY > threshold && currentIdx < tracks.lastIndex) {
                            onMoveTrack(currentIdx, currentIdx + 1)
                            draggingIndex = currentIdx + 1
                            dragAccumulatedY -= itemHeightPx
                        } else if (dragAccumulatedY < -threshold && currentIdx > 0) {
                            onMoveTrack(currentIdx, currentIdx - 1)
                            draggingIndex = currentIdx - 1
                            dragAccumulatedY += itemHeightPx
                        }
                    }
                )
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
                reorderModifier = reorderModifier,
                onMoveUp = if (index > 0) { { onMoveTrack(index, index - 1) } } else null,
                onMoveDown = if (index < tracks.lastIndex) { { onMoveTrack(index, index + 1) } } else null,
                onMoveToTop = if (index > 0) { { onMoveTrack(index, 0) } } else null,
                onMoveToBottom = if (index < tracks.lastIndex) { { onMoveTrack(index, tracks.lastIndex) } } else null,
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (isCurrentDragging) 10f else 1f)
                    .graphicsLayer {
                        if (isCurrentDragging) {
                            translationY = dragAccumulatedY
                            shadowElevation = 12f
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                    }
            )
        }
    }
}
