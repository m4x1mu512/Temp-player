package com.example.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

const val KEY_PLAYER_CONTAINER = "player_container"
const val KEY_PLAYER_ALBUM_ART = "player_album_art"
const val KEY_PLAYER_TRACK_TEXT = "player_track_text"

const val PLAYER_EXPAND_DURATION = 260
const val PLAYER_COLLAPSE_DURATION = 240
const val PLAYER_TRANSITION_DURATION = 260

val PlayerEmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
val PlayerEmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
val PlayerEmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

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
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = { _, _ ->
                tween(durationMillis = PLAYER_TRANSITION_DURATION, easing = PlayerEmphasizedEasing)
            },
            clipInOverlayDuringTransition = clipShape?.let { OverlayClip(it) }
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
        sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = { _, _ ->
                tween(durationMillis = PLAYER_EXPAND_DURATION, easing = PlayerEmphasizedDecelerateEasing)
            },
            clipInOverlayDuringTransition = clipShape?.let { OverlayClip(it) }
        )
    }
}
