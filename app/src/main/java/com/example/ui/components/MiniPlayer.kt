package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.MiniPlayerBgMode
import com.example.data.model.Track
import com.example.ui.theme.FavoriteRed
import com.example.ui.util.KEY_PLAYER_ALBUM_ART
import com.example.ui.util.KEY_PLAYER_CONTAINER
import com.example.ui.util.KEY_PLAYER_TRACK_TEXT
import com.example.ui.util.formatTime
import com.example.ui.util.playerSharedBounds
import com.example.ui.util.playerSharedElement
import com.example.ui.util.rememberMiniPlayerColors

@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
    duration: Long,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    bgMode: MiniPlayerBgMode = MiniPlayerBgMode.ALBUM_ART,
    customColor: Long = 0L,
    autoRotate: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val position by positionFlow.collectAsStateWithLifecycle()
    MiniPlayer(
        currentTrack = currentTrack,
        isPlaying = isPlaying,
        position = position,
        duration = duration,
        onTogglePlayPause = onTogglePlayPause,
        onNextTrack = onNextTrack,
        onPreviousTrack = onPreviousTrack,
        onClick = onClick,
        modifier = modifier,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        bgMode = bgMode,
        customColor = customColor,
        autoRotate = autoRotate,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    bgMode: MiniPlayerBgMode = MiniPlayerBgMode.ALBUM_ART,
    customColor: Long = 0L,
    autoRotate: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        if (currentTrack == null) return@AnimatedVisibility

        val configuration = LocalConfiguration.current
        val isLandscape = autoRotate && (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        val colorScheme = rememberMiniPlayerColors(
            bgMode = bgMode,
            albumArtUri = currentTrack.albumArtUri,
            customColorLong = customColor
        )

        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (colorScheme.isGradient) Color.Transparent else colorScheme.backgroundColor
            ),
            border = if (!colorScheme.isDark) BorderStroke(1.dp, Color(0x18000000)) else BorderStroke(1.dp, Color(0x1AFFFFFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = modifier
                .fillMaxWidth()
                .playerSharedBounds(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    key = KEY_PLAYER_CONTAINER,
                    clipShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .testTag("mini_player")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (colorScheme.isGradient && colorScheme.miniPlayerBrush != null) {
                            Modifier.background(colorScheme.miniPlayerBrush)
                        } else {
                            Modifier.background(colorScheme.backgroundColor)
                        }
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                // Deferred alpha read: does NOT trigger recomposition of MiniPlayer
                val miniControlsAlphaState: State<Float>? = if (animatedVisibilityScope != null) {
                    animatedVisibilityScope.transition.animateFloat(
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) {
                                tween(durationMillis = 200, delayMillis = 30, easing = FastOutSlowInEasing)
                            } else {
                                tween(durationMillis = 120, easing = FastOutSlowInEasing)
                            }
                        },
                        label = "mini_controls_alpha"
                    ) { state ->
                        if (state == EnterExitState.Visible) 1f else 0f
                    }
                } else null
                val getMiniControlsAlpha: () -> Float = { miniControlsAlphaState?.value ?: 1f }

                // Smooth continuous progress line without 250ms jerkiness
                val targetProgress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = if (isPlaying) tween(durationMillis = 250, easing = LinearEasing) else snap(),
                    label = "mini_player_animated_progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .graphicsLayer { alpha = getMiniControlsAlpha() }
                        .testTag("mini_player_progress"),
                    color = colorScheme.accentColor,
                    trackColor = colorScheme.progressTrackColor
                )

                val context = LocalContext.current

                if (isLandscape) {
                    // Landscape horizontal orientation layout for MiniPlayer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        // Clickable info section: Album art + Title/Artist opens full player
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onClick() }
                        ) {
                            // Album art
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .playerSharedElement(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        key = KEY_PLAYER_ALBUM_ART,
                                        clipShape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.progressTrackColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val miniLandscapeArtRequest = remember(currentTrack.albumArtUri) {
                                    ImageRequest.Builder(context)
                                        .data(currentTrack.albumArtUri ?: R.drawable.ic_default_art)
                                        .size(160, 160)
                                        .crossfade(150)
                                        .placeholder(R.drawable.ic_default_art)
                                        .error(R.drawable.ic_default_art)
                                        .build()
                                }
                                AsyncImage(
                                    model = miniLandscapeArtRequest,
                                    contentDescription = "Обложка трека",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Artist with Album
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .playerSharedBounds(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        key = KEY_PLAYER_TRACK_TEXT
                                    ),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (currentTrack.album.isNotBlank() && currentTrack.album != "Неизвестный альбом") {
                                        Text(
                                            text = " • ${currentTrack.album}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariantColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Time badge in landscape
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.onSurfaceColor.copy(alpha = 0.08f),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .graphicsLayer { alpha = getMiniControlsAlpha() }
                                .clickable { onClick() }
                        ) {
                            Text(
                                text = "${formatTime(position)} / ${formatTime(duration)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariantColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Favorite button if available
                        if (onToggleFavorite != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer { alpha = getMiniControlsAlpha() }
                                    .testTag("mini_player_favorite")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Удалить из избранного" else "В избранное",
                                    tint = if (isFavorite) FavoriteRed else colorScheme.onSurfaceVariantColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Playback Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.graphicsLayer { alpha = getMiniControlsAlpha() }
                        ) {
                            IconButton(
                                onClick = onPreviousTrack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("mini_player_previous")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Предыдущий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colorScheme.accentColor,
                                    contentColor = colorScheme.onAccentColor
                                ),
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("mini_player_play_pause")
                            ) {
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = {
                                        (scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeIn(tween(140)))
                                            .togetherWith(scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeOut(tween(100)))
                                    },
                                    label = "mini_land_play_pause"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playing) "Пауза" else "Воспроизведение",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onNextTrack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("mini_player_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Следующий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Portrait orientation layout: Made taller and comfortable for easy interaction
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Clickable info section: Album art + Title/Artist opens full player
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onClick() }
                        ) {
                            // Album art (taller: 56.dp)
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .playerSharedElement(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        key = KEY_PLAYER_ALBUM_ART,
                                        clipShape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.progressTrackColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val miniPortraitArtRequest = remember(currentTrack.albumArtUri) {
                                    ImageRequest.Builder(context)
                                        .data(currentTrack.albumArtUri ?: R.drawable.ic_default_art)
                                        .size(160, 160)
                                        .crossfade(150)
                                        .placeholder(R.drawable.ic_default_art)
                                        .error(R.drawable.ic_default_art)
                                        .build()
                                }
                                AsyncImage(
                                    model = miniPortraitArtRequest,
                                    contentDescription = "Обложка трека",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Artist
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .playerSharedBounds(
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        key = KEY_PLAYER_TRACK_TEXT
                                    ),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurfaceColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTrack.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariantColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Control buttons: Previous, Play/Pause, Next
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.graphicsLayer { alpha = getMiniControlsAlpha() }
                        ) {
                            // Previous track button
                            IconButton(
                                onClick = onPreviousTrack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("mini_player_previous")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Предыдущий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Play / Pause button with primary colored filled icon button
                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colorScheme.accentColor,
                                    contentColor = colorScheme.onAccentColor
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("mini_player_play_pause")
                            ) {
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = {
                                        (scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeIn(tween(140)))
                                            .togetherWith(scaleOut(spring(dampingRatio = 0.6f, stiffness = 400f)) + fadeOut(tween(100)))
                                    },
                                    label = "mini_port_play_pause"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playing) "Пауза" else "Воспроизведение",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Next track button
                            IconButton(
                                onClick = onNextTrack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("mini_player_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Следующий трек",
                                    tint = colorScheme.onSurfaceColor,
                                    modifier = Modifier.size(26.dp)
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

