package com.example.service

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Track
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

@OptIn(UnstableApi::class)
class ReplayGainController(private val context: Context) {

    companion object {
        private const val TAG = "ReplayGainController"
        private const val TARGET_RMS = 0.18f // Reference target ~ -15 dBFS / 89 dB SPL
        private const val MIN_GAIN_DB = -12.0f
        private const val MAX_GAIN_DB = 8.0f
    }

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentAudioSessionId: Int = 0

    var isEnabled: Boolean = true
        private set

    // Gain state for current track
    var hasStaticTag: Boolean = false
        private set
    var staticGainDb: Float = 0f
        private set
    var dynamicGainDb: Float = 0f
        private set

    private var smoothedRms: Float = TARGET_RMS
    private var bufferCount: Int = 0
    private var currentAppliedGainDb: Float = 0f

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        applyCurrentGain()
    }

    fun attachToAudioSession(audioSessionId: Int, enabled: Boolean) {
        releaseEnhancer()
        this.isEnabled = enabled
        if (audioSessionId <= 0) return
        this.currentAudioSessionId = audioSessionId

        try {
            val enhancer = LoudnessEnhancer(audioSessionId)
            enhancer.enabled = enabled
            this.loudnessEnhancer = enhancer
            Log.d(TAG, "LoudnessEnhancer attached to audio session $audioSessionId (enabled=$enabled)")
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer could not be attached: ${e.message}")
            loudnessEnhancer = null
        }
        applyCurrentGain()
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle LoudnessEnhancer: ${e.message}")
        }
        if (enabled) {
            applyCurrentGain()
        } else {
            setUnityGain()
        }
    }

    fun onTrackChanged(track: Track?) {
        hasStaticTag = false
        staticGainDb = 0f
        dynamicGainDb = 0f
        smoothedRms = TARGET_RMS
        bufferCount = 0
        currentAppliedGainDb = 0f

        if (isEnabled) {
            applyCurrentGain()
        } else {
            setUnityGain()
        }
    }

    fun onMetadata(metadata: Metadata) {
        if (!isEnabled || hasStaticTag) return

        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            val parsedGain = extractGainFromEntry(entry)
            if (parsedGain != null) {
                hasStaticTag = true
                staticGainDb = parsedGain
                Log.d(TAG, "Found ReplayGain tag in metadata: $parsedGain dB")
                applyCurrentGain()
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
        // Matches e.g. "-4.50 dB", "+2.3 dB", "-5.2", "=-4.8 dB"
        val regex = Regex("""([+-]?\d+(?:\.\d+)?)\s*(?:dB)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(input) ?: return null
        val valueStr = match.groupValues[1]
        val value = valueStr.toFloatOrNull() ?: return null

        // If tag was R128 in Q7.8 (e.g. -1280), normalize to dB
        return if (value < -50f || value > 50f) {
            value / 256f
        } else {
            value
        }
    }

    fun onAudioBufferRms(rms: Float) {
        if (!isEnabled || hasStaticTag) return
        if (rms < 0.02f) return // Skip silence or quiet intro/outro

        smoothedRms = smoothedRms * 0.95f + rms * 0.05f
        bufferCount++

        // After analyzing initial frames, adjust dynamic gain smoothly
        if (bufferCount >= 15) {
            val ratio = TARGET_RMS / max(0.02f, smoothedRms)
            val targetDb = (20.0 * log10(ratio.toDouble())).toFloat().coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            // Smoothly move dynamicGainDb towards target
            dynamicGainDb = dynamicGainDb * 0.90f + targetDb * 0.10f
            applyCurrentGain()
        }
    }

    private fun applyCurrentGain() {
        if (!isEnabled) {
            setUnityGain()
            return
        }

        val targetGainDb = if (hasStaticTag) {
            staticGainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        } else {
            dynamicGainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }

        currentAppliedGainDb = targetGainDb

        if (targetGainDb < 0f) {
            // Attenuation: use player volume, set loudness boost to 0
            try {
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {}

            val volumeFactor = 10.0.pow(targetGainDb / 20.0).toFloat().coerceIn(0.1f, 1.0f)
            exoPlayer?.volume = volumeFactor
        } else {
            // Amplification: player volume full, use loudness enhancer digital pre-gain
            exoPlayer?.volume = 1.0f

            val boostMb = (targetGainDb * 100f).toInt().coerceIn(0, 800) // 100 mB = 1 dB
            try {
                loudnessEnhancer?.setTargetGain(boostMb)
            } catch (e: Exception) {
                Log.w(TAG, "Error applying targetGain to LoudnessEnhancer: ${e.message}")
            }
        }
    }

    private fun setUnityGain() {
        exoPlayer?.volume = 1.0f
        try {
            loudnessEnhancer?.setTargetGain(0)
        } catch (_: Exception) {}
        currentAppliedGainDb = 0f
    }

    private fun releaseEnhancer() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null
    }

    fun release() {
        setUnityGain()
        releaseEnhancer()
        exoPlayer = null
        currentAudioSessionId = 0
    }
}
