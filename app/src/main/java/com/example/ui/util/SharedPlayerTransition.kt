package com.example.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

const val KEY_PLAYER_CONTAINER = "player_container"
const val KEY_PLAYER_ALBUM_ART = "player_album_art"
const val KEY_PLAYER_TRACK_TEXT = "player_track_text"

const val PLAYER_EXPAND_DURATION = 260
const val PLAYER_COLLAPSE_DURATION = 200
const val PLAYER_TRANSITION_DURATION = 250

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playerSharedBounds(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    key: String,
    clipShape: Shape? = null
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return this
    return with(sharedTransitionScope) {
        this@playerSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            clipInOverlayDuringTransition = clipShape?.let { OverlayClip(it) } ?: OverlayClip(RectangleShape),
            zIndexInOverlay = 5f,
            boundsTransform = { initialBounds, targetBounds ->
                val isExpanding = (targetBounds.width * targetBounds.height) >= (initialBounds.width * initialBounds.height)
                val duration = if (isExpanding) PLAYER_EXPAND_DURATION else PLAYER_COLLAPSE_DURATION
                tween(durationMillis = duration, easing = FastOutSlowInEasing)
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.playerSharedElement(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    key: String,
    clipShape: Shape? = null
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return this
    return with(sharedTransitionScope) {
        this@playerSharedElement.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            clipInOverlayDuringTransition = clipShape?.let { OverlayClip(it) } ?: OverlayClip(RectangleShape),
            zIndexInOverlay = 6f,
            boundsTransform = { initialBounds, targetBounds ->
                val isExpanding = (targetBounds.width * targetBounds.height) >= (initialBounds.width * initialBounds.height)
                val duration = if (isExpanding) PLAYER_EXPAND_DURATION else PLAYER_COLLAPSE_DURATION
                tween(durationMillis = duration, easing = FastOutSlowInEasing)
            }
        )
    }
}
