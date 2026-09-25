package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import com.example.ui.theme.FavoriteRed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import android.content.res.Configuration
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.ui.util.rememberPlayerColors
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AudioTrackSpecs
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioVisualizerView
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTurquoise
import com.example.ui.util.formatTime
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val trackAudioSpecs by viewModel.trackAudioSpecs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()

    val visualizerEnabled by viewModel.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()

    val sleepTimerMode by viewModel.sleepTimerMode.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()
    val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()

    val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val isEqualizerEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val isCurrentTrackFavorite = remember(favoriteTracks, currentTrack?.id) {
        val currentId = currentTrack?.id
        if (currentId != null) {
            favoriteTracks.any { it.id == currentId }
        } else {
            false
        }
    }

    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    var dragDistanceX by remember { mutableFloatStateOf(0f) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }
    val localDensity = LocalDensity.current
    val dismissThresholdPx = with(localDensity) { 90.dp.toPx() }
    val context = LocalContext.current

    var showVisualizerModeHint by remember { mutableStateOf(false) }
    LaunchedEffect(visualizerMode) {
        showVisualizerModeHint = true
        delay(1200)
        showVisualizerModeHint = false
    }

    val miniPlayerBgMode by viewModel.miniPlayerBgMode.collectAsStateWithLifecycle()
    val miniPlayerCustomColor by viewModel.miniPlayerCustomColor.collectAsStateWithLifecycle()

    val audioSpecs = trackAudioSpecs ?: remember(currentTrack) {
        currentTrack?.let { t ->
            val ext = (if (t.path.isNotBlank()) t.path else t.uriString).substringBefore('?').substringAfterLast('.', "").uppercase().ifEmpty { "MP3" }
            val calcBitrate = if (t.size > 0 && t.duration > 0) ((t.size * 8L) / t.duration).toInt().coerceIn(32, 9216) else 320
            AudioTrackSpecs(
                format = ext,
                sampleRateHz = 44100,
                bitrateKbps = calcBitrate,
                bitDepth = 16,
                channelCount = 2,
                isLossless = ext in listOf("FLAC", "WAV", "ALAC", "AIFF")
            )
        }
    }

    val colorScheme = rememberPlayerColors(
        bgMode = miniPlayerBgMode,
        albumArtUri = currentTrack?.albumArtUri,
        customColorLong = miniPlayerCustomColor
    )

    val playerMaterialColorScheme = remember(colorScheme) {
        if (colorScheme.isDark) {
            darkColorScheme(
                surface = colorScheme.backgroundColor,
                onSurface = colorScheme.onSurfaceColor,
                onSurfaceVariant = colorScheme.onSurfaceVariantColor,
                primary = colorScheme.accentColor,
                onPrimary = colorScheme.onAccentColor,
                primaryContainer = colorScheme.accentColor.copy(alpha = 0.25f),
                onPrimaryContainer = colorScheme.accentColor,
                background = Color.Transparent,
                onBackground = colorScheme.onSurfaceColor,
                surfaceVariant = colorScheme.progressTrackColor,
                outline = colorScheme.onSurfaceVariantColor.copy(alpha = 0.35f)
            )
        } else {
            lightColorScheme(
                surface = colorScheme.backgroundColor,
                onSurface = colorScheme.onSurfaceColor,
                onSurfaceVariant = colorScheme.onSurfaceVariantColor,
                primary = colorScheme.accentColor,
                onPrimary = colorScheme.onAccentColor,
                primaryContainer = colorScheme.accentColor.copy(alpha = 0.15f),
                onPrimaryContainer = colorScheme.accentColor,
                background = Color.Transparent,
                onBackground = colorScheme.onSurfaceColor,
                surfaceVariant = colorScheme.progressTrackColor,
                outline = colorScheme.onSurfaceVariantColor.copy(alpha = 0.35f)
            )
        }
    }

    MaterialTheme(colorScheme = playerMaterialColorScheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (colorScheme.isGradient && colorScheme.backgroundBrush != null) {
                        Modifier.background(colorScheme.backgroundBrush)
                    } else {
                        Modifier.background(colorScheme.backgroundColor)
                    }
                )
                .graphicsLayer {
                    val offset = dragOffsetY.value.coerceAtLeast(0f)
                    translationY = offset
                    val fraction = (offset / 700f).coerceIn(0f, 1f)
                    alpha = 1f - (fraction * 0.35f)
                    val scale = 1f - (fraction * 0.05f)
                    scaleX = scale
                    scaleY = scale
                }
                .testTag("player_screen")
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                dragDistanceY = 0f
                            },
                            onDragEnd = {
                                if (dragOffsetY.value > dismissThresholdPx) {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(2500f, tween(180))
                                        onNavigateBack()
                                    }
                                } else {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 350f))
                                    }
                                }
                                dragDistanceY = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 350f))
                                }
                                dragDistanceY = 0f
                            },
                            onDrag = { _, dragAmount ->
                                dragDistanceY += dragAmount.y
                                if (dragAmount.y > 0 || dragOffsetY.value > 0f) {
                                    coroutineScope.launch {
                                        dragOffsetY.snapTo((dragOffsetY.value + dragAmount.y).coerceAtLeast(0f))
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Drag handle bar for swipe-down to dismiss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 4.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    )
                }

                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Сейчас играет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("player_collapse_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Свернуть плеер",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSleepTimerDialog = true },
                            modifier = Modifier.testTag("player_sleep_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Таймер сна",
                                tint = if (sleepTimerMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { viewModel.cycleVisualizerMode() },
                            modifier = Modifier.testTag("player_visualizer_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Режим визуализатора: ${visualizerMode.displayName}",
                                tint = if (visualizerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showEqualizerDialog = true },
                            modifier = Modifier.testTag("player_equalizer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Эквалайзер",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        if (currentTrack == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Трек не выбран",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val track = currentTrack!!

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val availableHeight = maxHeight
                val isCompact = availableHeight < 640.dp
                val isMedium = availableHeight in 640.dp..760.dp
                val configuration = LocalConfiguration.current
                val isLandscape = autoRotate && (maxWidth > maxHeight || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

                if (isLandscape) {
                    // Dedicated Landscape horizontal orientation layout for PlayerScreen
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Artwork & Visualizer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayerArtworkCard(
                                    track = track,
                                    isPlaying = isPlaying,
                                    amplitudeFlow = viewModel.audioAmplitude,
                                    dragDistanceX = dragDistanceX,
                                    isLandscape = true,
                                    isCompact = isCompact,
                                    isMedium = isMedium,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (visualizerEnabled) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(42.dp)
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.cycleVisualizerMode() }
                                        .testTag("player_visualizer_container_landscape")
                                ) {
                                    AudioVisualizerView(
                                        fftDataFlow = viewModel.visualizerData,
                                        waveformDataFlow = viewModel.visualizerWaveform,
                                        amplitudeFlow = viewModel.audioAmplitude,
                                        mode = visualizerMode,
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showVisualizerModeHint,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = visualizerMode.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Info, Slider, Secondary Controls, Primary Controls
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Title & Artist
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (track.album.isNotBlank() && track.album != "Неизвестный альбом") {
                                    Text(
                                        text = track.album,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Progress Slider & Timestamps
                            PlayerProgressSection(
                                positionFlow = viewModel.playbackPosition,
                                duration = duration,
                                isCompact = isCompact,
                                onSeek = { viewModel.seekTo(it) },
                                sliderTestTag = "player_progress_slider_landscape",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Secondary Controls: Shuffle, Favorite, Add to Playlist, Repeat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleShuffle() },
                                    modifier = Modifier.testTag("player_shuffle_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Перемешать",
                                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleFavorite(track.id) },
                                    modifier = Modifier.testTag("player_favorite_button_landscape")
                                ) {
                                    val heartScale by animateFloatAsState(
                                        targetValue = if (isCurrentTrackFavorite) 1.2f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "heartScale_land"
                                    )
                                    Icon(
                                        imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isCurrentTrackFavorite) "Удалить из избранного" else "В избранное",
                                        tint = if (isCurrentTrackFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = heartScale
                                            scaleY = heartScale
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = { showAddToPlaylistDialog = true },
                                    modifier = Modifier.testTag("player_add_playlist_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = "Добавить в плейлист",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleRepeat() },
                                    modifier = Modifier.testTag("player_repeat_button_landscape")
                                ) {
                                    when (repeatMode) {
                                        RepeatMode.OFF -> Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Повтор выключен",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        RepeatMode.ALL -> Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Повтор всех",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        RepeatMode.ONE -> Icon(
                                            imageVector = Icons.Default.RepeatOne,
                                            contentDescription = "Повтор одного трека",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.openPlaybackQueue()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.testTag("player_secondary_queue_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Список воспроизведения",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Primary Playback Controls: -10s, Prev, Play/Pause, Next, +10s
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.seekBackward10s() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_seek_back_10_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Перемотка назад на 10 сек",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.previousTrack() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_prev_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Предыдущий трек",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .size(58.dp)
                                        .shadow(10.dp, CircleShape, spotColor = NeonCyan)
                                        .testTag("player_play_pause_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.nextTrack() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_next_button_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Следующий трек",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.seekForward10s() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_seek_forward_10_landscape")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Перемотка вперед на 10 сек",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Characteristics of the played track (kHz, kbps, format) in landscape
                            if (audioSpecs != null) {
                                AudioSpecsBadge(
                                    specs = audioSpecs,
                                    track = track,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .testTag("player_audio_specs_landscape")
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isCompact) 16.dp else 24.dp)
                            .padding(top = 2.dp, bottom = if (isCompact) 4.dp else 8.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragDistanceX = 0f
                                        dragDistanceY = 0f
                                    },
                                    onDragEnd = {
                                        if (abs(dragDistanceY) > abs(dragDistanceX) && dragOffsetY.value > dismissThresholdPx) {
                                            coroutineScope.launch {
                                                dragOffsetY.animateTo(2500f, tween(180))
                                                onNavigateBack()
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 350f))
                                            }
                                            if (abs(dragDistanceX) > abs(dragDistanceY)) {
                                                if (dragDistanceX < -80f) {
                                                    viewModel.nextTrack()
                                                } else if (dragDistanceX > 80f) {
                                                    viewModel.previousTrack()
                                                }
                                            }
                                        }
                                        dragDistanceX = 0f
                                        dragDistanceY = 0f
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 350f))
                                        }
                                        dragDistanceX = 0f
                                        dragDistanceY = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dragDistanceX += dragAmount.x
                                        dragDistanceY += dragAmount.y
                                        if (dragDistanceY > 0f && abs(dragDistanceY) > abs(dragDistanceX)) {
                                            coroutineScope.launch {
                                                dragOffsetY.snapTo((dragOffsetY.value + dragAmount.y).coerceAtLeast(0f))
                                            }
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Responsive Album Art that flexibly scales to available height
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = if (isCompact) 2.dp else 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerArtworkCard(
                                track = track,
                                isPlaying = isPlaying,
                                amplitudeFlow = viewModel.audioAmplitude,
                                dragDistanceX = dragDistanceX,
                                isLandscape = false,
                                isCompact = isCompact,
                                isMedium = isMedium,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                    // Visualizer Canvas View (scaled to fit screen height)
                    if (visualizerEnabled) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCompact) 30.dp else if (isMedium) 36.dp else 42.dp)
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.cycleVisualizerMode() }
                                .testTag("player_visualizer_container_portrait")
                        ) {
                            AudioVisualizerView(
                                fftDataFlow = viewModel.visualizerData,
                                waveformDataFlow = viewModel.visualizerWaveform,
                                amplitudeFlow = viewModel.audioAmplitude,
                                mode = visualizerMode,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize()
                            )

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showVisualizerModeHint,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = visualizerMode.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Track Title and Artist
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = if (isCompact) 1.dp else 3.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.album.isNotBlank() && track.album != "Неизвестный альбом" && !isCompact) {
                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Progress Slider & Timestamps
                    PlayerProgressSection(
                        positionFlow = viewModel.playbackPosition,
                        duration = duration,
                        isCompact = isCompact,
                        onSeek = { viewModel.seekTo(it) },
                        sliderTestTag = "player_progress_slider"
                    )

                    // Secondary controls: Shuffle, Favorite, Add to Playlist, Repeat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 0.dp else 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(
                            onClick = { viewModel.toggleShuffle() },
                            modifier = Modifier.testTag("player_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Перемешать",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Favorite
                        IconButton(
                            onClick = { viewModel.toggleFavorite(track.id) },
                            modifier = Modifier.testTag("player_favorite_button")
                        ) {
                            val heartScale by animateFloatAsState(
                                targetValue = if (isCurrentTrackFavorite) 1.2f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "heartScale"
                            )
                            Icon(
                                imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isCurrentTrackFavorite) "Удалить из избранного" else "В избранное",
                                tint = if (isCurrentTrackFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                }
                            )
                        }

                        // Add to Playlist
                        IconButton(
                            onClick = { showAddToPlaylistDialog = true },
                            modifier = Modifier.testTag("player_add_playlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Добавить в плейлист",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Repeat Mode (OFF, ALL, ONE)
                        IconButton(
                            onClick = { viewModel.toggleRepeat() },
                            modifier = Modifier.testTag("player_repeat_button")
                        ) {
                            when (repeatMode) {
                                RepeatMode.OFF -> Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Повтор выключен",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RepeatMode.ALL -> Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Повтор всех",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                RepeatMode.ONE -> Icon(
                                    imageVector = Icons.Default.RepeatOne,
                                    contentDescription = "Повтор одного трека",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Queue / Список воспроизведения (First tab of Main Screen)
                        IconButton(
                            onClick = {
                                viewModel.openPlaybackQueue()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("player_bottom_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Список воспроизведения",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Primary Playback Controls: -10s, Prev, Play/Pause, Next, +10s
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompact) 1.dp else 3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Replay 10s
                        IconButton(
                            onClick = { viewModel.seekBackward10s() },
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 46.dp)
                                .testTag("player_seek_back_10")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Перемотка назад на 10 сек",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isCompact) 24.dp else 26.dp)
                            )
                        }

                        // Previous Track
                        IconButton(
                            onClick = { viewModel.previousTrack() },
                            modifier = Modifier
                                .size(if (isCompact) 46.dp else 52.dp)
                                .testTag("player_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Предыдущий трек",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isCompact) 30.dp else 34.dp)
                            )
                        }

                        // Main Play/Pause Button
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(if (isCompact) 58.dp else 66.dp)
                                .shadow(if (isCompact) 8.dp else 12.dp, CircleShape, spotColor = NeonCyan)
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
                            )
                        }

                        // Next Track
                        IconButton(
                            onClick = { viewModel.nextTrack() },
                            modifier = Modifier
                                .size(if (isCompact) 46.dp else 52.dp)
                                .testTag("player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Следующий трек",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isCompact) 30.dp else 34.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = { viewModel.seekForward10s() },
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 46.dp)
                                .testTag("player_seek_forward_10")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Перемотка вперед на 10 сек",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isCompact) 24.dp else 26.dp)
                            )
                        }
                    }

                    // Characteristics of the played track (kHz, kbps, format)
                    if (audioSpecs != null) {
                        AudioSpecsBadge(
                            specs = audioSpecs,
                            track = track,
                            modifier = Modifier
                                .padding(top = if (isCompact) 2.dp else 4.dp, bottom = if (isCompact) 2.dp else 4.dp)
                                .testTag("player_audio_specs_badge")
                        )
                    }
                }
                }
            }
        }
    }
    }

    // Modal Dialogs
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
            remainingMillis = sleepTimerRemaining,
            onSetTimer = { viewModel.setSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showAddToPlaylistDialog && currentTrack != null) {
        AddToPlaylistDialog(
            track = currentTrack!!,
            playlists = playlists,
            onPlaylistSelected = { pl ->
                viewModel.addTrackToPlaylist(pl.id, currentTrack!!.id)
                showAddToPlaylistDialog = false
            },
            onCreateNewClicked = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            title = "Новый плейлист",
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }
    }
}

@Composable
fun AudioSpecsBadge(
    specs: AudioTrackSpecs,
    track: Track,
    modifier: Modifier = Modifier
) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDetailsDialog = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.testTag("audio_specs_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Format chip (MP3, FLAC, AAC, WAV, etc.)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (specs.isLossless) NeonCyan.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                border = BorderStroke(
                    1.dp,
                    if (specs.isLossless) NeonCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            ) {
                Text(
                    text = specs.format.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (specs.isLossless) NeonCyan else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Sample rate in kHz (e.g. 44.1 kHz, 48 kHz, 96 kHz)
            Text(
                text = specs.sampleRateFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // Bitrate in kbps (e.g. 320 kbps, 1411 kbps)
            Text(
                text = specs.bitrateFormatted.ifEmpty { "320 kbps" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (specs.bitDepthFormatted.isNotEmpty()) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = specs.bitDepthFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Характеристики звука",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    if (showDetailsDialog) {
        AudioSpecsDetailsDialog(
            specs = specs,
            track = track,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun AudioSpecsDetailsDialog(
    specs: AudioTrackSpecs,
    track: Track,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Характеристики звука",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudioSpecRow(label = "Формат / Кодек", value = specs.format.uppercase() + if (specs.isLossless) " (Lossless)" else "")
                AudioSpecRow(label = "Частота дискретизации", value = "${specs.sampleRateHz} Гц (${specs.sampleRateFormatted})")
                AudioSpecRow(label = "Битрейт", value = "${specs.bitrateKbps} кбит/с")
                AudioSpecRow(label = "Разрядность", value = "${specs.bitDepth} бит")
                AudioSpecRow(label = "Каналы", value = "${specs.channelCount} (${specs.channelsFormatted})")
                if (track.size > 0) {
                    val sizeMb = track.size / (1024.0 * 1024.0)
                    AudioSpecRow(label = "Размер файла", value = String.format(java.util.Locale.US, "%.2f МБ", sizeMb))
                }
                if (track.path.isNotBlank()) {
                    AudioSpecRow(label = "Файл", value = track.path.substringAfterLast('/'))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun AudioSpecRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun PlayerArtworkCard(
    track: Track,
    isPlaying: Boolean,
    amplitudeFlow: StateFlow<Float>,
    dragDistanceX: Float,
    isLandscape: Boolean,
    isCompact: Boolean,
    isMedium: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.035f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val amplitude by amplitudeFlow.collectAsStateWithLifecycle()
    val playbackStateScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "playback_state_scale"
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val artSize = if (isLandscape) {
            minOf(maxWidth * 0.88f, maxHeight * 0.92f).coerceAtLeast(120.dp)
        } else {
            minOf(
                maxWidth * (if (isCompact) 0.68f else if (isMedium) 0.73f else 0.78f),
                maxHeight * (if (isCompact) 0.70f else if (isMedium) 0.75f else 0.80f)
            ).coerceAtLeast(90.dp)
        }

        Box(
            modifier = Modifier
                .size(artSize)
                .graphicsLayer {
                    val ampPulse = if (isPlaying) 1.0f + (amplitude * 0.045f) else 1.0f
                    val scale = pulseScale * ampPulse * playbackStateScale
                    scaleX = scale
                    scaleY = scale
                    rotationY = (dragDistanceX / 25f).coerceIn(-18f, 18f)
                    translationX = (dragDistanceX / 3.5f).coerceIn(-60f, 60f)
                    cameraDistance = 14f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.94f)
                    .shadow(
                        elevation = if (isPlaying) (if (isLandscape) 20.dp else 28.dp) else (if (isLandscape) 8.dp else 10.dp),
                        shape = RoundedCornerShape(if (isLandscape) 22.dp else 28.dp),
                        spotColor = if (isPlaying) NeonCyan.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f),
                        ambientColor = if (isPlaying) NeonPurple.copy(alpha = 0.45f) else Color.Transparent
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(if (isLandscape) 20.dp else 26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (track.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Обложка трека",
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_default_art),
                        placeholder = painterResource(id = R.drawable.ic_default_art),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = R.drawable.ic_default_art,
                        contentDescription = "Обложка трека",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerProgressSection(
    positionFlow: StateFlow<Long>,
    duration: Long,
    isCompact: Boolean,
    onSeek: (Long) -> Unit,
    sliderTestTag: String,
    modifier: Modifier = Modifier
) {
    val position by positionFlow.collectAsStateWithLifecycle()
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 0.dp else 2.dp)
    ) {
        val currentPos = if (isUserScrubbing) scrubPosition.toLong() else position
        val sliderVal = if (duration > 0) currentPos.toFloat().coerceIn(0f, duration.toFloat()) else 0f

        Slider(
            value = sliderVal,
            onValueChange = {
                isUserScrubbing = true
                scrubPosition = it
            },
            onValueChangeFinished = {
                onSeek(scrubPosition.toLong())
                isUserScrubbing = false
            },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sliderTestTag)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPos),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
