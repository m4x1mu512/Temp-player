package com.example.service

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Track
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * High-fidelity ReplayGain controller modeled after Rhythm and professional audio players.
 *
 * Key guarantees:
 * - Pure attenuation / scaling on ExoPlayer volume to prevent hardware DSP conflicts.
 * - Auto-locking dynamic analysis: calculates gain once at track start and freezes it,
 *   preventing annoying volume pumping/surges during breakdowns or quiet intros.
 * - Smooth volume interpolation to eliminate clicks, pops, and sudden jumps.
 * - Does not fight with user-configured Equalizer, Preamp, or Loudness Enhancer.
 */
@OptIn(UnstableApi::class)
class ReplayGainController(private val context: Context) {

    companion object {
        private const val TAG = "ReplayGainController"
        private const val TARGET_RMS = 0.18f // Reference target ~ -15 dBFS / 89 dB SPL
        private const val MIN_GAIN_DB = -12.0f
        private const val MAX_GAIN_DB = 0.0f // Pure attenuation: never amplify beyond unity to prevent digital clipping!
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var exoPlayer: ExoPlayer? = null
    private var volumeAnimator: ValueAnimator? = null

    var isEnabled: Boolean = true
        private set

    // Gain state for current track
    var hasStaticTag: Boolean = false
        private set
    var staticGainDb: Float = 0f
        private set
    var dynamicGainDb: Float = 0f
        private set
    private var isGainLocked: Boolean = false

    private var smoothedRms: Float = TARGET_RMS
    private var bufferCount: Int = 0
    private var currentAppliedGainDb: Float = 0f

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        applyCurrentGain(smooth = false)
    }

    fun attachToAudioSession(audioSessionId: Int, enabled: Boolean) {
        this.isEnabled = enabled
        // No hardware DSP effect attached here: ReplayGain cleanly adjusts player volume
        applyCurrentGain(smooth = true)
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        if (enabled) {
            applyCurrentGain(smooth = true)
        } else {
            setUnityGain(smooth = true)
        }
    }

    fun onTrackChanged(track: Track?) {
        volumeAnimator?.cancel()
        hasStaticTag = false
        staticGainDb = 0f
        dynamicGainDb = 0f
        smoothedRms = TARGET_RMS
        bufferCount = 0
        isGainLocked = false
        currentAppliedGainDb = 0f

        if (isEnabled) {
            applyCurrentGain(smooth = false)
        } else {
            setUnityGain(smooth = false)
        }
    }

    fun onMetadata(metadata: Metadata) {
        if (!isEnabled || hasStaticTag) return

        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            val parsedGain = extractGainFromEntry(entry)
            if (parsedGain != null) {
                hasStaticTag = true
                staticGainDb = parsedGain.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
                isGainLocked = true
                Log.d(TAG, "Found ReplayGain metadata tag: $staticGainDb dB")
                applyCurrentGain(smooth = true)
                return
            }
        }
    }

    private fun extractGainFromEntry(entry: Metadata.Entry): Float? {
        val str = entry.toString()
        if (str.contains("REPLAYGAIN_TRACK_GAIN", ignoreCase = true) ||
            str.contains("replaygain_track_gain", ignoreCase = true) ||
            str.contains("R128_TRACK_GAIN", ignoreCase = true)
        ) {
            return parseGainString(str)
        }
        return null
    }

    private fun parseGainString(input: String): Float? {
        val regex = Regex("""([+-]?\d+(?:\.\d+)?)\s*(?:dB)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(input) ?: return null
        val valueStr = match.groupValues[1]
        val value = valueStr.toFloatOrNull() ?: return null

        return if (value < -50f || value > 50f) {
            value / 256f
        } else {
            value
        }
    }

    fun onAudioBufferRms(rms: Float) {
        try {
            // Once locked or if tagged with metadata, never pump volume while track is playing!
            if (!isEnabled || hasStaticTag || isGainLocked) return
            if (rms < 0.02f) return // Skip silence or intro

            smoothedRms = smoothedRms * 0.90f + rms * 0.10f
            bufferCount++

            // Settle within the first ~25-30 frames (0.5s), then permanently lock for this track
            if (bufferCount >= 25) {
                val ratio = TARGET_RMS / max(0.02f, smoothedRms)
                val targetDb = (20.0 * log10(ratio.toDouble())).toFloat().coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
                dynamicGainDb = targetDb
                isGainLocked = true // Lock to prevent volume pumping during song drops!
                applyCurrentGain(smooth = true)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error calculating dynamic gain: ${t.message}")
        }
    }

    fun applyCurrentGain(smooth: Boolean = true) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            applyCurrentGainInternal(smooth)
        } else {
            mainHandler.post {
                applyCurrentGainInternal(smooth)
            }
        }
    }

    private fun applyCurrentGainInternal(smooth: Boolean) {
        if (!isEnabled) {
            setUnityGainInternal(smooth)
            return
        }

        try {
            val targetGainDb = if (hasStaticTag) {
                staticGainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            } else {
                dynamicGainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            }

            currentAppliedGainDb = targetGainDb
            val targetVolume = 10.0.pow(targetGainDb / 20.0).toFloat().coerceIn(0.15f, 1.0f)
            setPlayerVolume(targetVolume, smooth)
        } catch (e: Exception) {
            Log.w(TAG, "Error applying current gain: ${e.message}")
        }
    }

    private fun setUnityGain(smooth: Boolean = true) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setUnityGainInternal(smooth)
        } else {
            mainHandler.post {
                setUnityGainInternal(smooth)
            }
        }
    }

    private fun setUnityGainInternal(smooth: Boolean) {
        currentAppliedGainDb = 0f
        setPlayerVolume(1.0f, smooth)
    }

    private fun setPlayerVolume(targetVolume: Float, smooth: Boolean) {
        val player = exoPlayer ?: return
        val currentVolume = player.volume

        if (!smooth || kotlin.math.abs(currentVolume - targetVolume) < 0.01f) {
            volumeAnimator?.cancel()
            player.volume = targetVolume
            return
        }

        // Smoothly ramp volume over 80ms to avoid any audio click / pop
        volumeAnimator?.cancel()
        volumeAnimator = ValueAnimator.ofFloat(currentVolume, targetVolume).apply {
            duration = 80L
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                try {
                    player.volume = anim.animatedValue as Float
                } catch (_: Exception) {}
            }
            start()
        }
    }

    fun release() {
        volumeAnimator?.cancel()
        volumeAnimator = null
        exoPlayer = null
    }
}
