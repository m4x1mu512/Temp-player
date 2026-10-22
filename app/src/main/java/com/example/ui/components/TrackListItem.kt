package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.ui.theme.FavoriteRed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.Track

@Composable
fun TrackListItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = track.isFavorite,
    isDragging: Boolean = false,
    isHighlighted: Boolean = false,
    showReorderHandle: Boolean = false,
    reorderModifier: Modifier = Modifier,
    onEditTrackInfo: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val targetBgColor = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
        else -> Color.Transparent
    }

    val itemBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 350),
        label = "track_item_bg_${track.id}"
    )

    val itemShape = RoundedCornerShape(if (isDragging) 18.dp else 16.dp)
    val itemBorderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(itemShape)
            .clickable { onClick() }
            .background(itemBgColor)
            .border(
                width = 1.dp,
                color = itemBorderColor,
                shape = itemShape
            )
            .padding(start = 10.dp, end = 4.dp, top = 7.dp, bottom = 7.dp)
            .testTag("track_item_${track.id}")
    ) {
        // Thumbnail with playing overlay
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val thumbnailRequest = remember(track.albumArtUri) {
                ImageRequest.Builder(context)
                    .data(track.albumArtUri ?: R.drawable.ic_default_art)
                    .size(150, 150)
                    .crossfade(false)
                    .error(R.drawable.ic_default_art)
                    .build()
            }

            AsyncImage(
                model = thumbnailRequest,
                contentDescription = "Обложка ${track.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp)
            )

            if (isCurrent) {
                val playingOverlayAlpha by animateFloatAsState(
                    targetValue = if (isPlaying) 0.46f else 0.28f,
                    animationSpec = tween(durationMillis = 260),
                    label = "playing_overlay_alpha_${track.id}"
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = playingOverlayAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    val eqScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.05f else 0.95f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "eq_scale_${track.id}"
                    )
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Сейчас играет",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = eqScale
                                scaleY = eqScale
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.formattedDuration()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action buttons grouped closely together near the right edge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(end = 0.dp)
        ) {
            // Favorite Button
            val heartScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.25f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heartScale_${track.id}"
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() }
                    .testTag("favorite_button_${track.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Удалить из избранного" else "В избранное",
                    tint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        }
                )
            }

            // More options dropdown
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true }
                        .testTag("more_button_${track.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции трека",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onEditTrackInfo != null) {
                        DropdownMenuItem(
                            text = { Text("Редактировать инфо") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditTrackInfo()
                            },
                            modifier = Modifier.testTag("menu_edit_info_${track.id}")
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Добавить в плейлист") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Удалить из избранного" else "В избранное") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                }
            }

            // Drag Handle for moving track
            if (showReorderHandle) {
                Box(
                    modifier = reorderModifier
                        .size(32.dp)
                        .testTag("reorder_handle_${track.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Переместить трек",
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
