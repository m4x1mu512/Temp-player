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