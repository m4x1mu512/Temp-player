package com.example.ui.components

import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AudioTrackSpecs
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.ui.screens.AudioSpecsBadge
import com.example.ui.theme.FavoriteRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.util.formatTime
import com.example.ui.util.rememberPlayerColors
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SheetDragDirection { NONE, VERTICAL, HORIZONTAL }

@Composable
fun ExpandablePlayerSheet(
    viewModel: MainViewModel,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    if (currentTrack == null) return

    val track = currentTrack!!
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val positionFlow = viewModel.playbackPosition
    val position by positionFlow.collectAsStateWithLifecycle()

    val visualizerEnabled by viewModel.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()

    val sleepTimerMode by viewModel.sleepTimerMode.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()
    val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()

    val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val isEqualizerEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isCurrentTrackFavorite = remember(favoriteIds, track.id) {
        favoriteIds.contains(track.id)
    }

    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    var showVisualizerModeHint by remember { mutableStateOf(false) }
    LaunchedEffect(visualizerMode) {
        showVisualizerModeHint = true
        delay(1200)
        showVisualizerModeHint = false
    }

    val miniPlayerBgMode by viewModel.miniPlayerBgMode.collectAsStateWithLifecycle()
    val miniPlayerCustomColor by viewModel.miniPlayerCustomColor.collectAsStateWithLifecycle()
    val trackAudioSpecs by viewModel.trackAudioSpecs.collectAsStateWithLifecycle()

    val audioSpecs = trackAudioSpecs ?: remember(track) {
        val ext = (if (track.path.isNotBlank()) track.path else track.uriString)
            .substringBefore('?')
            .substringAfterLast('.', "")
            .uppercase()
            .ifEmpty { "MP3" }
        val calcBitrate = if (track.size > 0 && track.duration > 0) {
            ((track.size * 8L) / track.duration).toInt().coerceIn(32, 9216)
        } else 320
        AudioTrackSpecs(
            format = ext,
            sampleRateHz = 44100,
            bitrateKbps = calcBitrate,
            bitDepth = 16,
            channelCount = 2,
            isLossless = ext in listOf("FLAC", "WAV", "ALAC", "AIFF")
        )
    }

    val colorScheme = rememberPlayerColors(
        bgMode = miniPlayerBgMode,
        albumArtUri = track.albumArtUri,
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

    val isLandscape = autoRotate && (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val miniBarHeight = if (isLandscape) 56.dp else 64.dp
        val collapsedVisibleHeight = miniBarHeight + navBarsBottom

        val screenHeightPx = with(density) { screenHeight.toPx() }
        val collapsedVisibleHeightPx = with(density) { collapsedVisibleHeight.toPx() }
        val maxDragPx = (screenHeightPx - collapsedVisibleHeightPx).coerceAtLeast(0f)

        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val animOffsetY = remember { Animatable(maxDragPx) }
        var dragOffsetY by remember { mutableFloatStateOf(maxDragPx) }
        var isDragging by remember { mutableStateOf(false) }
        var isInitialized by remember { mutableStateOf(false) }

        val currentOffsetY = if (isDragging) dragOffsetY else animOffsetY.value
        val expandProgress = if (maxDragPx > 0f) {
            (1f - (currentOffsetY / maxDragPx)).coerceIn(0f, 1f)
        } else if (isExpanded) 1f else 0f

        LaunchedEffect(maxDragPx) {
            if (maxDragPx > 0f) {
                if (!isInitialized) {
                    isInitialized = true
                    val target = if (isExpanded) 0f else maxDragPx
                    dragOffsetY = target
                    animOffsetY.snapTo(target)
                } else if (!isExpanded && !isDragging && !animOffsetY.isRunning) {
                    dragOffsetY = maxDragPx
                    animOffsetY.snapTo(maxDragPx)
                }
            }
        }

        // Expand / Collapse Helper functions
        val expandToFull: () -> Unit = {
            isExpanded = true
            isDragging = false
            coroutineScope.launch {
                animOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
        val collapseToMini: () -> Unit = {
            isExpanded = false
            isDragging = false
            coroutineScope.launch {
                animOffsetY.animateTo(
                    targetValue = maxDragPx,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }

        val settleToTarget: (velocityY: Float, totalDragY: Float) -> Unit = { velocityY, totalDragY ->
            val targetExpanded = when {
                velocityY < -60f -> true // light flick up -> expand
                velocityY > 60f -> false // light flick down -> collapse
                isExpanded -> !(totalDragY > 15f || expandProgress < 0.96f) // short swipe down (>15px) collapses
                else -> totalDragY < -15f || expandProgress > 0.04f // short swipe up (< -15px) opens
            }
            isExpanded = targetExpanded
            coroutineScope.launch {
                animOffsetY.snapTo(dragOffsetY)
                isDragging = false
                val targetValue = if (targetExpanded) 0f else maxDragPx
                animOffsetY.animateTo(
                    targetValue = targetValue,
                    initialVelocity = velocityY,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }

        // Listen for navigation-to-player requests from ViewModel or elsewhere
        LaunchedEffect(Unit) {
            viewModel.navigateToPlayerEvent.collect {
                expandToFull()
            }
        }

        // BackHandler: collapses player when expanded
        BackHandler(enabled = isExpanded || expandProgress > 0.05f) {
            collapseToMini()
        }

        val sheetCornerRadius = lerp(20.dp, 0.dp, expandProgress)

        // Sheet Container with real-time translationY revealing whatever screen is underneath
        MaterialTheme(colorScheme = playerMaterialColorScheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, currentOffsetY.roundToInt()) }
                    .shadow(
                        elevation = lerp(12.dp, 0.dp, expandProgress),
                        shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
                    )
                    .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius))
                    .then(
                        if (colorScheme.isGradient && colorScheme.backgroundBrush != null) {
                            Modifier.background(colorScheme.backgroundBrush)
                        } else {
                            Modifier.background(colorScheme.backgroundColor)
                        }
                    )
                    .border(
                        border = if (expandProgress < 0.2f) {
                            if (!colorScheme.isDark) BorderStroke(1.dp, Color(0x18000000)) else BorderStroke(1.dp, Color(0x1AFFFFFF))
                        } else BorderStroke(0.dp, Color.Transparent),
                        shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
                    )
                    .pointerInput(maxDragPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var dragDir = SheetDragDirection.NONE
                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val velocityX = velocity.x
                                    val velocityY = velocity.y

                                    if (dragDir == SheetDragDirection.VERTICAL) {
                                        settleToTarget(velocityY, totalDragY)
                                    } else if (dragDir == SheetDragDirection.HORIZONTAL) {
                                        // Strictly handle track switching - NEVER collapse/expand on horizontal swipe!
                                        if (totalDragX < -35f || velocityX < -400f) {
                                            viewModel.nextTrack()
                                        } else if (totalDragX > 35f || velocityX > 400f) {
                                            viewModel.previousTrack()
                                        }
                                    } else {
                                        // Quick flick with very little displacement
                                        val isPredominantlyVertical = abs(velocityY) > abs(velocityX) * 1.5f && abs(velocityY) > 180f
                                        val isPredominantlyHorizontal = abs(velocityX) > abs(velocityY) * 1.5f && abs(velocityX) > 300f

                                        if (isPredominantlyVertical) {
                                            settleToTarget(velocityY, totalDragY)
                                        } else if (isPredominantlyHorizontal) {
                                            if (totalDragX < -30f || velocityX < -300f) {
                                                viewModel.nextTrack()
                                            } else if (totalDragX > 30f || velocityX > 300f) {
                                                viewModel.previousTrack()
                                            }
                                        }
                                    }
                                    break
                                }

                                if (change.isConsumed) {
                                    continue
                                }

                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val posChange = change.positionChange()
                                if (posChange != Offset.Zero) {
                                    totalDragX += posChange.x
                                    val dy = posChange.y
                                    totalDragY += dy

                                    if (dragDir == SheetDragDirection.NONE) {
                                        if (abs(totalDragX) > 14f && abs(totalDragX) > abs(totalDragY) * 1.25f) {
                                            dragDir = SheetDragDirection.HORIZONTAL
                                        } else if (abs(totalDragY) > 12f && abs(totalDragY) > abs(totalDragX) * 1.25f) {
                                            dragDir = SheetDragDirection.VERTICAL
                                            isDragging = true
                                            dragOffsetY = animOffsetY.value
                                        }
                                    }

                                    if (dragDir == SheetDragDirection.VERTICAL) {
                                        change.consume()
                                        dragOffsetY = (dragOffsetY + dy).coerceIn(0f, maxDragPx)
                                    } else if (dragDir == SheetDragDirection.HORIZONTAL) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                    .testTag(if (expandProgress > 0.5f) "player_screen" else "mini_player")
            ) {
                // 1. ALBUM ARTWORK: Smoothly interpolates size, position, and corner radius!
                val context = LocalContext.current
                val miniArtSize = if (isLandscape) 50.dp else 54.dp
                val miniArtX = if (isLandscape) 20.dp else 14.dp
                val miniArtY = if (isLandscape) 4.dp else 5.dp
                val miniCornerRadius = 12.dp

                val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topBarHeight = statusBarsTop + 54.dp

                val fullArtSize = if (isLandscape) {
                    minOf(screenWidth * 0.40f, (screenHeight - 60.dp) * 0.85f).coerceIn(160.dp, 320.dp)
                } else {
                    minOf(screenWidth * 0.78f, (screenHeight - topBarHeight) * 0.38f).coerceIn(180.dp, 360.dp)
                }

                val fullArtX = if (isLandscape) {
                    28.dp + ((screenWidth * 0.44f) - fullArtSize).coerceAtLeast(0.dp) / 2
                } else {
                    (screenWidth - fullArtSize) / 2
                }

                val fullArtY = if (isLandscape) {
                    statusBarsTop + (screenHeight - statusBarsTop - fullArtSize).coerceAtLeast(0.dp) / 2
                } else {
                    topBarHeight + 10.dp
                }

                val fullCornerRadius = if (isLandscape) 20.dp else 26.dp

                val currentArtSize = lerp(miniArtSize, fullArtSize, expandProgress)
                val currentArtX = lerp(miniArtX, fullArtX, expandProgress)
                val currentArtY = lerp(miniArtY, fullArtY, expandProgress)
                val currentArtCornerRadius = lerp(miniCornerRadius, fullCornerRadius, expandProgress)

                // Single Smoothly Morphing Artwork Box
                Box(
                    modifier = Modifier
                        .offset(x = currentArtX, y = currentArtY)
                        .size(currentArtSize)
                        .shadow(
                            elevation = lerp(2.dp, 24.dp, expandProgress),
                            shape = RoundedCornerShape(currentArtCornerRadius),
                            spotColor = if (isPlaying) NeonCyan.copy(alpha = 0.5f * expandProgress) else Color.Black.copy(alpha = 0.2f),
                            ambientColor = if (isPlaying) NeonPurple.copy(alpha = 0.35f * expandProgress) else Color.Transparent
                        )
                        .clip(RoundedCornerShape(currentArtCornerRadius))
                        .background(colorScheme.progressTrackColor)
                        .clickable(enabled = expandProgress < 0.35f) {
                            expandToFull()
                        }
                ) {
                    Crossfade(
                        targetState = track.albumArtUri,
                        animationSpec = tween(durationMillis = 180),
                        label = "expandable_player_art_crossfade"
                    ) { artUri ->
                        if (artUri == null) {
                            Image(
                                painter = painterResource(R.drawable.ic_default_art),
                                contentDescription = "Обложка трека",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val artRequest = remember(artUri) {
                                ImageRequest.Builder(context)
                                    .data(artUri)
                                    .crossfade(false)
                                    .error(R.drawable.ic_default_art)
                                    .build()
                            }
                            AsyncImage(
                                model = artRequest,
                                contentDescription = "Обложка трека",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 2. MINI PLAYER ELEMENTS (Visible when collapsed, smoothly fades out on expansion)
                val miniAlpha = (1f - expandProgress * 2.5f).coerceIn(0f, 1f)
                if (miniAlpha > 0f) {
                    // Continuous thin progress line at top of mini player
                    val targetProgress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = if (isPlaying) tween(durationMillis = 250, easing = LinearEasing) else tween(0),
                        label = "mini_animated_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .graphicsLayer { alpha = miniAlpha }
                            .testTag("mini_player_progress"),
                        color = colorScheme.accentColor,
                        trackColor = colorScheme.progressTrackColor
                    )

                    // Mini player clickable backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniBarHeight)
                            .graphicsLayer { alpha = miniAlpha }
                            .clickable(enabled = expandProgress < 0.35f) {
                                expandToFull()
                            }
                    ) {
                        // Title & Artist for MiniPlayer
                        val textStartX = currentArtX + currentArtSize + 12.dp
                        val buttonsWidth = if (isLandscape) 210.dp else 180.dp
                        val textWidth = (screenWidth - textStartX - buttonsWidth).coerceAtLeast(60.dp)

                        Column(
                            modifier = Modifier
                                .offset(x = textStartX, y = if (isLandscape) 8.dp else 12.dp)
                                .width(textWidth)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Mini Player Control Buttons (Favorite, Previous, Play/Pause, Next)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleFavorite(track.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("mini_player_favorite")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isCurrentTrackFavorite) "Удалить из избранного" else "В избранное",
                                    tint = if (isCurrentTrackFavorite) FavoriteRed else colorScheme.onSurfaceVariantColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.previousTrack() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("mini_player_previous")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Предыдущий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = { viewModel.togglePlayPause() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colorScheme.accentColor,
                                    contentColor = colorScheme.onAccentColor
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("mini_player_play_pause")
                            ) {
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = {
                                        (scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeIn(tween(140)))
                                            .togetherWith(scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeOut(tween(100)))
                                    },
                                    label = "mini_play_pause"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playing) "Пауза" else "Воспроизведение",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.nextTrack() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("mini_player_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Следующий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // 3. FULL PLAYER ELEMENTS (Visible when expanded, fades in smoothly)
                val fullAlpha = ((expandProgress - 0.20f) / 0.60f).coerceIn(0f, 1f)
                if (fullAlpha > 0f) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fullAlpha }
                    ) {
                        // Top Bar with Drag Handle & Collapse Icon
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            // Drag Handle Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                )
                            }

                            CenterAlignedTopAppBar(
                                title = {},
                                navigationIcon = {
                                    IconButton(
                                        onClick = { collapseToMini() },
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

                        if (isLandscape) {
                            // Landscape Full Player Layout: Controls in right column
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(0.48f))

                                Column(
                                    modifier = Modifier
                                        .weight(0.52f)
                                        .padding(start = 16.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Title & Artist
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Progress Slider
                                    SheetProgressSection(
                                        trackId = track.id,
                                        positionFlow = positionFlow,
                                        duration = duration,
                                        isPlaying = isPlaying,
                                        onSeek = { viewModel.seekTo(it) }
                                    )

                                    // Primary Playback Controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.previousTrack() }, modifier = Modifier.testTag("player_prev_button")) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущий", modifier = Modifier.size(30.dp))
                                        }
                                        FilledIconButton(
                                            onClick = { viewModel.togglePlayPause() },
                                            modifier = Modifier.size(56.dp).testTag("player_play_pause_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Воспроизведение",
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.nextTrack() }, modifier = Modifier.testTag("player_next_button")) {
                                            Icon(Icons.Default.SkipNext, contentDescription = "Следующий", modifier = Modifier.size(30.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Portrait Full Player Layout
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = navBarsBottom + 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Spacer corresponding to the artwork position
                                Spacer(modifier = Modifier.height(fullArtSize + 12.dp))

                                // Visualizer (if enabled)
                                if (visualizerEnabled) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
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
                                    }
                                }

                                // Track Title & Artist
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (track.album.isNotBlank() && track.album != "Неизвестный альбом") {
                                        Spacer(modifier = Modifier.height(2.dp))
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
                                SheetProgressSection(
                                    trackId = track.id,
                                    positionFlow = positionFlow,
                                    duration = duration,
                                    isPlaying = isPlaying,
                                    onSeek = { viewModel.seekTo(it) }
                                )

                                // Secondary Controls (Shuffle, Favorite, Add to Playlist, Repeat, Queue)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(track.id) },
                                        modifier = Modifier.testTag("player_favorite_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isCurrentTrackFavorite) "Удалить из избранного" else "В избранное",
                                            tint = if (isCurrentTrackFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

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

                                    IconButton(
                                        onClick = { viewModel.toggleRepeat() },
                                        modifier = Modifier.testTag("player_repeat_button")
                                    ) {
                                        when (repeatMode) {
                                            RepeatMode.OFF -> Icon(Icons.Default.Repeat, contentDescription = "Повтор выключен", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            RepeatMode.ALL -> Icon(Icons.Default.Repeat, contentDescription = "Повтор всех", tint = MaterialTheme.colorScheme.primary)
                                            RepeatMode.ONE -> Icon(Icons.Default.RepeatOne, contentDescription = "Повтор одного", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            collapseToMini()
                                            onNavigateToQueue()
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

                                // Primary Playback Controls (-10s, Prev, Play/Pause, Next, +10s)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.seekBackward10s() },
                                        modifier = Modifier.size(46.dp).testTag("player_seek_back_10")
                                    ) {
                                        Icon(Icons.Default.Replay10, contentDescription = "Назад на 10 сек", modifier = Modifier.size(26.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.previousTrack() },
                                        modifier = Modifier.size(52.dp).testTag("player_prev_button")
                                    ) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущий трек", modifier = Modifier.size(34.dp))
                                    }

                                    FilledIconButton(
                                        onClick = { viewModel.togglePlayPause() },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .size(66.dp)
                                            .shadow(12.dp, CircleShape, spotColor = NeonCyan)
                                            .testTag("player_play_pause_button")
                                    ) {
                                        AnimatedContent(
                                            targetState = isPlaying,
                                            transitionSpec = {
                                                (scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeIn(tween(140)))
                                                    .togetherWith(scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeOut(tween(100)))
                                            },
                                            label = "player_play_pause"
                                        ) { playing ->
                                            Icon(
                                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (playing) "Пауза" else "Воспроизведение",
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.nextTrack() },
                                        modifier = Modifier.size(52.dp).testTag("player_next_button")
                                    ) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Следующий трек", modifier = Modifier.size(34.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.seekForward10s() },
                                        modifier = Modifier.size(46.dp).testTag("player_seek_forward_10")
                                    ) {
                                        Icon(Icons.Default.Forward10, contentDescription = "Вперед на 10 сек", modifier = Modifier.size(26.dp))
                                    }
                                }

                                // Audio Specs Badge
                                AudioSpecsBadge(
                                    specs = audioSpecs,
                                    track = track,
                                    modifier = Modifier.testTag("player_audio_specs_badge")
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

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onPlaylistSelected = { pl ->
                viewModel.addTrackToPlaylist(pl.id, track.id)
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

@Composable
private fun SheetProgressSection(
    trackId: Long,
    positionFlow: StateFlow<Long>,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawPosition by positionFlow.collectAsStateWithLifecycle()
    var isUserScrubbing by remember(trackId) { mutableStateOf(false) }
    var scrubPosition by remember(trackId) { mutableFloatStateOf(0f) }

    val smoothAnimatedPos = remember(trackId) { Animatable(0f) }

    LaunchedEffect(trackId) {
        isUserScrubbing = false
        scrubPosition = 0f
        smoothAnimatedPos.snapTo(0f)
    }

    LaunchedEffect(rawPosition, isPlaying, isUserScrubbing, trackId) {
        if (!isUserScrubbing) {
            val rawFloat = rawPosition.toFloat()
            if (!isPlaying) {
                smoothAnimatedPos.snapTo(rawFloat)
            } else {
                if (abs(smoothAnimatedPos.value - rawFloat) > 1200f) {
                    smoothAnimatedPos.snapTo(rawFloat)
                }
                val target = if (duration > 0) minOf(duration.toFloat(), rawFloat + 250f) else rawFloat + 250f
                smoothAnimatedPos.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        val currentFloat = if (isUserScrubbing) scrubPosition else smoothAnimatedPos.value
        val sliderVal = if (duration > 0) currentFloat.coerceIn(0f, duration.toFloat()) else 0f

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
                .testTag("player_progress_slider")
        )

        val currentSeconds = (currentFloat / 1000f).toLong().coerceAtLeast(0L)
        val formattedCurrent = remember(currentSeconds) { formatTime(currentSeconds * 1000L) }
        val formattedDuration = remember(duration) { formatTime(duration) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formattedCurrent,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
