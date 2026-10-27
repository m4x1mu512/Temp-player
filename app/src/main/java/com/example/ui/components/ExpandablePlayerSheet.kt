package com.example.ui.components

import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as backdropBlur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.GlowProgressSlider
import com.example.ui.components.InteractiveMiniProgressBar
import com.example.ui.util.formatTime
import com.example.ui.util.rememberPlayerColors
import com.example.ui.util.rememberShouldUseSimplifiedShadows
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class SheetDragDirection { NONE, VERTICAL, HORIZONTAL }

@Composable
fun ExpandablePlayerSheet(
    viewModel: MainViewModel,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onBackgroundBlurChange: (Float) -> Unit = {},
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
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

    val visualizerEnabled by viewModel.visualizerEnabled.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()

    val sleepTimerMode by viewModel.sleepTimerMode.collectAsStateWithLifecycle()
    val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()

    // Equalizer, sleep-timer and playlist state is collected inside the dialogs that use it.
    // Collected here, every equalizer tweak and every sleep-timer tick (twice a second)
    // recomposed the whole sheet - the always-visible mini player included.

    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isCurrentTrackFavorite = remember(favoriteIds, track.id) {
        favoriteIds.contains(track.id)
    }

    val favoriteHeartScale by animateFloatAsState(
        targetValue = if (isCurrentTrackFavorite) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "player_favorite_heart_scale"
    )
    val favoriteHeartColor by animateColorAsState(
        targetValue = if (isCurrentTrackFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "player_favorite_heart_color"
    )

    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()

    var transitionDirection by remember { mutableIntStateOf(1) }
    var lastTrackId by remember { mutableLongStateOf(track.id) }

    LaunchedEffect(track.id) {
        if (track.id != lastTrackId) {
            val prevIdx = currentQueue.indexOfFirst { it.id == lastTrackId }
            val newIdx = currentQueue.indexOfFirst { it.id == track.id }
            if (prevIdx >= 0 && newIdx >= 0 && prevIdx != newIdx) {
                transitionDirection = if (newIdx >= prevIdx) 1 else -1
            }
            lastTrackId = track.id
        }
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

    val audioSpecs = trackAudioSpecs ?: remember(track.path, track.uriString, track.size, track.duration) {
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
        customColorLong = miniPlayerCustomColor,
        trackKey = "${track.id}_${track.title}_${track.albumArtUriString}"
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
        // Match the collapsed mini-player height to a track row:
        // 54dp artwork + 16dp vertical content padding + 6dp outer vertical padding = 76dp.
        val miniBarHeight = 76.dp
        // Keep the collapsed mini-player 76dp above the bottom edge, as requested.
        // This gap controls the sheet position; it does not paint a separate background.
        val miniPlayerBottomGap = 76.dp
        val collapsedVisibleHeight = miniBarHeight + navBarsBottom + miniPlayerBottomGap

        val screenHeightPx = with(density) { screenHeight.toPx() }
        val collapsedVisibleHeightPx = with(density) { collapsedVisibleHeight.toPx() }
        val maxDragPx = (screenHeightPx - collapsedVisibleHeightPx).coerceAtLeast(0f)

        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val animOffsetY = remember { Animatable(maxDragPx) }
        var dragOffsetY by remember { mutableFloatStateOf(maxDragPx) }
        var isDragging by remember { mutableStateOf(false) }
        var isInitialized by remember { mutableStateOf(false) }

        // Keep animation progress out of composition. Continuous values are read only by
        // graphics layers; composition sees endpoint/gesture state and stays stable per frame.
        val expandProgress = if (isExpanded) 1f else 0f

        // Read continuous progress inside draw/layer callbacks where possible, so purely visual
        // effects can update without invalidating the whole composition on every animation frame.
        val progressForDrawing: () -> Float = {
            val offsetY = if (isDragging) dragOffsetY else animOffsetY.value
            if (maxDragPx > 0f) {
                (1f - (offsetY / maxDragPx)).coerceIn(0f, 1f)
            } else if (isExpanded) 1f else 0f
        }

        // Keep the latest callback without restarting a coroutine for every animation frame.
        val latestBackgroundBlurChange by rememberUpdatedState(onBackgroundBlurChange)
        LaunchedEffect(animOffsetY, maxDragPx) {
            snapshotFlow {
                val offsetY = if (isDragging) dragOffsetY else animOffsetY.value
                val progress = if (maxDragPx > 0f) {
                    (1f - (offsetY / maxDragPx)).coerceIn(0f, 1f)
                } else if (isExpanded) 1f else 0f
                (1f - progress).coerceIn(0f, 1f)
            }.collect { blurProgress ->
                latestBackgroundBlurChange(blurProgress)
            }
        }

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
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
        }
        val collapseToMini: () -> Unit = {
            isExpanded = false
            isDragging = false
            coroutineScope.launch {
                animOffsetY.animateTo(
                    targetValue = maxDragPx,
                    animationSpec = tween(durationMillis = 290, easing = FastOutSlowInEasing)
                )
            }
        }
        val collapseToMiniFast: () -> Unit = {
            isExpanded = false
            isDragging = false
            coroutineScope.launch {
                animOffsetY.animateTo(
                    targetValue = maxDragPx,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            }
        }

        // Resolve gesture thresholds once per layout/density change rather than converting dp
        // values when every gesture settles. The thresholds and resulting gesture behavior stay the same.
        val flingThresholdPx = with(density) { 420.dp.toPx() }
        val minimumTravelThresholdPx = with(density) { 36.dp.toPx() }

        val settleToTarget: (velocityY: Float, totalDragY: Float) -> Unit = { velocityY, totalDragY ->
            // Resolve the gesture from direction, meaningful travel, and current sheet position.
            // Tiny accidental movements settle to the nearest endpoint instead of toggling the player.
            val flingThreshold = flingThresholdPx
            val travelThreshold = (maxDragPx * 0.12f).coerceAtLeast(minimumTravelThresholdPx)
            val targetExpanded = when {
                velocityY < -flingThreshold -> true
                velocityY > flingThreshold -> false
                totalDragY < -travelThreshold -> true
                totalDragY > travelThreshold -> false
                else -> progressForDrawing() >= 0.5f
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
        BackHandler(enabled = isExpanded || isDragging || animOffsetY.isRunning) {
            collapseToMini()
        }

        val screenWidthPx = with(density) { screenWidth.toPx() }
        val trackSwipeOffset = remember { Animatable(0f) }
        var trackSwipeDragOffset by remember { mutableFloatStateOf(0f) }
        var isTrackSwiping by remember { mutableStateOf(false) }

        val onSwipeNextTrack: () -> Unit = {
            transitionDirection = 1
            viewModel.nextTrack()
        }

        val onSwipePreviousTrack: () -> Unit = {
            transitionDirection = -1
            viewModel.previousTrack(forcePrevious = true)
        }

        val onButtonPreviousTrack: () -> Unit = {
            transitionDirection = -1
            viewModel.previousTrack(forcePrevious = false)
        }

        val sheetCornerRadius = if (isExpanded) 0.dp else 20.dp

        // Sheet Container with real-time translationY revealing whatever screen is underneath
        MaterialTheme(colorScheme = playerMaterialColorScheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Translate the whole sheet in the layer phase instead of the layout phase.
                    // This avoids a full remeasure/re-layout of the player on every animation frame,
                    // which is especially noticeable on the first expansion after app startup.
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else animOffsetY.value
                        val drawProgress = progressForDrawing()
                        val drawCornerRadius = lerp(20.dp, 0.dp, drawProgress)
                        shape = RoundedCornerShape(
                            topStart = drawCornerRadius,
                            topEnd = drawCornerRadius
                        )
                        clip = true
                        // The container spans the whole screen. Keep its shadow off while
                        // collapsed so it cannot leave vertical shadow lines below the mini-player.
                        // Restore the sheet shadow as it expands.
                        shadowElevation = with(density) {
                            lerp(0.dp, 12.dp, drawProgress).toPx()
                        }
                    }
                    .drawBehind {
                        // Draw the sheet surface on the render pass, blending it in with the
                        // same progress as the sheet motion instead of switching backgrounds
                        // abruptly when the expanded state changes.
                        val backgroundAlpha = progressForDrawing()
                        if (colorScheme.isGradient && colorScheme.backgroundBrush != null) {
                            drawRect(brush = colorScheme.backgroundBrush, alpha = backgroundAlpha)
                        } else {
                            drawRect(color = colorScheme.backgroundColor, alpha = backgroundAlpha)
                        }
                    }
                    .pointerInput(maxDragPx, isExpanded) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var isDraggingSheet = false
                            var isHorizontalTrackSwipe = false
                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    val velocity = velocityTracker.calculateVelocity()
                                    val velocityX = velocity.x
                                    val velocityY = velocity.y

                                    if (isDraggingSheet) {
                                        settleToTarget(velocityY, totalDragY)
                                    } else if (isHorizontalTrackSwipe) {
                                        val swipeThresholdPx = screenWidthPx * 0.22f
                                        val minFlingVelocity = 750f

                                        if (totalDragX < -swipeThresholdPx || (velocityX < -minFlingVelocity && totalDragX < -20f)) {
                                            onSwipeNextTrack()
                                        } else if (totalDragX > swipeThresholdPx || (velocityX > minFlingVelocity && totalDragX > 20f)) {
                                            onSwipePreviousTrack()
                                        }
                                        coroutineScope.launch {
                                            // Transfer the final pointer position once, then animate back.
                                            trackSwipeOffset.snapTo(trackSwipeDragOffset)
                                            isTrackSwiping = false
                                            trackSwipeOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    } else {
                                        // Quick vertical swipe without full drag engagement
                                        if (isExpanded) {
                                            if ((velocityY > 300f || totalDragY > 30f) && totalDragY > abs(totalDragX) * 1.2f) {
                                                collapseToMini()
                                            }
                                        } else {
                                            if ((velocityY < -300f || totalDragY < -30f) && abs(totalDragY) > abs(totalDragX) * 1.2f) {
                                                expandToFull()
                                            }
                                        }
                                    }
                                    break
                                }

                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val posChange = change.positionChange()
                                if (posChange != Offset.Zero) {
                                    totalDragX += posChange.x
                                    val dy = posChange.y
                                    totalDragY += dy

                                    if (!isDraggingSheet && !isHorizontalTrackSwipe) {
                                        if (isExpanded) {
                                            // When expanded: downward swipe collapses sheet (horizontal swipes handled by artwork and title)
                                            if (totalDragY > 10f && totalDragY > abs(totalDragX) * 0.8f) {
                                                isDraggingSheet = true
                                                isDragging = true
                                                dragOffsetY = animOffsetY.value
                                            }
                                        } else {
                                            // When collapsed (mini player): slight upward swipe expands sheet, horizontal swipe changes track
                                            if (totalDragY < -5f && abs(totalDragY) > abs(totalDragX) * 0.6f) {
                                                isDraggingSheet = true
                                                isDragging = true
                                                dragOffsetY = animOffsetY.value
                                            } else if (abs(totalDragX) > 28f && abs(totalDragX) > abs(totalDragY) * 1.5f) {
                                                isHorizontalTrackSwipe = true
                                                isTrackSwiping = true
                                            }
                                        }
                                    }

                                    if (isDraggingSheet) {
                                        change.consume()
                                        dragOffsetY = (dragOffsetY + dy).coerceIn(0f, maxDragPx)
                                    } else if (isHorizontalTrackSwipe) {
                                        change.consume()
                                        // Pointer movement updates the layer state directly; no coroutine per event.
                                        trackSwipeDragOffset = totalDragX
                                    }
                                }
                            }
                        }
                    }
                    .testTag(if (isExpanded) "player_screen" else "mini_player")
            ) {
                // One uniform frosted-glass surface for the collapsed player.
                // Keep the tint translucent and avoid nested backgrounds that could
                // create the lighter inset rectangle seen in earlier screenshots.
                if (!isExpanded || animOffsetY.isRunning) {
                    // The collapsed glass surface ends just below the progress bar,
                    // leaving a small gap to the device's bottom edge. Round all four corners
                    // only on this surface; the full player keeps its existing sheet geometry.
                    val glassShape = RoundedCornerShape(
                        topStart = sheetCornerRadius,
                        topEnd = sheetCornerRadius,
                        bottomStart = if (isExpanded) 0.dp else 18.dp,
                        bottomEnd = if (isExpanded) 0.dp else 18.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniBarHeight)
                            .graphicsLayer {
                                alpha = (1f - progressForDrawing()).coerceIn(0f, 1f)
                            }
                            // Read only the page layer behind this sibling surface. The backdrop
                            // is clipped to the same rounded shape, so no rectangular blur layer
                            // can leak over the mini-player's text or controls.
                            .then(
                                if (backdrop != null) {
                                    Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { glassShape },
                                        effects = {
                                            backdropBlur(24.dp.toPx())
                                        }
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                // A restrained album-derived gradient keeps the whole collapsed
                                // surface translucent. Keep it on this exact clipped layer so the
                                // tint cannot form a separate inset rectangle over the artwork/text.
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorScheme.backgroundColor.copy(
                                            alpha = if (colorScheme.isDark) 0.30f else 0.25f
                                        ),
                                        colorScheme.accentColor.copy(
                                            alpha = if (colorScheme.isDark) 0.17f else 0.12f
                                        ),
                                        colorScheme.backgroundColor.copy(
                                            alpha = if (colorScheme.isDark) 0.30f else 0.25f
                                        )
                                    )
                                ),
                                shape = glassShape
                            )
                            .border(
                                width = 1.dp,
                                color = (if (colorScheme.isDark) Color.White else Color.Black)
                                    .copy(alpha = 0.08f),
                                shape = glassShape
                            )
                    )
                }

                // 1. ALBUM ARTWORK: Smoothly interpolates size, position, and corner radius!
                val context = LocalContext.current
                val miniArtSize = if (isLandscape) 50.dp else 54.dp
                val miniArtX = if (isLandscape) 20.dp else 14.dp
                // Center the artwork vertically in the collapsed mini-player so its center
                // has equal spacing to the top and bottom edges in both orientations.
                val miniArtY = ((miniBarHeight - miniArtSize) / 2).coerceAtLeast(0.dp)
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

                // Mini-player text fades out during expansion, so keep its layout anchored
                // to the collapsed artwork instead of recomputing it for every animation frame.
                val currentArtSize = miniArtSize
                val currentArtX = miniArtX
                val currentArtY = miniArtY
                val currentArtCornerRadius = miniCornerRadius

                // Soft album-art accent glow: adds depth to the full player without competing with the cover.
                if (isExpanded || animOffsetY.isRunning) {
                    Box(
                        modifier = Modifier
                            .offset(y = (fullArtY - 44.dp).coerceAtLeast(0.dp))
                            .fillMaxWidth()
                            .height(fullArtSize + 88.dp)
                            .graphicsLayer {
                                val drawProgress = progressForDrawing()
                                alpha = ((drawProgress - 0.35f) / 0.65f).coerceIn(0f, 1f) * 0.72f
                            }
                            .blur(44.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colorScheme.accentColor.copy(alpha = 0.22f),
                                        colorScheme.accentColor.copy(alpha = 0.07f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Keep one artwork composable alive for the entire transition.
                // Swapping to a separate ExpandedSlidingArtwork at 85% progress used to
                // compose a new image/shadow/gesture subtree mid-animation and could stall
                // the first expansion. The same artwork now morphs continuously from mini
                // to expanded geometry without a late compositional hand-off.
                val simplifiedShadows = rememberShouldUseSimplifiedShadows()
                Box(
                    modifier = Modifier
                        .offset(x = miniArtX, y = miniArtY)
                        .size(fullArtSize)
                        .graphicsLayer {
                            val drawProgress = progressForDrawing()
                            val miniScale = miniArtSize / fullArtSize
                            val scale = (miniScale + (1f - miniScale) * drawProgress).coerceAtLeast(0.01f)
                            val drawCornerRadius = lerp(miniCornerRadius, fullCornerRadius, drawProgress)
                            val swipe = if (isTrackSwiping) trackSwipeDragOffset else trackSwipeOffset.value

                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            scaleX = scale
                            scaleY = scale
                            translationX = with(density) {
                                (lerp(miniArtX, fullArtX, drawProgress) - miniArtX).toPx()
                            } + swipe * 0.4f
                            translationY = with(density) {
                                (lerp(miniArtY, fullArtY, drawProgress) - miniArtY).toPx()
                            }
                            shape = RoundedCornerShape(drawCornerRadius / scale)
                            clip = true
                            shadowElevation = with(density) {
                                (lerp(2.dp, if (simplifiedShadows) 8.dp else 18.dp, drawProgress) / scale).toPx()
                            }
                            spotShadowColor = if (!simplifiedShadows && isPlaying) {
                                NeonCyan.copy(alpha = 0.18f * drawProgress)
                            } else {
                                Color.Black.copy(alpha = 0.16f)
                            }
                            ambientShadowColor = if (!simplifiedShadows && isPlaying) {
                                NeonPurple.copy(alpha = 0.10f * drawProgress)
                            } else {
                                Color.Transparent
                            }
                        }
                        .background(colorScheme.progressTrackColor)
                        .then(
                            if (isExpanded) {
                                Modifier.pointerInput(track.id, isExpanded) {
                                    var totalDragX = 0f
                                    var velocityTracker = VelocityTracker()
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            totalDragX = 0f
                                            velocityTracker = VelocityTracker()
                                        },
                                        onDragEnd = {
                                            val velocity = velocityTracker.calculateVelocity().x
                                            val thresholdPx = size.width * 0.20f
                                            if (totalDragX < -thresholdPx || (velocity < -600f && totalDragX < -20f)) {
                                                onSwipeNextTrack()
                                            } else if (totalDragX > thresholdPx || (velocity > 600f && totalDragX > 20f)) {
                                                onSwipePreviousTrack()
                                            }
                                            trackSwipeDragOffset = 0f
                                            isTrackSwiping = false
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragX += dragAmount
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            trackSwipeDragOffset = totalDragX
                                            isTrackSwiping = true
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Crossfade(
                        targetState = track.albumArtUri,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
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
                            // Decode straight to the size the artwork is drawn at. Without this
                            // Coil decoded full-resolution covers (a 3000x3000 JPEG is ~36 MB
                            // as ARGB_8888) and the crossfade kept two of them alive at once.
                            val artTargetPx = with(LocalDensity.current) {
                                fullArtSize.roundToPx()
                            }.coerceAtLeast(256)
                            val artRequest = remember(artUri, artTargetPx) {
                                ImageRequest.Builder(context)
                                    .data(artUri)
                                    .size(artTargetPx)
                                    .crossfade(false)
                                    .placeholder(R.drawable.ic_default_art)
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
                val miniAlpha = 1f
                if (!isExpanded || animOffsetY.isRunning) {
                    // The collapsed mini-player intentionally has no progress bar.


                    // Mini player clickable backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniBarHeight)
                            .graphicsLayer {
                                alpha = (1f - progressForDrawing() * 2.5f).coerceIn(0f, 1f)
                            }
                            .clickable(enabled = expandProgress < 0.35f) {
                                expandToFull()
                            }
                    ) {
                        // Title, Artist, and Track Time (Current / Duration)
                        val textStartX = currentArtX + currentArtSize + 12.dp
                        val buttonsWidth = if (isLandscape) 210.dp else 180.dp
                        val textWidth = (screenWidth - textStartX - buttonsWidth).coerceAtLeast(60.dp)

                        Column(
                            modifier = Modifier
                                .offset(x = textStartX, y = if (isLandscape) 6.dp else 8.dp)
                                .width(textWidth)
                                .graphicsLayer {
                                    if (expandProgress < 0.35f) {
                                        val swipe = if (isTrackSwiping) trackSwipeDragOffset else trackSwipeOffset.value
                                        translationX = swipe * 0.45f
                                    }
                                }
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

                        // Mini Player Control Buttons (Favorite, Previous/Rewind, Play/Pause, Next/Fast-Forward)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                // Keep the controls' vertical center on the album artwork's center line.
                                .offset(y = miniArtY + miniArtSize / 2 - miniBarHeight / 2)
                                // Keep the controls' visual travel in the graphics layer rather than
                                // recalculating their layout position on every animation frame.
                                .graphicsLayer {
                                    val drawProgress = progressForDrawing()
                                    val miniCenterOffset = miniArtY + miniArtSize / 2 - miniBarHeight / 2
                                    val fullCenterOffset = fullArtY + fullArtSize / 2 - miniBarHeight / 2
                                    translationY = with(density) {
                                        (lerp(miniCenterOffset, fullCenterOffset, drawProgress) - miniCenterOffset).toPx()
                                    }
                                }
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
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = favoriteHeartScale
                                            scaleY = favoriteHeartScale
                                        }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onButtonPreviousTrack)
                                    .testTag("mini_player_previous"),
                                contentAlignment = Alignment.Center
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

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onSwipeNextTrack)
                                    .testTag("mini_player_next"),
                                contentAlignment = Alignment.Center
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
                val fullAlpha = 1f
                if (isExpanded || animOffsetY.isRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = ((progressForDrawing() - 0.20f) / 0.60f).coerceIn(0f, 1f)
                            }
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
                                    .padding(top = 8.dp, bottom = 2.dp)
                                    .clickable { collapseToMini() },
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
                                        .padding(start = 16.dp)
                                        .pointerInput(track.id) {
                                            var totalDragX = 0f
                                            var velocityTracker = VelocityTracker()
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    velocityTracker = VelocityTracker()
                                                    totalDragX = 0f
                                                },
                                                onDragEnd = {
                                                    val velocity = velocityTracker.calculateVelocity().x
                                                    val thresholdPx = 90f
                                                    val minFlingVelocity = 450f
                                                    if (totalDragX < -thresholdPx || (velocity < -minFlingVelocity && totalDragX < -15f)) {
                                                        onSwipeNextTrack()
                                                    } else if (totalDragX > thresholdPx || (velocity > minFlingVelocity && totalDragX > 15f)) {
                                                        onSwipePreviousTrack()
                                                    }
                                                },
                                                onDragCancel = {},
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    totalDragX += dragAmount
                                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                }
                                            )
                                        },
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                     // Title, Artist & Album with edge-to-edge slide transition
                                    AnimatedContent(
                                        targetState = track,
                                        contentKey = { it.id },
                                        transitionSpec = {
                                            if (transitionDirection >= 0) {
                                                (slideInHorizontally(
                                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                    initialOffsetX = { fullWidth -> fullWidth }
                                                ) + fadeIn(animationSpec = tween(240, delayMillis = 60)))
                                                .togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                        targetOffsetX = { fullWidth -> -fullWidth }
                                                    ) + fadeOut(animationSpec = tween(180))
                                                )
                                            } else {
                                                (slideInHorizontally(
                                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                    initialOffsetX = { fullWidth -> -fullWidth }
                                                ) + fadeIn(animationSpec = tween(240, delayMillis = 60)))
                                                .togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                        targetOffsetX = { fullWidth -> fullWidth }
                                                    ) + fadeOut(animationSpec = tween(180))
                                                )
                                            }
                                        },
                                        label = "landscape_track_title_artist_slide_flip",
                                        modifier = Modifier.fillMaxWidth()
                                    ) { currentT ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = currentT.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentT.artist,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (currentT.album.isNotBlank() && currentT.album != "Неизвестный альбом") {
                                                Text(
                                                    text = currentT.album,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
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
                                        IconButton(onClick = onButtonPreviousTrack, modifier = Modifier.testTag("player_prev_button")) {
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
                                        IconButton(onClick = onSwipeNextTrack, modifier = Modifier.testTag("player_next_button")) {
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

                                // Track Title & Artist & Album with edge-to-edge slide transition and swipe gesture
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .pointerInput(track.id) {
                                            var totalDragX = 0f
                                            var velocityTracker = VelocityTracker()
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    velocityTracker = VelocityTracker()
                                                    totalDragX = 0f
                                                },
                                                onDragEnd = {
                                                    val velocity = velocityTracker.calculateVelocity().x
                                                    val thresholdPx = 90f
                                                    val minFlingVelocity = 450f
                                                    if (totalDragX < -thresholdPx || (velocity < -minFlingVelocity && totalDragX < -15f)) {
                                                        onSwipeNextTrack()
                                                    } else if (totalDragX > thresholdPx || (velocity > minFlingVelocity && totalDragX > 15f)) {
                                                        onSwipePreviousTrack()
                                                    }
                                                },
                                                onDragCancel = {},
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    totalDragX += dragAmount
                                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                }
                                            )
                                        }
                                ) {
                                    AnimatedContent(
                                        targetState = track,
                                        contentKey = { it.id },
                                        transitionSpec = {
                                            if (transitionDirection >= 0) {
                                                (slideInHorizontally(
                                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                    initialOffsetX = { fullWidth -> fullWidth }
                                                ) + fadeIn(animationSpec = tween(240, delayMillis = 60)))
                                                .togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                        targetOffsetX = { fullWidth -> -fullWidth }
                                                    ) + fadeOut(animationSpec = tween(180))
                                                )
                                            } else {
                                                (slideInHorizontally(
                                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                    initialOffsetX = { fullWidth -> -fullWidth }
                                                ) + fadeIn(animationSpec = tween(240, delayMillis = 60)))
                                                .togetherWith(
                                                    slideOutHorizontally(
                                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                                        targetOffsetX = { fullWidth -> fullWidth }
                                                    ) + fadeOut(animationSpec = tween(180))
                                                )
                                            }
                                        },
                                        label = "track_title_artist_slide_flip",
                                        modifier = Modifier.fillMaxWidth()
                                    ) { currentT ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = currentT.title,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = currentT.artist,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (currentT.album.isNotBlank() && currentT.album != "Неизвестный альбом") {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = currentT.album,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
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
                                    FilledTonalIconButton(
                                        onClick = { viewModel.toggleShuffle() },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = if (isShuffle) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (isShuffle) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("player_shuffle_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Перемешать",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(track.id) },
                                        modifier = Modifier.testTag("player_favorite_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentTrackFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isCurrentTrackFavorite) "Удалить из избранного" else "В избранное",
                                            tint = favoriteHeartColor,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = favoriteHeartScale
                                                scaleY = favoriteHeartScale
                                            }
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

                                    FilledTonalIconButton(
                                        onClick = { viewModel.toggleRepeat() },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("player_repeat_button")
                                    ) {
                                        when (repeatMode) {
                                            RepeatMode.OFF -> Icon(Icons.Default.Repeat, contentDescription = "Повтор выключен")
                                            RepeatMode.ALL -> Icon(Icons.Default.Repeat, contentDescription = "Повтор всех")
                                            RepeatMode.ONE -> Icon(Icons.Default.RepeatOne, contentDescription = "Повтор одного")
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            // Collapse the player and reveal the queue in parallel.
                                            // Queue navigation uses a lightweight screen transition.
                                            collapseToMiniFast()
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
                                        onClick = onButtonPreviousTrack,
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
                                            .shadow(7.dp, CircleShape, spotColor = NeonCyan.copy(alpha = 0.24f))
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
                                        onClick = onSwipeNextTrack,
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
        val sleepTimerRemaining by viewModel.sleepTimerRemainingMillis.collectAsStateWithLifecycle()
        SleepTimerDialog(
            currentMode = sleepTimerMode,
            remainingMillis = sleepTimerRemaining,
            onSetTimer = { viewModel.setSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showAddToPlaylistDialog) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
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
private fun MiniPlayerProgressFromFlow(
    positionFlow: StateFlow<Long>,
    duration: Long,
    isPlaying: Boolean,
    accentColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    // Keep frequent playback-position updates scoped to the progress bar rather than
    // recomposing the entire player sheet on every position tick.
    val position by positionFlow.collectAsStateWithLifecycle()
    InteractiveMiniProgressBar(
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        onSeek = {},
        accentColor = accentColor,
        trackColor = trackColor,
        modifier = modifier,
        alpha = alpha,
        interactive = false,
        testTag = "mini_player_progress"
    )
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

        val accentColor = MaterialTheme.colorScheme.primary

        GlowProgressSlider(
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
            accentColor = accentColor,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            trackHeight = 4.dp,
            thumbRadius = 5.dp,
            glowRadius = 14.dp,
            glowAlpha = 0.36f,
            testTag = "player_progress_slider"
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

@Composable
private fun PlayerArtworkCard(
    track: Track,
    size: Dp,
    cornerRadius: Dp,
    isPlaying: Boolean,
    progressTrackColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val simplifiedShadows = rememberShouldUseSimplifiedShadows()
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (simplifiedShadows) 6.dp else 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = if (!simplifiedShadows && isPlaying) NeonCyan.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.18f),
                ambientColor = if (!simplifiedShadows && isPlaying) NeonPurple.copy(alpha = 0.10f) else Color.Transparent
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(progressTrackColor)
    ) {
        val artUri = track.albumArtUri
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
                    // Fade decoded artwork in instead of replacing the placeholder in one frame.
                    .crossfade(180)
                    .placeholder(R.drawable.ic_default_art)
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

@Composable
private fun ExpandedSlidingArtwork(
    currentTrack: Track,
    direction: Int,
    artSize: Dp,
    cornerRadius: Dp,
    isPlaying: Boolean,
    progressTrackColor: Color,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    // Update the layer directly during pointer movement; avoid launching a coroutine per event.
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val artSizePx = with(density) { artSize.toPx() }

    Box(
        modifier = modifier
            .size(artSize)
            .pointerInput(currentTrack.id) {
                var totalDragX = 0f
                var velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker = VelocityTracker()
                        totalDragX = 0f
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        val thresholdPx = artSizePx * 0.20f
                        val minFlingVelocity = 600f
                        coroutineScope.launch {
                            if (totalDragX < -thresholdPx || (velocity < -minFlingVelocity && totalDragX < -20f)) {
                                onSwipeNext()
                            } else if (totalDragX > thresholdPx || (velocity > minFlingVelocity && totalDragX > 20f)) {
                                onSwipePrevious()
                            }
                            Animatable(dragOffsetX).animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { dragOffsetX = value }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            Animatable(dragOffsetX).animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { dragOffsetX = value }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val resistance = (1f - (abs(totalDragX) / (artSizePx * 2.2f)).coerceIn(0f, 0.45f))
                        dragOffsetX = totalDragX * resistance
                    }
                )
            }
            .graphicsLayer {
                translationX = dragOffsetX
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentTrack,
            contentKey = { it.id },
            transitionSpec = {
                if (direction >= 0) {
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -fullWidth }
                        ) + fadeOut(animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeIn(animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> fullWidth }
                        ) + fadeOut(animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))
                    )
                }
            },
            label = "expanded_player_sliding_artwork"
        ) { targetTrack ->
            PlayerArtworkCard(
                track = targetTrack,
                size = artSize,
                cornerRadius = cornerRadius,
                isPlaying = isPlaying,
                progressTrackColor = progressTrackColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
