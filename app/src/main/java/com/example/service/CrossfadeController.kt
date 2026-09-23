package com.example.service

import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Controller responsible for managing crossfade transitions between two audio players.
 * Uses an equal-power crossfade curve (cos / sin) to prevent perceived dips in loudness
 * while seamlessly blending the end of one track into the beginning of the next.
 */
class CrossfadeController(
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "CrossfadeController"
        const val DEFAULT_DURATION_SECONDS = 4
        const val MIN_DURATION_SECONDS = 1
        const val MAX_DURATION_SECONDS = 10
        private const val STEP_INTERVAL_MS = 40L
    }

    var isEnabled: Boolean = true
    var durationSeconds: Int = DEFAULT_DURATION_SECONDS
        set(value) {
            field = value.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        }

    val durationMs: Long
        get() = durationSeconds * 1000L

    var isCrossfading: Boolean = false
        private set

    private var crossfadeJob: Job? = null
    private var fadingOutPlayer: ExoPlayer? = null
    private var fadingInPlayer: ExoPlayer? = null

    /**
     * Determines whether crossfade should trigger for current track and position.
     */
    fun shouldTriggerCrossfade(
        currentPositionMs: Long,
        totalDurationMs: Long,
        hasNextTrack: Boolean
    ): Boolean {
        if (!isEnabled || isCrossfading || !hasNextTrack) return false
        // Track must be comfortably longer than the crossfade window
        if (totalDurationMs < durationMs + 2000L) return false

        val remainingMs = totalDurationMs - currentPositionMs
        return remainingMs in 1..durationMs
    }

    /**
     * Executes the crossfade transition between two players.
     * Uses equal-power volume curves: cos(progress * PI / 2) for fade-out, sin(progress * PI / 2) for fade-in.
     */
    fun startCrossfade(
        outgoingPlayer: ExoPlayer,
        incomingPlayer: ExoPlayer,
        onCrossfadeComplete: () -> Unit
    ) {
        cancelCrossfade()
        isCrossfading = true
        fadingOutPlayer = outgoingPlayer
        fadingInPlayer = incomingPlayer

        // Incoming player starts at silence
        incomingPlayer.volume = 0f

        val totalMs = durationMs
        val totalSteps = (totalMs / STEP_INTERVAL_MS).toInt().coerceAtLeast(10)

        crossfadeJob = coroutineScope.launch {
            try {
                for (step in 1..totalSteps) {
                    delay(STEP_INTERVAL_MS)
                    if (!isActive || !isCrossfading) break

                    val progress = step.toFloat() / totalSteps
                    // Equal-power volume curve
                    val outVol = cos(progress * (PI / 2.0)).toFloat().coerceIn(0f, 1f)
                    val inVol = sin(progress * (PI / 2.0)).toFloat().coerceIn(0f, 1f)

                    outgoingPlayer.volume = outVol
                    incomingPlayer.volume = inVol
                }
            } catch (e: Exception) {
                Log.d(TAG, "Crossfade interrupted: ${e.message}")
            } finally {
                // Ensure players end in valid volume states
                try {
                    outgoingPlayer.volume = 1f
                    outgoingPlayer.stop()
                } catch (_: Exception) {}
                try {
                    incomingPlayer.volume = 1f
                } catch (_: Exception) {}

                fadingOutPlayer = null
                fadingInPlayer = null
                isCrossfading = false
                onCrossfadeComplete()
            }
        }
    }

    /**
     * Cancels any active crossfade immediately and restores normal volumes.
     */
    fun cancelCrossfade() {
        if (!isCrossfading && crossfadeJob == null) return
        crossfadeJob?.cancel()
        crossfadeJob = null
        try {
            fadingOutPlayer?.volume = 1f
            fadingOutPlayer?.stop()
        } catch (_: Exception) {}
        try {
            fadingInPlayer?.volume = 1f
        } catch (_: Exception) {}
        fadingOutPlayer = null
        fadingInPlayer = null
        isCrossfading = false
    }

    fun release() {
        cancelCrossfade()
    }
}
