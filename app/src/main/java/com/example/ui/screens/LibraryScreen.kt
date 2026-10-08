package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.example.ui.theme.FavoriteRed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Playlist
import com.example.data.model.SortOrder
import com.example.data.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ReorderableTrackList
import com.example.ui.components.ScrollToCurrentTrackFab
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackListItem
import com.example.ui.util.smoothScrollToTrackIndex
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val displayedTracks by viewModel.displayedTracks.collectAsStateWithLifecycle()
    val rawTracks by viewModel.rawTracks.collectAsStateWithLifecycle()
    val allTracksOrdered by viewModel.allTracksOrdered.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val folderGroups by viewModel.folderGroups.collectAsStateWithLifecycle()
    val artistGroups by viewModel.artistGroups.collectAsStateWithLifecycle()
    val albumGroups by viewModel.albumGroups.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
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
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // Intercept back gesture when a folder (or any category group) is open to return to the all-folders overview
    BackHandler(enabled = selectedGroupTitle != null || selectedPlaylist != null) {
        selectedGroupTitle = null
        selectedGroupTracks = null
        selectedPlaylist = null
    }

    val coroutineScope = rememberCoroutineScope()
    val queueListState = rememberLazyListState()

    // Pages: 0: Список воспроизведения (Queue), 1: Папки, 2: Плейлисты, 3: Альбомы, 4: Исполнители, 5: Поиск
    val pageCount = 6
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )

    // Reset selected group drilldown when user swipes between tabs
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != 1 && pagerState.settledPage != 2 && pagerState.settledPage != 3 && pagerState.settledPage != 4) {
            selectedGroupTitle = null
            selectedGroupTracks = null
            selectedPlaylist = null
        }
    }

    val scrollToTrackSmooth: (LazyListState, Int) -> Unit = { listState, targetIndex ->
        coroutineScope.launch {
            listState.smoothScrollToTrackIndex(targetIndex)
        }
    }

    val navigateToQueueAndScrollToCurrentTrack: () -> Unit = {
        coroutineScope.launch {
            selectedGroupTitle = null
            selectedGroupTracks = null
            selectedPlaylist = null
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(
                    page = 0,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            val q = if (currentQueue.isNotEmpty()) currentQueue else rawTracks
            val targetIdx = q.indexOfFirst { it.id == currentTrack?.id }
            if (targetIdx >= 0) {
                queueListState.smoothScrollToTrackIndex(targetIdx)
            }
        }
    }

    // Single source of truth for external navigation event to queue & playing track
    LaunchedEffect(Unit) {
        viewModel.navigateToQueueEvent.collect {
            navigateToQueueAndScrollToCurrentTrack()
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

    val navigateToTab: (Int) -> Unit = { targetPage ->
        if (pagerState.currentPage != targetPage) {
            selectedGroupTitle = null
            selectedGroupTracks = null
            selectedPlaylist = null
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
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
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Список воспроизведения (Queue)
                IconButton(
                    onClick = {
                        if (pagerState.currentPage != 0) {
                            navigateToTab(0)
                        }
                        selectedGroupTitle = null
                        selectedGroupTracks = null
                        selectedPlaylist = null
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
                    onClick = { navigateToTab(1) },
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
                    onClick = { navigateToTab(2) },
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
                    onClick = { navigateToTab(3) },
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
                    onClick = { navigateToTab(4) },
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
                    onClick = { navigateToTab(5) },
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
            if (currentTrack != null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .navigationBarsPadding()
                )
            }
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
                                val queueToDisplay = if (currentQueue.isNotEmpty()) currentQueue else allTracksOrdered
                                if (queueToDisplay.isEmpty()) {
                                    EmptyState(
                                        title = "Список воспроизведения пуст",
                                        message = "Выберите трек из папки, альбома, плейлиста или исполнителя",
                                        actionButtonText = null,
                                        onActionClick = null
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        ReorderableTrackList(
                                            tracks = queueToDisplay,
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            favoriteIds = favoriteIds,
                                            listState = queueListState,
                                            onPlayTrack = { track ->
                                                viewModel.playTrack(
                                                    track = track,
                                                    queue = queueToDisplay
                                                )
                                            },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onAddToPlaylist = { trackForPlaylistDialog = it },
                                            onMoveTrack = { from, to -> viewModel.moveQueueTrack(from, to) }
                                        )

                                        ScrollToCurrentTrackFab(
                                            currentTrack = currentTrack,
                                            isPlaying = isPlaying,
                                            onClick = {
                                                val targetIndex = queueToDisplay.indexOfFirst { it.id == currentTrack?.id }
                                                if (targetIndex >= 0) {
                                                    scrollToTrackSmooth(queueListState, targetIndex)
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 16.dp, bottom = 16.dp)
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // 2. "Папки" (с коллажем обложек и верхней карточкой)
                                FoldersSection(
                                    folderGroups = folderGroups,
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
                                    onAddToPlaylist = { trackForPlaylistDialog = it },
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            viewModel.playWithShuffle(tracks)
                                        }
                                    }
                                )
                            }

                            2 -> {
                                // 3. "Плейлисты"
                                PlaylistsSection(
                                    allTracks = allTracksOrdered,
                                    favorites = favoriteTracks,
                                    playlists = playlists,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedCategoryTitle = selectedGroupTitle,
                                    selectedPlaylist = selectedPlaylist,
                                    onSelectCategory = { title ->
                                        selectedGroupTitle = title
                                        selectedPlaylist = null
                                    },
                                    onSelectPlaylist = { pl ->
                                        selectedPlaylist = pl
                                        selectedGroupTitle = null
                                    },
                                    onBackFromGroup = {
                                        selectedGroupTitle = null
                                        selectedPlaylist = null
                                    },
                                    onCreatePlaylist = { showCreatePlaylistDialog = true },
                                    onRenamePlaylist = { playlistToRename = it },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onAddToPlaylist = { trackForPlaylistDialog = it },
                                    getPlaylistTracks = { viewModel.getPlaylistTracks(it) },
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onMoveAllTracksTrack = { from, to -> viewModel.moveAllTracksTrack(from, to) },
                                    onMoveFavoriteTrack = { from, to -> viewModel.moveFavoriteTrack(from, to) },
                                    onMovePlaylistTrack = { id, from, to -> viewModel.movePlaylistTrack(id, from, to) },
                                    onShuffleTracks = { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            viewModel.playWithShuffle(tracks)
                                        }
                                    }
                                )
                            }

                            3 -> {
                                // 4. "Альбомы" (с обложками альбомов)
                                AlbumsSection(
                                    albumGroups = albumGroups,
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
                                    onAddToPlaylist = { trackForPlaylistDialog = it },
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            viewModel.playWithShuffle(tracks)
                                        }
                                    }
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
                                    onAddToPlaylist = { trackForPlaylistDialog = it },
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            viewModel.playWithShuffle(tracks)
                                        }
                                    }
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
                                        val searchListState = rememberLazyListState()
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            LazyColumn(
                                                state = searchListState,
                                                contentPadding = PaddingValues(bottom = 80.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(
                                                    items = displayedTracks,
                                                    key = { it.id },
                                                    contentType = { "track" }
                                                ) { track ->
                                                    TrackListItem(
                                                        track = track,
                                                        isCurrent = currentTrack?.id == track.id,
                                                        isPlaying = isPlaying && currentTrack?.id == track.id,
                                                        isFavorite = favoriteIds.contains(track.id),
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

                                            ScrollToCurrentTrackFab(
                                                currentTrack = currentTrack,
                                                isPlaying = isPlaying,
                                                onClick = {
                                                    val targetIndex = displayedTracks.indexOfFirst { it.id == currentTrack?.id }
                                                    if (targetIndex >= 0) {
                                                        scrollToTrackSmooth(searchListState, targetIndex)
                                                    } else {
                                                        navigateToQueueAndScrollToCurrentTrack()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = 16.dp, bottom = 16.dp)
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
    onAddToPlaylist: (Track) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    if (selectedTitle != null) {
        val tracks = groups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()
        val distinctArtUris = remember(tracks) {
            tracks.mapNotNull { it.albumArtUri }.distinct().take(4)
        }
        val totalDurationMs = remember(tracks) { tracks.sumOf { it.duration } }
        val formattedTotalDuration = remember(totalDurationMs) {
            val totalSeconds = totalDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            if (hours > 0) {
                String.format("%d ч %02d мин", hours, minutes % 60)
            } else {
                String.format("%d мин %02d сек", minutes, seconds)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Исполнители",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hero Artist Card with Collage/Icon, Title, Subtitle, Track count, Total duration and Play Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    if (distinctArtUris.isNotEmpty()) {
                        FolderCoverCollage(
                            artUris = distinctArtUris,
                            size = 92.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Исполнитель",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Треков: ${tracks.size} • $formattedTotalDuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (tracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("play_artist_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Слушать", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { onShuffleTracks(tracks) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("shuffle_artist_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Перемешать",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = tracks,
                        key = { it.id },
                        contentType = { "track" }
                    ) { track ->
                        TrackListItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isFavorite = favoriteIds.contains(track.id),
                            onClick = { onPlayTrack(track, tracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        )
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                ScrollToCurrentTrackFab(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onClick = {
                        val targetIndex = tracks.indexOfFirst { it.id == currentTrack?.id }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                listState.smoothScrollToTrackIndex(targetIndex)
                            }
                        } else {
                            onNavigateToQueue()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    } else {
        val groupKeys = remember(groups) { groups.keys.toList() }
        val context = LocalContext.current
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groupKeys, key = { it }) { groupKey ->
                val groupTracks = groups[groupKey] ?: emptyList()
                val count = groupTracks.size
                val firstArtUri = remember(groupTracks) {
                    groupTracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectGroup(groupKey, groupTracks) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (firstArtUri != null) {
                                val artRequest = remember(firstArtUri) {
                                    ImageRequest.Builder(context)
                                        .data(firstArtUri)
                                        .size(150, 150)
                                        .crossfade(150)
                                        .error(R.drawable.ic_default_art)
                                        .build()
                                }
                                AsyncImage(
                                    model = artRequest,
                                    contentDescription = groupKey,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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
private fun FolderCoverCollage(
    artUris: List<android.net.Uri>,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    val distinctUris = remember(artUris) { artUris.distinct() }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            distinctUris.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[0]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[1]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface))
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[2]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[3]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            distinctUris.size == 3 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[0]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(distinctUris[1]).size(120, 120).crossfade(100).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface))
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(distinctUris[2]).size(240, 120).crossfade(100).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
            distinctUris.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(distinctUris[0]).size(150, 150).crossfade(100).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(distinctUris[1]).size(150, 150).crossfade(100).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            distinctUris.size == 1 -> {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(distinctUris[0]).size(240, 240).crossfade(100).error(R.drawable.ic_default_art).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        }
    }
}

@Composable
private fun FoldersSection(
    folderGroups: Map<String, List<Track>>,
    currentTrack: Track?,
    isPlaying: Boolean,
    selectedTitle: String?,
    onSelectGroup: (String, List<Track>) -> Unit,
    onBackFromGroup: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    if (selectedTitle != null) {
        val tracks = folderGroups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()
        val distinctArtUris = remember(tracks) {
            tracks.mapNotNull { it.albumArtUri }.distinct().take(4)
        }
        val totalDurationMs = remember(tracks) { tracks.sumOf { it.duration } }
        val formattedTotalDuration = remember(totalDurationMs) {
            val totalSeconds = totalDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            if (hours > 0) {
                String.format("%d ч %02d мин", hours, minutes % 60)
            } else {
                String.format("%d мин %02d сек", minutes, seconds)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Папки",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hero Folder Card with Collage of covers, Title, Track count, Total duration and Play Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    FolderCoverCollage(
                        artUris = distinctArtUris,
                        size = 92.dp,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Папка с музыкой",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Треков: ${tracks.size} • $formattedTotalDuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (tracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("play_folder_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Слушать", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { onShuffleTracks(tracks) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("shuffle_folder_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Перемешать",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = tracks,
                        key = { it.id },
                        contentType = { "track" }
                    ) { track ->
                        TrackListItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isFavorite = favoriteIds.contains(track.id),
                            onClick = { onPlayTrack(track, tracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        )
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                ScrollToCurrentTrackFab(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onClick = {
                        val targetIndex = tracks.indexOfFirst { it.id == currentTrack?.id }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                listState.smoothScrollToTrackIndex(targetIndex)
                            }
                        } else {
                            onNavigateToQueue()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    } else {
        if (folderGroups.isEmpty()) {
            EmptyState(
                title = "Нет папок",
                message = "В медиатеке не найдены папки с аудио",
                icon = Icons.Default.Folder,
                actionButtonText = null,
                onActionClick = null
            )
        } else {
            val folderKeys = remember(folderGroups) { folderGroups.keys.toList() }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderKeys, key = { it }) { folderName ->
                    val folderTracks = folderGroups[folderName] ?: emptyList()
                    val count = folderTracks.size
                    val distinctArtUris = remember(folderTracks) {
                        folderTracks.mapNotNull { it.albumArtUri }.distinct().take(4)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onSelectGroup(folderName, folderTracks) }
                            .testTag("folder_card_$folderName")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            FolderCoverCollage(
                                artUris = distinctArtUris,
                                size = 56.dp,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
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
}

@Composable
private fun AlbumsSection(
    albumGroups: Map<String, List<Track>>,
    currentTrack: Track?,
    isPlaying: Boolean,
    selectedTitle: String?,
    onSelectGroup: (String, List<Track>) -> Unit,
    onBackFromGroup: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    val context = LocalContext.current

    if (selectedTitle != null) {
        val tracks = albumGroups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()
        val albumArtUri = remember(tracks) {
            tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
        }
        val artistName = remember(tracks) {
            tracks.firstOrNull { it.artist.isNotBlank() && it.artist != "Неизвестный исполнитель" }?.artist ?: "Различные исполнители"
        }
        val totalDurationMs = remember(tracks) { tracks.sumOf { it.duration } }
        val formattedTotalDuration = remember(totalDurationMs) {
            val totalSeconds = totalDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            if (hours > 0) {
                String.format("%d ч %02d мин", hours, minutes % 60)
            } else {
                String.format("%d мин %02d сек", minutes, seconds)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Альбомы",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hero Album Card with Cover, Title, Artist and Play Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val coverRequest = remember(albumArtUri) {
                            ImageRequest.Builder(context)
                                .data(albumArtUri ?: R.drawable.ic_default_art)
                                .size(260, 260)
                                .crossfade(150)
                                .error(R.drawable.ic_default_art)
                                .build()
                        }
                        AsyncImage(
                            model = coverRequest,
                            contentDescription = "Обложка альбома $selectedTitle",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Треков: ${tracks.size} • $formattedTotalDuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (tracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("play_album_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Слушать", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { onShuffleTracks(tracks) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("shuffle_album_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Перемешать",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = tracks,
                        key = { it.id },
                        contentType = { "track" }
                    ) { track ->
                        TrackListItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isFavorite = favoriteIds.contains(track.id),
                            onClick = { onPlayTrack(track, tracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        )
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                ScrollToCurrentTrackFab(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onClick = {
                        val targetIndex = tracks.indexOfFirst { it.id == currentTrack?.id }
                        if (targetIndex >= 0) {
                            coroutineScope.launch {
                                listState.smoothScrollToTrackIndex(targetIndex)
                            }
                        } else {
                            onNavigateToQueue()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    } else {
        if (albumGroups.isEmpty()) {
            EmptyState(
                title = "Нет альбомов",
                message = "В медиатеке не найдены альбомы",
                icon = Icons.Default.Album,
                actionButtonText = null,
                onActionClick = null
            )
        } else {
            val albumKeys = remember(albumGroups) { albumGroups.keys.toList() }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("albums_grid")
            ) {
                items(albumKeys, key = { it }) { albumTitle ->
                    val tracks = albumGroups[albumTitle] ?: emptyList()
                    val albumArtUri = remember(tracks) {
                        tracks.firstOrNull { it.albumArtUri != null }?.albumArtUri
                    }
                    val artistName = remember(tracks) {
                        tracks.firstOrNull { it.artist.isNotBlank() && it.artist != "Неизвестный исполнитель" }?.artist ?: "Различные исполнители"
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGroup(albumTitle, tracks) }
                            .testTag("album_card_$albumTitle")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                val coverRequest = remember(albumArtUri) {
                                    ImageRequest.Builder(context)
                                        .data(albumArtUri ?: R.drawable.ic_default_art)
                                        .size(300, 300)
                                        .crossfade(150)
                                        .error(R.drawable.ic_default_art)
                                        .build()
                                }

                                AsyncImage(
                                    model = coverRequest,
                                    contentDescription = "Обложка $albumTitle",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (tracks.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .clickable { onPlayTrack(tracks.first(), tracks) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Слушать $albumTitle",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = albumTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Треков: ${tracks.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
    selectedCategoryTitle: String?,
    selectedPlaylist: Playlist?,
    onSelectCategory: (String) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onBackFromGroup: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    getPlaylistTracks: (Long) -> kotlinx.coroutines.flow.StateFlow<List<Track>>,
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onMoveAllTracksTrack: (Int, Int) -> Unit = { _, _ -> },
    onMoveFavoriteTrack: (Int, Int) -> Unit = { _, _ -> },
    onMovePlaylistTrack: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    val isDrilldown = selectedCategoryTitle != null || selectedPlaylist != null

    if (isDrilldown) {
        val currentPlaylist = remember(selectedPlaylist?.id, playlists) {
            if (selectedPlaylist != null) {
                playlists.find { it.id == selectedPlaylist.id } ?: selectedPlaylist
            } else null
        }
        val title = when {
            selectedCategoryTitle == "Все треки" -> "Все треки"
            selectedCategoryTitle == "Избранное" -> "Избранное"
            currentPlaylist != null -> currentPlaylist.name
            else -> ""
        }
        val subtitle = when {
            selectedCategoryTitle == "Все треки" -> "Вся медиатека"
            selectedCategoryTitle == "Избранное" -> "Любимые композиции"
            else -> "Пользовательский плейлист"
        }

        val customPlaylistTracks = if (selectedPlaylist != null) {
            val flow = remember(selectedPlaylist.id) { getPlaylistTracks(selectedPlaylist.id) }
            val tracksState by flow.collectAsStateWithLifecycle()
            tracksState
        } else {
            emptyList()
        }

        val tracks = when (selectedCategoryTitle) {
            "Все треки" -> allTracks
            "Избранное" -> favorites
            else -> customPlaylistTracks
        }
        val listState = rememberLazyListState()

        val distinctArtUris = remember(tracks) {
            tracks.mapNotNull { it.albumArtUri }.distinct().take(4)
        }
        val totalDurationMs = remember(tracks) { tracks.sumOf { it.duration } }
        val formattedTotalDuration = remember(totalDurationMs) {
            val totalSeconds = totalDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            if (hours > 0) {
                String.format("%d ч %02d мин", hours, minutes % 60)
            } else {
                String.format("%d мин %02d сек", minutes, seconds)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackFromGroup() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Плейлисты",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hero Playlist Card with Collage/Icon, Title, Subtitle, Track count, Total duration and Play Actions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    if (distinctArtUris.isNotEmpty()) {
                        FolderCoverCollage(
                            artUris = distinctArtUris,
                            size = 92.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selectedCategoryTitle == "Избранное") FavoriteRed.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedCategoryTitle) {
                                    "Избранное" -> Icons.Default.Favorite
                                    "Все треки" -> Icons.Default.MusicNote
                                    else -> Icons.Default.QueueMusic
                                },
                                contentDescription = null,
                                tint = if (selectedCategoryTitle == "Избранное") FavoriteRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedCategoryTitle == "Избранное") FavoriteRed else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Треков: ${tracks.size} • $formattedTotalDuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (tracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("play_playlist_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Слушать", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { onShuffleTracks(tracks) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("shuffle_playlist_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Перемешать",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tracks.isEmpty()) {
                    EmptyState(
                        title = if (selectedCategoryTitle == "Избранное") "Нет избранных треков" else "Плейлист пуст",
                        message = if (selectedCategoryTitle == "Избранное") {
                            "Нажмите на значок сердечка у любого трека, чтобы добавить его сюда"
                        } else {
                            "В этом плейлисте пока нет добавленных треков"
                        },
                        icon = if (selectedCategoryTitle == "Избранное") Icons.Default.Favorite else Icons.Default.QueueMusic,
                        actionButtonText = null,
                        onActionClick = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val onMoveItem: (Int, Int) -> Unit = { from, to ->
                        when {
                            selectedCategoryTitle == "Все треки" -> onMoveAllTracksTrack(from, to)
                            selectedCategoryTitle == "Избранное" -> onMoveFavoriteTrack(from, to)
                            selectedPlaylist != null -> onMovePlaylistTrack(selectedPlaylist.id, from, to)
                        }
                    }

                    ReorderableTrackList(
                        tracks = tracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        listState = listState,
                        onPlayTrack = { track -> onPlayTrack(track, tracks) },
                        onToggleFavorite = { onToggleFavorite(it) },
                        onAddToPlaylist = { onAddToPlaylist(it) },
                        onMoveTrack = onMoveItem,
                        modifier = Modifier.fillMaxSize()
                    )

                    val coroutineScope = rememberCoroutineScope()
                    ScrollToCurrentTrackFab(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onClick = {
                            val targetIndex = tracks.indexOfFirst { it.id == currentTrack?.id }
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    listState.smoothScrollToTrackIndex(targetIndex)
                                }
                            } else {
                                onNavigateToQueue()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    )
                }
            }
        }
    } else {
        val allTracksCollageUris = remember(allTracks) {
            allTracks.mapNotNull { it.albumArtUri }.distinct().take(4)
        }
        val favoritesCollageUris = remember(favorites) {
            favorites.mapNotNull { it.albumArtUri }.distinct().take(4)
        }

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
                        .clickable { onSelectCategory("Все треки") }
                        .testTag("all_tracks_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (allTracksCollageUris.isNotEmpty()) {
                            FolderCoverCollage(
                                artUris = allTracksCollageUris,
                                size = 44.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
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
                        .clickable { onSelectCategory("Избранное") }
                        .testTag("favorites_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (favoritesCollageUris.isNotEmpty()) {
                            FolderCoverCollage(
                                artUris = favoritesCollageUris,
                                size = 44.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
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
            items(playlists, key = { it.id }) { pl ->
                var menuExpanded by remember { mutableStateOf(false) }
                val plTracksFlow = remember(pl.id) { getPlaylistTracks(pl.id) }
                val plTracks by plTracksFlow.collectAsStateWithLifecycle()
                val plCollageUris = remember(plTracks) {
                    plTracks.mapNotNull { it.albumArtUri }.distinct().take(4)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onSelectPlaylist(pl) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (plCollageUris.isNotEmpty()) {
                            FolderCoverCollage(
                                artUris = plCollageUris,
                                size = 44.dp,
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
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
                                text = "Треков: ${plTracks.size}",
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
