package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    val coroutineScope = rememberCoroutineScope()
    val currentList = remember { mutableStateListOf<Track>().apply { addAll(tracks) } }

    var dragStartTrackId by remember { mutableStateOf<Long?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = contentPadding
    ) { from, to ->
        currentList.apply {
            add(to.index, removeAt(from.index))
        }
    }

    // Keep currentList in sync with external tracks when not dragging
    LaunchedEffect(tracks) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            val trackIds = tracks.map { it.id }
            val currentIds = currentList.map { it.id }
            if (currentIds != trackIds) {
                currentList.clear()
                currentList.addAll(tracks)
            }
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
            ReorderableItem(reorderableLazyListState, key = track.id) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "reorder_elevation")

                TrackListItem(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isFavorite = favoriteIds.contains(track.id),
                    onClick = { onPlayTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    showReorderHandle = true,
                    isDragging = isDragging,
                    reorderModifier = Modifier.draggableHandle(
                        interactionSource = interactionSource,
                        onDragStarted = {
                            dragStartTrackId = track.id
                            dragStartIndex = currentList.indexOfFirst { it.id == track.id }
                        },
                        onDragStopped = {
                            val startId = dragStartTrackId
                            val from = dragStartIndex
                            val to = if (startId != null) currentList.indexOfFirst { it.id == startId } else -1
                            if (from != null && from != -1 && to != -1 && from != to) {
                                onMoveTrack(from, to)
                            }
                            dragStartTrackId = null
                            dragStartIndex = null
                        }
                    ),
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
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    } else null,
                    onMoveToBottom = if (index < currentList.lastIndex) {
                        {
                            val from = index
                            val to = currentList.lastIndex
                            val item = currentList.removeAt(from)
                            currentList.add(to, item)
                            onMoveTrack(from, to)
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentList.lastIndex)
                            }
                        }
                    } else null,
                    modifier = Modifier.shadow(elevation, RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
