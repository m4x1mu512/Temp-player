package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import com.example.TrackMoveLogger
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
    onPlayTrackAtIndex: ((Track, Int) -> Unit)? = null,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    onEditTrackInfo: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    onRemoveFromPlaylist: ((Track) -> Unit)? = null,
    highlightedTrackId: Long? = null,
    currentTrackIndex: Int? = null,
    reorderEnabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp)
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val triggerVibration: () -> Unit = remember(haptic, vibrator) {
        {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35L)
                }
            } catch (_: Exception) {}
        }
    }

    val currentList = remember { mutableStateListOf<Track>().apply { addAll(tracks) } }
    // Stable per-row keys: unlike Track.id, these identify an occurrence in the list.
    val entryKeys = remember { mutableStateListOf<Long>().apply { tracks.indices.forEach { add(it.toLong()) } } }
    var nextEntryKey by remember { mutableLongStateOf(tracks.size.toLong()) }
    var dragStartEntryKey by remember { mutableStateOf<Long?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentTrackEntryKey by remember { mutableStateOf<Long?>(null) }
    var dragInProgress by remember { mutableStateOf(false) }
    // Keep the locally reordered rows until the parent publishes the same order.
    // Otherwise the old source snapshot can immediately overwrite the drag result.
    var pendingParentOrder by remember { mutableStateOf<List<Track>?>(null) }

    // Reorderable handles can outlive the composition in which their modifier was
    // created. Read queue/playback values and callbacks through updated state so a
    // later drag never operates on the previous queue snapshot.
    val latestCurrentTrack by rememberUpdatedState(currentTrack)
    val latestCurrentTrackIndex by rememberUpdatedState(currentTrackIndex)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestOnMoveTrack by rememberUpdatedState(onMoveTrack)

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = contentPadding
    ) { from, to ->
        TrackMoveLogger.log(
            context,
            "REORDER_CALLBACK_BEFORE",
            "fromIndex=${from.index} toIndex=${to.index} currentTrackId=${currentTrack?.id} currentTrackIndex=${currentTrackIndex} rows=" +
                currentList.indices.joinToString(";") { i -> "$i:id=${currentList[i].id},key=${entryKeys.getOrNull(i)}" }
        )
        currentList.apply {
            add(to.index, removeAt(from.index))
        }
        entryKeys.apply {
            add(to.index, removeAt(from.index))
        }
        TrackMoveLogger.log(
            context,
            "REORDER_CALLBACK_AFTER",
            "currentTrackId=${currentTrack?.id} currentTrackIndex=${currentTrackIndex} rows=" +
                currentList.indices.joinToString(";") { i -> "$i:id=${currentList[i].id},key=${entryKeys.getOrNull(i)}" }
        )
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }

    // Synchronize without allocating temporary ID lists. Also refresh Track objects
    // when their IDs/order stay the same but metadata (title, artist, art URI) changes.
    // The effect runs only when the source list changes, never on drag animation frames.
    LaunchedEffect(tracks, dragInProgress, pendingParentOrder) {
        if (!dragInProgress && !reorderableLazyListState.isAnyItemDragging) {
            val pending = pendingParentOrder
            if (pending != null && tracks != pending) {
                // The callback has been sent, but Compose has not delivered the updated
                // queue snapshot yet. Do not restore the stale source order in this gap.
                return@LaunchedEffect
            }

            if (pending != null) pendingParentOrder = null

            // Reconcile rows and occurrence keys together. Retain keys for matching
            // occurrences so duplicate Track IDs do not collapse into one row.
            val oldTracks = currentList.toList()
            val oldKeys = entryKeys.toList()
            val used = BooleanArray(oldTracks.size)
            val reconciledKeys = tracks.mapIndexed { index, track ->
                val match = oldTracks.indices.firstOrNull { oldIndex ->
                    !used[oldIndex] && oldTracks[oldIndex] == track
                }
                if (match != null) {
                    used[match] = true
                    oldKeys[match]
                } else {
                    nextEntryKey++
                }
            }

            currentList.clear()
            currentList.addAll(tracks)
            entryKeys.clear()
            entryKeys.addAll(reconciledKeys)
        }
    }

    // Some lists (for example playlists containing the same track more than once)
    // do not have a playback-queue index. In that case, highlight only the first matching
    // occurrence instead of marking every row with the same media ID as currently playing.
    // A queue can contain the same media ID more than once. Prefer the occurrence
    // nearest the queue's current index instead of always selecting the first matching ID.
    val firstMatchingCurrentTrackIndex = if (currentTrack != null) {
        val matchingIndices = currentList.indices.filter { currentList[it].id == currentTrack.id }
        when {
            matchingIndices.isEmpty() -> -1
            currentTrackIndex != null && currentTrackIndex in currentList.indices ->
                matchingIndices.minByOrNull { kotlin.math.abs(it - currentTrackIndex) } ?: -1
            else -> matchingIndices.first()
        }
    } else {
        -1
    }

    // Queue index can briefly lag behind the locally reordered list while the parent
    // updates playback state. Never use an index that points at a different track:
    // otherwise the row next to the playing track appears active after a reorder.
    val resolvedCurrentTrackIndex = currentTrackIndex
        ?.takeIf { index ->
            index in currentList.indices &&
                currentTrack != null &&
                currentList[index].id == currentTrack.id
        }
        ?: firstMatchingCurrentTrackIndex

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        // Each occurrence has its own key, which travels with it during reordering.
        itemsIndexed(
            items = currentList,
            key = { index, _ -> entryKeys[index] },
            contentType = { _, _ -> "track" }
        ) { index, track ->
            val itemKey = entryKeys[index]
            ReorderableItem(reorderableLazyListState, key = itemKey) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "reorder_elevation")

                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Press) {
                            triggerVibration()
                        }
                    }
                }

                val isCurrentRow = if (dragInProgress && dragCurrentTrackEntryKey != null) {
                    itemKey == dragCurrentTrackEntryKey
                } else {
                    index == resolvedCurrentTrackIndex
                }

                TrackListItem(
                    track = track,
                    isCurrent = isCurrentRow,
                    isPlaying = isPlaying && isCurrentRow,
                    isFavorite = favoriteIds.contains(track.id),
                    isHighlighted = highlightedTrackId != null && highlightedTrackId == track.id,
                    onClick = {
                        val indexedCallback = onPlayTrackAtIndex
                        if (indexedCallback != null) indexedCallback(track, index) else onPlayTrack(track)
                    },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onEditTrackInfo = { onEditTrackInfo?.invoke(track) },
                    onPlayNext = onPlayNext?.let { callback -> { callback(track) } },
                    onAddToQueue = onAddToQueue?.let { callback -> { callback(track) } },
                    onRemoveFromQueue = onRemoveFromQueue?.let { callback -> { callback(track) } },
                    onRemoveFromPlaylist = onRemoveFromPlaylist?.let { callback -> { callback(track) } },
                    showReorderHandle = reorderEnabled,
                    isDragging = isDragging,
                    reorderModifier = if (reorderEnabled) Modifier.draggableHandle(
                        interactionSource = interactionSource,
                        onDragStarted = {
                            dragInProgress = true
                            triggerVibration()
                            val liveDraggedIndex = entryKeys.indexOf(itemKey)
                            dragStartEntryKey = itemKey
                            dragStartIndex = liveDraggedIndex
                            // Pin the playing occurrence to its stable entry key for the
                            // whole drag. Index-based highlighting otherwise jumps between
                            // rows as the dragged item crosses the current track.
                            // Resolve the playing row from the live list at gesture time.
                            // The queue index can be stale after a local reorder, and a captured
                            // resolvedCurrentTrackIndex may refer to the row that moved into that slot.
                            val liveTrack = latestCurrentTrack
                            val liveQueueIndex = latestCurrentTrackIndex
                            val liveCurrentIndex = liveTrack?.let { playing ->
                                val matchingIndices = currentList.indices.filter { currentList[it].id == playing.id }
                                when {
                                    matchingIndices.isEmpty() -> -1
                                    liveQueueIndex != null && liveQueueIndex in currentList.indices ->
                                        matchingIndices.minByOrNull {
                                            kotlin.math.abs(it - liveQueueIndex)
                                        } ?: -1
                                    else -> matchingIndices.first()
                                }
                            } ?: -1
                            dragCurrentTrackEntryKey = entryKeys.getOrNull(liveCurrentIndex)
                            TrackMoveLogger.log(
                                context,
                                "DRAG_START",
                                "draggedIndex=$liveDraggedIndex draggedTrackId=${currentList.getOrNull(liveDraggedIndex)?.id} draggedEntryKey=$itemKey " +
                                    "currentTrackId=${liveTrack?.id} currentTrackIndex=${liveQueueIndex} resolvedCurrentIndex=$resolvedCurrentTrackIndex " +
                                    "liveCurrentIndex=$liveCurrentIndex pinnedCurrentKey=$dragCurrentTrackEntryKey isPlaying=$latestIsPlaying rows=" +
                                    currentList.indices.joinToString(";") { i -> "$i:id=${currentList[i].id},key=${entryKeys.getOrNull(i)}" }
                            )
                        },
                        onDragStopped = {
                            val startKey = dragStartEntryKey
                            val from = dragStartIndex ?: -1
                            val to = startKey?.let { key -> entryKeys.indexOf(key) } ?: -1
                            TrackMoveLogger.log(
                                context,
                                "DRAG_STOP",
                                "fromOriginalIndex=$from resolvedToIndex=$to draggedEntryKey=$startKey " +
                                    "draggedTrackId=${currentList.getOrNull(to)?.id} currentTrackId=${latestCurrentTrack?.id} " +
                                    "currentTrackIndex=${latestCurrentTrackIndex} resolvedCurrentIndex=$resolvedCurrentTrackIndex " +
                                    "pinnedCurrentKey=$dragCurrentTrackEntryKey isPlaying=$latestIsPlaying rows=" +
                                    currentList.indices.joinToString(";") { i -> "$i:id=${currentList[i].id},key=${entryKeys.getOrNull(i)}" }
                            )
                            if (from >= 0 && to >= 0 && from != to) {
                                // 'from' is the index in the unchanged parent snapshot at
                                // drag start; 'to' is the final index in the locally reordered
                                // list. Keep that exact result until the parent queue catches up.
                                pendingParentOrder = currentList.toList()
                                latestOnMoveTrack(from, to)
                            }
                            dragStartEntryKey = null
                            dragStartIndex = null
                            dragCurrentTrackEntryKey = null
                            // Reconcile any source-list updates deferred during the drag.
                            dragInProgress = false
                        }
                    ) else Modifier,
                    modifier = Modifier.shadow(elevation, RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
