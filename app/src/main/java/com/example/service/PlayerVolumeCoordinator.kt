package com.example.service

import androidx.media3.exoplayer.ExoPlayer
import java.util.IdentityHashMap

/**
 * The only owner of ExoPlayer.volume.
 *
 * Independent audio features publish their own gain factor; the final output is their
 * product, so ReplayGain, crossfade and temporary EQ transitions cannot overwrite each other.
 * All calls are made on the playback (main) thread, matching ExoPlayer's application looper.
 */
class PlayerVolumeCoordinator {
    private data class Gains(
        var master: Float = 1f,
        var replayGain: Float = 1f,
        var crossfade: Float = 1f,
        var transition: Float = 1f
    )

    private val gainsByPlayer = IdentityHashMap<ExoPlayer, Gains>()

    fun attach(player: ExoPlayer) {
        apply(player, gainsByPlayer.getOrPut(player) { Gains() })
    }

    fun setMasterGain(player: ExoPlayer, gain: Float) =
        update(player) { master = gain.sanitizedGain() }

    fun setReplayGain(player: ExoPlayer, gain: Float) =
        update(player) { replayGain = gain.sanitizedGain() }

    fun setCrossfadeGain(player: ExoPlayer, gain: Float) =
        update(player) { crossfade = gain.sanitizedGain() }

    fun setTransitionGain(player: ExoPlayer, gain: Float) =
        update(player) { transition = gain.sanitizedGain() }

    fun getReplayGain(player: ExoPlayer): Float =
        gainsByPlayer[player]?.replayGain ?: 1f

    fun release(player: ExoPlayer) {
        gainsByPlayer.remove(player)
    }

    private inline fun update(player: ExoPlayer, change: Gains.() -> Unit) {
        val gains = gainsByPlayer.getOrPut(player) { Gains() }
        gains.change()
        apply(player, gains)
    }

    private fun apply(player: ExoPlayer, gains: Gains) {
        player.volume = calculateOutputVolume(
            gains.master,
            gains.replayGain,
            gains.crossfade,
            gains.transition
        )
    }

    internal companion object {
        fun calculateOutputVolume(
            master: Float,
            replayGain: Float,
            crossfade: Float,
            transition: Float
        ): Float = master.sanitizedGain() *
            replayGain.sanitizedGain() *
            crossfade.sanitizedGain() *
            transition.sanitizedGain()

        private fun Float.sanitizedGain(): Float =
            if (isFinite()) coerceIn(0f, 1f) else 1f
    }
}
