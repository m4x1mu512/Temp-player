package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Track
import com.example.ui.theme.FavoriteRed
import com.example.ui.util.formatTime

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
    onToggleFavorite: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        if (currentTrack == null) return@AnimatedVisibility

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("mini_player")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Progress line
                val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("mini_player_progress"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack.albumArtUri != null) {
                                    AsyncImage(
                                        model = currentTrack.albumArtUri,
                                        contentDescription = "Обложка трека",
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.ic_default_art),
                                        placeholder = painterResource(id = R.drawable.ic_default_art),
                                        modifier = Modifier.size(52.dp)
                                    )
                                } else {
                                    AsyncImage(
                                        model = R.drawable.ic_default_art,
                                        contentDescription = "Обложка трека",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Artist with Album
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (currentTrack.album.isNotBlank() && currentTrack.album != "Неизвестный альбом") {
                                        Text(
                                            text = " • ${currentTrack.album}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { onClick() }
                        ) {
                            Text(
                                text = "${formatTime(position)} / ${formatTime(duration)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Favorite button if available
                        if (onToggleFavorite != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("mini_player_favorite")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Удалить из избранного" else "В избранное",
                                    tint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Playback Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("mini_player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                    modifier = Modifier.size(26.dp)
                                )
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
                                    tint = MaterialTheme.colorScheme.onSurface,
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack.albumArtUri != null) {
                                    AsyncImage(
                                        model = currentTrack.albumArtUri,
                                        contentDescription = "Обложка трека",
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.ic_default_art),
                                        placeholder = painterResource(id = R.drawable.ic_default_art),
                                        modifier = Modifier.size(56.dp)
                                    )
                                } else {
                                    AsyncImage(
                                        model = R.drawable.ic_default_art,
                                        contentDescription = "Обложка трека",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Artist
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTrack.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Control buttons: Previous, Play/Pause, Next
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Play / Pause button with primary colored filled icon button
                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("mini_player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                                    modifier = Modifier.size(28.dp)
                                )
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
                                    tint = MaterialTheme.colorScheme.onSurface,
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

