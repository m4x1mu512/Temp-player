package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.example.ui.util.PlayerEmphasizedDecelerateEasing
import com.example.ui.util.PlayerEmphasizedAccelerateEasing
import com.example.ui.theme.FavoriteRed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.EditTrackInfoDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ReorderableTrackList
import com.example.ui.components.ScrollToCurrentTrackFab
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackListItem
import com.example.ui.util.smoothScrollToTrackIndex
import com.example.ui.util.centerTrackIndexInstantly
import com.example.ui.util.firstDistinctArtUris
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
    val musicFolders by viewModel.musicFolders.collectAsStateWithLifecycle()
    val musicFolderSetupCompleted by viewModel.musicFolderSetupCompleted.collectAsStateWithLifecycle()
    val allTracksOrdered by viewModel.allTracksOrdered.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val openQueueEvent by viewModel.openQueueEvent.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val folderGroups by viewModel.folderGroups.collectAsStateWithLifecycle()
    val artistGroups by viewModel.artistGroups.collectAsStateWithLifecycle()
    val albumGroups by viewModel.albumGroups.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    // Equalizer and sleep-timer state is read inside the dialogs below: collecting it here at
    // the root recomposed the whole screen (pager, every list) on each equalizer change and
    // twice per second while a sleep timer was running.

    val miniPlayerBgMode by viewModel.miniPlayerBgMode.collectAsStateWithLifecycle()
    val miniPlayerCustomColor by viewModel.miniPlayerCustomColor.collectAsStateWithLifecycle()
    val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val firstRunFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            viewModel.addMusicFolder(uri.toString())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var trackForEditInfoDialog by remember { mutableStateOf<Track?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Shared by every section. Created inline before, these lambdas were new objects on each
    // composition, which made the section parameters unstable and defeated skipping: a
    // recomposition of this screen re-ran all four section bodies for nothing.
    val onToggleFavoriteCallback: (Long) -> Unit = remember(viewModel) {
        { trackId -> viewModel.toggleFavorite(trackId) }
    }
    val onAddToPlaylistCallback: (Track) -> Unit = remember {
        { track -> trackForPlaylistDialog = track }
    }
    val onEditTrackInfoCallback: (Track) -> Unit = remember {
        { track -> trackForEditInfoDialog = track }
    }
    val onShuffleTracksCallback: (List<Track>) -> Unit = remember(viewModel) {
        { tracks -> if (tracks.isNotEmpty()) viewModel.playWithShuffle(tracks) }
    }

    // Dedicated filter drilldown states per library section
    var selectedFolderTitle by remember { mutableStateOf<String?>(null) }
    var selectedFolderTracks by remember { mutableStateOf<List<Track>?>(null) }

    var selectedPlaylistCategoryTitle by remember { mutableStateOf<String?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
    var selectedAlbumTracks by remember { mutableStateOf<List<Track>?>(null) }

    var selectedArtistTitle by remember { mutableStateOf<String?>(null) }
    var selectedArtistTracks by remember { mutableStateOf<List<Track>?>(null) }

    val resetAllDrilldowns = {
        selectedFolderTitle = null
        selectedFolderTracks = null
        selectedPlaylistCategoryTitle = null
        selectedPlaylist = null
        selectedAlbumTitle = null
        selectedAlbumTracks = null
        selectedArtistTitle = null
        selectedArtistTracks = null
    }

    if (!musicFolderSetupCompleted) {
        AlertDialog(
            onDismissRequest = { viewModel.completeMusicFolderSetup() },
            title = { Text("Где искать музыку?") },
            text = {
                Column {
                    Text("Выбери папки на устройстве, в которых приложение будет искать музыкальные файлы. Сканирование запускается только вручную.")
                    Spacer(modifier = Modifier.height(10.dp))
                    if (musicFolders.isNotEmpty()) {
                        Text(
                            text = "Выбрано папок: ${musicFolders.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.completeMusicFolderSetup() }) {
                    Text("Готово")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { firstRunFolderPicker.launch(null) }) {
                        Text("Добавить папку")
                    }
                    TextButton(onClick = { viewModel.completeMusicFolderSetup() }) {
                        Text("Позже")
                    }
                }
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val queueListState = rememberLazyListState()

    // Pages: 0: Список воспроизведения (Queue), 1: Папки, 2: Плейлисты, 3: Альбомы, 4: Исполнители, 5: Поиск
    val pageCount = 6
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )

    val hasOpenDrilldown = selectedFolderTitle != null ||
            selectedPlaylistCategoryTitle != null ||
            selectedPlaylist != null ||
            selectedAlbumTitle != null ||
            selectedArtistTitle != null

    // Intercept back gesture when inside a folder, playlist, album, or artist to return to that section's initial overview
    BackHandler(enabled = hasOpenDrilldown) {
        when (pagerState.currentPage) {
            1 -> {
                selectedFolderTitle = null
                selectedFolderTracks = null
            }
            2 -> {
                selectedPlaylistCategoryTitle = null
                selectedPlaylist = null
            }
            3 -> {
                selectedAlbumTitle = null
                selectedAlbumTracks = null
            }
            4 -> {
                selectedArtistTitle = null
                selectedArtistTracks = null
            }
            else -> resetAllDrilldowns()
        }
    }

    // Reset drilldowns whenever user changes tabs / swipes between screens so initial screen opens
    LaunchedEffect(pagerState.currentPage) {
        resetAllDrilldowns()
    }
    LaunchedEffect(pagerState.settledPage) {
        resetAllDrilldowns()
    }

    val scrollToTrackSmooth: (LazyListState, Int) -> Unit = { listState, targetIndex ->
        coroutineScope.launch {
            listState.smoothScrollToTrackIndex(targetIndex)
        }
    }

    val navigateToQueueAndScrollToCurrentTrack: () -> Unit = {
        coroutineScope.launch {
            resetAllDrilldowns()
            if (pagerState.currentPage != 0) {
                pagerState.scrollToPage(0)
            }
            val q = if (currentQueue.isNotEmpty()) currentQueue else allTracksOrdered
            val targetIdx = q.indexOfFirst { it.id == currentTrack?.id }
            if (targetIdx >= 0) {
                queueListState.centerTrackIndexInstantly(targetIdx)
            }
        }
    }

    // StateFlow survives navigation away from LibraryScreen, so a queue-open request
    // triggered while Settings is on top is handled when LibraryScreen is composed again.
    LaunchedEffect(openQueueEvent) {
        if (openQueueEvent > 0) {
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
        resetAllDrilldowns()
        if (pagerState.currentPage != targetPage) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = tween(
                        durationMillis = 240,
                        easing = PlayerEmphasizedDecelerateEasing
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
            val glassShape = RoundedCornerShape(
                bottomStart = 22.dp,
                bottomEnd = 22.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(glassShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                            )
                        ),
                        shape = glassShape
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                        ),
                        shape = glassShape
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Список воспроизведения (Queue)
                IconButton(
                    onClick = { navigateToTab(0) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_queue_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 0) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Список воспроизведения",
                            tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Папки (Folders)
                IconButton(
                    onClick = { navigateToTab(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_folders_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 1) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Папки",
                            tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 3. Плейлисты (Playlists)
                IconButton(
                    onClick = { navigateToTab(2) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_playlists_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 2) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Плейлисты",
                            tint = if (pagerState.currentPage == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 4. Альбомы (Albums)
                IconButton(
                    onClick = { navigateToTab(3) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_albums_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 3) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = "Альбомы",
                            tint = if (pagerState.currentPage == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 5. Исполнители (Artists)
                IconButton(
                    onClick = { navigateToTab(4) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_artists_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 4) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Исполнители",
                            tint = if (pagerState.currentPage == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 6. Поиск (Search)
                IconButton(
                    onClick = { navigateToTab(5) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_search_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (pagerState.currentPage == 5) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = if (pagerState.currentPage == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
                        onDismissRequest = { moreMenuExpanded = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Эквалайзер") },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                moreMenuExpanded = false
                                showEqualizerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Таймер сна") },
                            leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                moreMenuExpanded = false
                                showSleepTimerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сканировать") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                moreMenuExpanded = false
                                viewModel.scanMusic()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сортировка") },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                moreMenuExpanded = false
                                sortMenuExpanded = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                if (allTracksOrdered.isEmpty() && !isScanning) {
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
                                        icon = Icons.Default.QueueMusic,
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
                                            onToggleFavorite = onToggleFavoriteCallback,
                                            onAddToPlaylist = onAddToPlaylistCallback,
                                            onEditTrackInfo = onEditTrackInfoCallback,
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
                                                .padding(end = 16.dp, bottom = 124.dp)
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
                                    selectedTitle = selectedFolderTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedFolderTitle = title
                                        selectedFolderTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedFolderTitle = null
                                        selectedFolderTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = onToggleFavoriteCallback,
                                    onAddToPlaylist = onAddToPlaylistCallback,
                                    onEditTrackInfo = onEditTrackInfoCallback,
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = onShuffleTracksCallback
                                )
                            }

                            2 -> {
                                // 3. "Плейлисты"
                                // Collected here rather than at the root: the favourites list is
                                // only rendered by this tab, so keeping it hot at the root made
                                // every heart tap re-filter and re-sort it while the user was
                                // on another tab (the pager only composes the visible page).
                                val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
                                PlaylistsSection(
                                    allTracks = allTracksOrdered,
                                    favorites = favoriteTracks,
                                    playlists = playlists,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedCategoryTitle = selectedPlaylistCategoryTitle,
                                    selectedPlaylist = selectedPlaylist,
                                    onSelectCategory = { title ->
                                        selectedPlaylistCategoryTitle = title
                                        selectedPlaylist = null
                                    },
                                    onSelectPlaylist = { pl ->
                                        selectedPlaylist = pl
                                        selectedPlaylistCategoryTitle = null
                                    },
                                    onBackFromGroup = {
                                        selectedPlaylistCategoryTitle = null
                                        selectedPlaylist = null
                                    },
                                    onCreatePlaylist = { showCreatePlaylistDialog = true },
                                    onRenamePlaylist = { playlistToRename = it },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = onToggleFavoriteCallback,
                                    onAddToPlaylist = onAddToPlaylistCallback,
                                    onEditTrackInfo = onEditTrackInfoCallback,
                                    getPlaylistTracks = { viewModel.getPlaylistTracks(it) },
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onMoveAllTracksTrack = { from, to -> viewModel.moveAllTracksTrack(from, to) },
                                    onMoveFavoriteTrack = { from, to -> viewModel.moveFavoriteTrack(from, to) },
                                    onMovePlaylistTrack = { id, from, to -> viewModel.movePlaylistTrack(id, from, to) },
                                    onShuffleTracks = onShuffleTracksCallback
                                )
                            }

                            3 -> {
                                // 4. "Альбомы" (с обложками альбомов)
                                AlbumsSection(
                                    albumGroups = albumGroups,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedAlbumTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedAlbumTitle = title
                                        selectedAlbumTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedAlbumTitle = null
                                        selectedAlbumTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = onToggleFavoriteCallback,
                                    onAddToPlaylist = onAddToPlaylistCallback,
                                    onEditTrackInfo = onEditTrackInfoCallback,
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = onShuffleTracksCallback
                                )
                            }

                            4 -> {
                                // 5. "Исполнители"
                                GroupedListSection(
                                    groups = artistGroups,
                                    icon = Icons.Default.Person,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    selectedTitle = selectedArtistTitle,
                                    onSelectGroup = { title, tracks ->
                                        selectedArtistTitle = title
                                        selectedArtistTracks = tracks
                                    },
                                    onBackFromGroup = {
                                        selectedArtistTitle = null
                                        selectedArtistTracks = null
                                    },
                                    onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                    onToggleFavorite = onToggleFavoriteCallback,
                                    onAddToPlaylist = onAddToPlaylistCallback,
                                    onEditTrackInfo = onEditTrackInfoCallback,
                                    favoriteIds = favoriteIds,
                                    onNavigateToQueue = navigateToQueueAndScrollToCurrentTrack,
                                    onShuffleTracks = onShuffleTracksCallback
                                )
                            }

                            5 -> {
                                // 6. "Поиск"
                                Column(modifier = Modifier.fillMaxSize()) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        placeholder = {
                                            Text(
                                                "Поиск по названию, артисту или альбому",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = if (searchQuery.isNotBlank()) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { viewModel.onSearchQueryChanged("") }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = "Очистить поиск"
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
                                            cursorColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .testTag("search_input")
                                    )

                                    if (searchQuery.isNotBlank() && displayedTracks.isNotEmpty()) {
                                        Text(
                                            text = "Найдено треков: ${displayedTracks.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(
                                                start = 22.dp,
                                                end = 22.dp,
                                                bottom = 4.dp
                                            )
                                        )
                                    }

                                    if (displayedTracks.isEmpty()) {
                                        EmptyState(
                                            title = if (searchQuery.isBlank()) "Введите поисковый запрос" else "Ничего не найдено",
                                            message = if (searchQuery.isBlank()) "Найдите треки, альбомы или исполнителей" else "По запросу «$searchQuery» треков не обнаружено. Попробуйте изменить запрос",
                                            icon = if (searchQuery.isBlank()) Icons.Default.Search else Icons.Default.MusicNote,
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
                                                        onAddToPlaylist = { trackForPlaylistDialog = track },
                                                        onEditTrackInfo = { trackForEditInfoDialog = track }
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
                                                    .padding(end = 16.dp, bottom = 124.dp)
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

    trackForEditInfoDialog?.let { track ->
        EditTrackInfoDialog(
            track = track,
            onDismiss = { trackForEditInfoDialog = null },
            onConfirm = { updatedTitle, updatedArtist, updatedAlbum, updatedGenre, updatedYear ->
                viewModel.updateTrackInfo(
                    trackId = track.id,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    genre = updatedGenre,
                    year = updatedYear
                )
                trackForEditInfoDialog = null
            }
        )
    }

    if (showEqualizerDialog) {
        val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
        val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
        val isEqualizerEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()
        EqualizerDialog(
            isEnabled = isEqualizerEnabled,
            bands = equalizerBands,
            currentPreset = equalizerPreset,
            onEnableChanged = { viewModel.setEqualizerEnabled(it) },
            onPresetSelected = { viewModel.setEqualizerPreset(it) },
            onBandLevelChanged = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
            onDismiss = { showEqualizerDialog = false },
            viewModel = viewModel
        )
    }

    if (showSleepTimerDialog) {
        val sleepTimerRemainingMillis by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()
        val sleepTimerMode by viewModel.sleepTimerMode.collectAsStateWithLifecycle()
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

private val DrilldownSharedAxisTransition: androidx.compose.animation.AnimatedContentTransitionScope<Boolean>.() -> androidx.compose.animation.ContentTransform = {
    if (targetState) {
        // Forward transition: entering drilldown (detail screen)
        (slideInHorizontally(
            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
            initialOffsetX = { (it * 0.18f).toInt() }
        ) + scaleIn(
            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
            initialScale = 0.92f
        ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing)))
        .togetherWith(
            slideOutHorizontally(
                animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                targetOffsetX = { -(it * 0.14f).toInt() }
            ) + scaleOut(
                animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                targetScale = 0.94f
            ) + fadeOut(animationSpec = tween(160, easing = PlayerEmphasizedAccelerateEasing))
        )
    } else {
        // Reverse transition: returning back to root screen
        (slideInHorizontally(
            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
            initialOffsetX = { -(it * 0.14f).toInt() }
        ) + scaleIn(
            animationSpec = tween(240, easing = PlayerEmphasizedDecelerateEasing),
            initialScale = 0.94f
        ) + fadeIn(animationSpec = tween(180, easing = PlayerEmphasizedDecelerateEasing)))
        .togetherWith(
            slideOutHorizontally(
                animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                targetOffsetX = { (it * 0.18f).toInt() }
            ) + scaleOut(
                animationSpec = tween(200, easing = PlayerEmphasizedAccelerateEasing),
                targetScale = 0.92f
            ) + fadeOut(animationSpec = tween(160, easing = PlayerEmphasizedAccelerateEasing))
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
    onEditTrackInfo: (Track) -> Unit = {},
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    AnimatedContent(
        targetState = selectedTitle != null,
        transitionSpec = DrilldownSharedAxisTransition,
        label = "artist_drilldown_transition"
    ) { hasSelectedTitle ->
        if (hasSelectedTitle && selectedTitle != null) {
        val tracks = groups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()
        val distinctArtUris = remember(tracks) {
            tracks.firstDistinctArtUris()
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                        RoundedCornerShape(20.dp)
                    )
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
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onEditTrackInfo = { onEditTrackInfo(track) }
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
                        .padding(end = 16.dp, bottom = 124.dp)
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
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .border(
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectGroup(groupKey, groupTracks) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(18.dp))
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
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
    // Rebuilt on every recomposition before: each card change (current track, favourite,
    // scan) produced four new ImageRequest objects and restarted their crossfades.
    val tileRequests = remember(distinctUris) {
        distinctUris.take(4).map { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .size(120, 120)
                .crossfade(100)
                .build()
        }
    }

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
                            model = tileRequests[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = tileRequests[1],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface))
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = tileRequests[2],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = tileRequests[3],
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
                            model = tileRequests[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
                        AsyncImage(
                            model = tileRequests[1],
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
    onEditTrackInfo: (Track) -> Unit = {},
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    AnimatedContent(
        targetState = selectedTitle != null,
        transitionSpec = DrilldownSharedAxisTransition,
        label = "folders_drilldown_transition"
    ) { hasSelectedTitle ->
        if (hasSelectedTitle && selectedTitle != null) {
            val tracks = folderGroups[selectedTitle] ?: emptyList()
        val listState = rememberLazyListState()
        val distinctArtUris = remember(tracks) {
            tracks.firstDistinctArtUris()
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
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onEditTrackInfo = { onEditTrackInfo(track) }
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
                        .padding(end = 16.dp, bottom = 124.dp)
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
                        folderTracks.firstDistinctArtUris()
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectGroup(folderName, folderTracks) }
                            .testTag("folder_card_$folderName")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            FolderCoverCollage(
                                artUris = distinctArtUris,
                                size = 52.dp,
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
    onEditTrackInfo: (Track) -> Unit = {},
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    val context = LocalContext.current

    AnimatedContent(
        targetState = selectedTitle != null,
        transitionSpec = DrilldownSharedAxisTransition,
        label = "albums_drilldown_transition"
    ) { hasSelectedTitle ->
        if (hasSelectedTitle && selectedTitle != null) {
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
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onEditTrackInfo = { onEditTrackInfo(track) }
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
                        .padding(end = 16.dp, bottom = 124.dp)
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
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { onSelectGroup(albumTitle, tracks) }
                            .testTag("album_card_$albumTitle")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.84f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val coverRequest = remember(albumArtUri) {
                                ImageRequest.Builder(context)
                                    .data(albumArtUri ?: R.drawable.ic_default_art)
                                    .size(420, 500)
                                    .crossfade(180)
                                    .error(R.drawable.ic_default_art)
                                    .build()
                            }

                            AsyncImage(
                                model = coverRequest,
                                contentDescription = "Обложка $albumTitle",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0x18000000),
                                                Color(0xD9000000)
                                            ),
                                            startY = 120f
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.42f))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${tracks.size} треков",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(start = 13.dp, end = 48.dp, bottom = 14.dp)
                            ) {
                                Text(
                                    text = albumTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (tracks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .size(34.dp)
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
    onEditTrackInfo: (Track) -> Unit = {},
    getPlaylistTracks: (Long) -> kotlinx.coroutines.flow.StateFlow<List<Track>>,
    favoriteIds: Set<Long> = emptySet(),
    onNavigateToQueue: () -> Unit = {},
    onMoveAllTracksTrack: (Int, Int) -> Unit = { _, _ -> },
    onMoveFavoriteTrack: (Int, Int) -> Unit = { _, _ -> },
    onMovePlaylistTrack: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onShuffleTracks: (List<Track>) -> Unit = { list -> if (list.isNotEmpty()) onPlayTrack(list.random(), list) }
) {
    val isDrilldown = selectedCategoryTitle != null || selectedPlaylist != null

    AnimatedContent(
        targetState = isDrilldown,
        transitionSpec = DrilldownSharedAxisTransition,
        label = "playlists_drilldown_transition"
    ) { hasDrilldown ->
        if (hasDrilldown) {
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
            tracks.firstDistinctArtUris()
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
                        onEditTrackInfo = { onEditTrackInfo(it) },
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
                            .padding(end = 16.dp, bottom = 124.dp)
                    )
                }
            }
        }
    } else {
        val allTracksCollageUris = remember(allTracks) {
            allTracks.firstDistinctArtUris()
        }
        val favoritesCollageUris = remember(favorites) {
            favorites.firstDistinctArtUris()
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                            RoundedCornerShape(18.dp)
                        )
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
                        containerColor = FavoriteRed.copy(alpha = 0.075f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .border(
                            BorderStroke(1.dp, FavoriteRed.copy(alpha = 0.16f)),
                            RoundedCornerShape(18.dp)
                        )
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            RoundedCornerShape(18.dp)
                        )
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
                    plTracks.firstDistinctArtUris()
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)),
                            RoundedCornerShape(22.dp)
                        )
                        .clickable { onSelectPlaylist(pl) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
                    ) {
                        if (plCollageUris.isNotEmpty()) {
                            FolderCoverCollage(
                                artUris = plCollageUris,
                                size = 60.dp,
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
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

                        Spacer(modifier = Modifier.width(15.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
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
}
