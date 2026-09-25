package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import com.example.ui.theme.FavoriteRed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Playlist
import com.example.data.model.SortOrder
import com.example.data.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackListItem
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedTracks by viewModel.displayedTracks.collectAsStateWithLifecycle()
    val rawTracks by viewModel.rawTracks.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val folderGroups by viewModel.folderGroups.collectAsStateWithLifecycle()
    val artistGroups by viewModel.artistGroups.collectAsStateWithLifecycle()
    val albumGroups by viewModel.albumGroups.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val isEqualizerEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemainingMillis by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()
    val sleepTimerMode by viewModel.sleepTimerMode.collectAsStateWithLifecycle()
    val miniPlayerBgMode by viewModel.miniPlayerBgMode.collectAsStateWithLifecycle()
    val miniPlayerCustomColor by viewModel.miniPlayerCustomColor.collectAsStateWithLifecycle()
    val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val openQueueEvent by viewModel.openQueueEvent.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Selected folder / artist / album filter drilldown
    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedGroupTracks by remember { mutableStateOf<List<Track>?>(null) }

    // Intercept back gesture when a folder (or any category group) is open to return to the all-folders overview
    BackHandler(enabled = selectedGroupTitle != null) {
        selectedGroupTitle = null
        selectedGroupTracks = null
    }

    val coroutineScope = rememberCoroutineScope()
    val queueListState = rememberLazyListState()
    var scrollToCurrentTrackTrigger by remember { mutableIntStateOf(0) }
    var lastHandledOpenQueue by rememberSaveable { mutableIntStateOf(0) }

    // Pages: 0: Список воспроизведения (Queue), 1: Папки, 2: Плейлисты, 3: Альбомы, 4: Исполнители, 5: Поиск
    val pageCount = 6
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )

    // Reset selected group drilldown when user swipes between tabs
    LaunchedEffect(pagerState.settledPage) {
        selectedGroupTitle = null
        selectedGroupTracks = null
    }

    // Explicit event to open playback queue (tab 0) and scroll to current track
    LaunchedEffect(openQueueEvent) {
        if (openQueueEvent > 0 && openQueueEvent != lastHandledOpenQueue) {
            lastHandledOpenQueue = openQueueEvent
            selectedGroupTitle = null
            selectedGroupTracks = null
            try {
                pagerState.scrollToPage(0)
            } catch (_: Exception) {}
            if (pagerState.currentPage != 0) {
                delay(60)
                try {
                    pagerState.scrollToPage(0)
                } catch (_: Exception) {}
            }
            scrollToCurrentTrackTrigger++
        }
    }

    // Auto-scroll to currently playing / paused track when opening playback queue
    LaunchedEffect(pagerState.currentPage, currentTrack?.id, scrollToCurrentTrackTrigger, currentQueue) {
        if (pagerState.currentPage == 0 && currentTrack != null) {
            val queueToDisplay = if (currentQueue.isNotEmpty()) currentQueue else rawTracks
            val targetIndex = queueToDisplay.indexOfFirst { it.id == currentTrack?.id }
            if (targetIndex >= 0) {
                try {
                    delay(80)
                    val currentFirst = queueListState.firstVisibleItemIndex
                    if (kotlin.math.abs(currentFirst - targetIndex) > 15) {
                        val preScroll = if (targetIndex > currentFirst) targetIndex - 8 else targetIndex + 8
                        queueListState.scrollToItem(preScroll.coerceIn(0, queueToDisplay.lastIndex))
                    }
                    queueListState.animateScrollToItem(targetIndex)
                } catch (_: Exception) {
                    queueListState.scrollToItem(targetIndex)
                }
            }
        }
    }

    LaunchedEffect(scanMessage) {
        scanMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearScanMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.testTag("library_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // 7 top bar icons: 6 tabs + 1 more menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Список воспроизведения (Queue)
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage != 0) {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                        scrollToCurrentTrackTrigger++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Список воспроизведения",
                        tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Папки (Folders)
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_folders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Папки",
                        tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Плейлисты (Playlists)
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_playlists_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Плейлисты",
                        tint = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4. Альбомы (Albums)
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_albums_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = "Альбомы",
                        tint = if (pagerState.currentPage == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 5. Исполнители (Artists)
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_artists_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Исполнители",
                        tint = if (pagerState.currentPage == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 6. Поиск (Search)
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(5) }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = if (pagerState.currentPage == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 7. Три точки (Menu)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { moreMenuExpanded = true },
                        modifier = Modifier.testTag("nav_more_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Эквалайзер") },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                showEqualizerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Таймер сна") },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                showSleepTimerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сканировать") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                viewModel.scanMusic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сортировка") },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                sortMenuExpanded = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                moreMenuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.displayName,
                                        fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                        color = if (order == sortOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.onSortOrderChanged(order)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val isCurrentTrackFavorite = remember(favoriteTracks, currentTrack?.id) {
                val currentId = currentTrack?.id
                if (currentId != null) {
                    favoriteTracks.any { it.id == currentId }
                } else false
            }

            MiniPlayer(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                isFavorite = isCurrentTrackFavorite,
                onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it.id) } },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNextTrack = { viewModel.nextTrack() },
                onPreviousTrack = { viewModel.previousTrack() },
                onClick = onNavigateToPlayer,
                bgMode = miniPlayerBgMode,
                customColor = miniPlayerCustomColor,
                autoRotate = autoRotate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Content with horizontal swipe between screens
            Box(modifier = Modifier.fillMaxSize()) {
                if (rawTracks.isEmpty() && !isScanning) {
                    EmptyState(
                        title = "Медиатека пуста",
                        message = "На устройстве не найдено аудиофайлов или требуется обновить список",
                        actionButtonText = "Сканировать аудио",
                        onActionClick = { viewModel.scanMusic() }
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                // 1. "Список воспроизведения" (Queue of tracks from current folder/album/artist/playlist)
                                val queueToDisplay = if (currentQueue.isNotEmpty()) currentQueue else rawTracks
                                if (queueToDisplay.isEmpty()) {
                                    EmptyState(
                                        title = "Список воспроизведения пуст",
                                        message = "Выберите трек из папки, альбома, плейлиста или исполнителя",
                                        actionButtonText = null,
                                        onActionClick = null
                                    )
                                } else {
                                    LazyColumn(
                                        state = queueListState,
                                        contentPadding = PaddingValues(bottom = 80.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(queueToDisplay, key = { it.id }) { track ->
                                            TrackListItem(
                                                track = track,
                                                isCurrent = currentTrack?.id == track.id,
                                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                                onClick = {
                                                    viewModel.playTrack(
                                                        track = track,
                                                        queue = queueToDisplay
                                                    )
                                                },
                                                onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                                onAddToPlaylist = { trackForPlaylistDialog = track }
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // 2. "Папки"
                                GroupedListSection(
                                    groups = folderGroups,
                                    icon = Icons.Default.Folder,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedGroupTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedGroupTitle = title
                                        selectedGroupTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedGroupTitle = null
                                        selectedGroupTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onAddToPlaylist = { trackForPlaylistDialog = it }
                                )
                            }

                            2 -> {
                                // 3. "Плейлисты"
                                PlaylistsSection(
                                    allTracks = rawTracks,
                                    favorites = favoriteTracks,
                                    playlists = playlists,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedGroupTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedGroupTitle = title
                                        selectedGroupTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedGroupTitle = null
                                        selectedGroupTracks = null
                                    },
                                    onCreatePlaylist = { showCreatePlaylistDialog = true },
                                    onRenamePlaylist = { playlistToRename = it },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onAddToPlaylist = { trackForPlaylistDialog = it },
                                    getPlaylistTracks = { viewModel.getPlaylistTracks(it) }
                                )
                            }

                            3 -> {
                                // 4. "Альбомы"
                                GroupedListSection(
                                    groups = albumGroups,
                                    icon = Icons.Default.Album,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedGroupTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedGroupTitle = title
                                        selectedGroupTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedGroupTitle = null
                                        selectedGroupTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onAddToPlaylist = { trackForPlaylistDialog = it }
                                )
                            }

                            4 -> {
                                // 5. "Исполнители"
                                GroupedListSection(
                                    groups = artistGroups,
                                    icon = Icons.Default.Person,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedGroupTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedGroupTitle = title
                                        selectedGroupTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedGroupTitle = null
                                        selectedGroupTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onAddToPlaylist = { trackForPlaylistDialog = it }
                                )
                            }

                            5 -> {
                                // 6. "Поиск"
                                Column(modifier = Modifier.fillMaxSize()) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        placeholder = { Text("Поиск по названию, артисту или альбому") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .testTag("search_input")
                                    )

                                    if (displayedTracks.isEmpty()) {
                                        EmptyState(
                                            title = if (searchQuery.isBlank()) "Введите поисковый запрос" else "Ничего не найдено",
                                            message = if (searchQuery.isBlank()) "Найдите треки, альбомы или исполнителей" else "По запросу «$searchQuery» треков не обнаружено",
                                            actionButtonText = null,
                                            onActionClick = null
                                        )
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(bottom = 80.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(displayedTracks, key = { it.id }) { track ->
                                                TrackListItem(
                                                    track = track,
                                                    isCurrent = currentTrack?.id == track.id,
                                                    isPlaying = isPlaying && currentTrack?.id == track.id,
                                                    onClick = {
                                                        viewModel.playTrack(
                                                            track = track,
                                                            queue = displayedTracks
                                                        )
                                                    },
                                                    onToggleFavorite = { viewModel.toggleFavorite(track.id) },
                                                    onAddToPlaylist = { trackForPlaylistDialog = track }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            title = "Новый плейлист",
            onConfirm = {
                viewModel.createPlaylist(it)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    playlistToRename?.let { pl ->
        CreatePlaylistDialog(
            initialName = pl.name,
            title = "Переименовать плейлист",
            onConfirm = {
                viewModel.renamePlaylist(pl.id, it)
                playlistToRename = null
            },
            onDismiss = { playlistToRename = null }
        )
    }

    trackForPlaylistDialog?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onPlaylistSelected = { pl ->
                viewModel.addTrackToPlaylist(pl.id, track.id)
                trackForPlaylistDialog = null
            },
            onCreateNewClicked = {
                trackForPlaylistDialog = null
                showCreatePlaylistDialog = true
            },
            onDismiss = { trackForPlaylistDialog = null }
        )
    }

    if (showEqualizerDialog) {
        EqualizerDialog(
            isEnabled = isEqualizerEnabled,
            bands = equalizerBands,
            currentPreset = equalizerPreset,
            onEnableChanged = { viewModel.setEqualizerEnabled(it) },
            onPresetSelected = { viewModel.setEqualizerPreset(it) },
            onBandLevelChanged = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMode = sleepTimerMode,
            remainingMillis = sleepTimerRemainingMillis,
            onSetTimer = {
                viewModel.setSleepTimer(it)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
private fun GroupedListSection(
    groups: Map<String, List<Track>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentTrack: Track?,
    isPlaying: Boolean,
    selectedTitle: String?,
    onSelectGroup: (String, List<Track>) -> Unit,
    onBackFromGroup: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    if (selectedTitle != null) {
        val tracks = groups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()

        LaunchedEffect(selectedTitle, tracks.size, currentTrack?.id) {
            if (currentTrack != null && tracks.isNotEmpty()) {
                val targetIndex = tracks.indexOfFirst { it.id == currentTrack.id }
                if (targetIndex >= 0) {
                    try {
                        delay(60)
                        listState.scrollToItem(targetIndex)
                    } catch (_: Exception) {}
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "← Назад",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$selectedTitle (${tracks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        onClick = { onPlayTrack(track, tracks) },
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups.keys.toList()) { groupKey ->
                val count = groups[groupKey]?.size ?: 0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectGroup(groupKey, groups[groupKey] ?: emptyList()) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = groupKey,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Треков: $count",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    allTracks: List<Track>,
    favorites: List<Track>,
    playlists: List<Playlist>,
    currentTrack: Track?,
    isPlaying: Boolean,
    selectedTitle: String?,
    onSelectGroup: (String, List<Track>) -> Unit,
    onBackFromGroup: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    getPlaylistTracks: (Long) -> kotlinx.coroutines.flow.StateFlow<List<Track>>
) {
    if (selectedTitle != null) {
        // Show selected playlist / favorites / all tracks
        val tracks = when (selectedTitle) {
            "Все треки" -> allTracks
            "Избранное" -> favorites
            else -> {
                val pl = playlists.find { it.name == selectedTitle }
                if (pl != null) {
                    val flow = getPlaylistTracks(pl.id)
                    val list by flow.collectAsStateWithLifecycle()
                    list
                } else emptyList()
            }
        }
        val listState = rememberLazyListState()

        LaunchedEffect(selectedTitle, tracks.size, currentTrack?.id) {
            if (currentTrack != null && tracks.isNotEmpty()) {
                val targetIndex = tracks.indexOfFirst { it.id == currentTrack.id }
                if (targetIndex >= 0) {
                    try {
                        delay(60)
                        listState.scrollToItem(targetIndex)
                    } catch (_: Exception) {}
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "← Назад",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$selectedTitle (${tracks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tracks.isEmpty()) {
                EmptyState(
                    title = "Плейлист пуст",
                    message = "В этом плейлисте пока нет добавленных треков",
                    actionButtonText = null,
                    onActionClick = null
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            onClick = { onPlayTrack(track, tracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1) All tracks card ("Все треки")
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectGroup("Все треки", allTracks) }
                        .testTag("all_tracks_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Все треки",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Треков: ${allTracks.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2) Favorites card ("Избранное")
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectGroup("Избранное", favorites) }
                        .testTag("favorites_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FavoriteRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Избранное",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Треков: ${favorites.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3) Section Header: "Ваши плейлисты"
            item {
                Text(
                    text = "Ваши плейлисты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // 4) Button: "Создать плейлист"
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onCreatePlaylist() }
                        .testTag("create_playlist_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Создать плейлист",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // User Playlists
            items(playlists) { pl ->
                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectGroup(pl.name, emptyList()) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Пользовательский плейлист",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню плейлиста")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Переименовать") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRenamePlaylist(pl)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePlaylist(pl)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
