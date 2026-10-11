package com.example.service.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

/**
 * Rhythm Spatialization Processor - Real-time 3D audio enhancement / Virtualizer.
 *
 * Modeled directly after cromaguy/Rhythm:
 * Uses mid-side (M/S) stereo widening with soft clipping:
 * - Mid (M) = (L + R) / 2
 * - Side (S) = (L - R) / 2
 * - Enhanced L = Mid + Side * width
 * - Enhanced R = Mid - Side * width
 *
 * When disabled or bypassed, passes audio directly with zero volume surge, zero pop, and zero delay.
 */
@OptIn(UnstableApi::class)
class RhythmSpatializationProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "RhythmSpatialization"
    }

    // Parent processor for dynamic configuration sharing (crossfade / dual player thread safety)
    @Volatile
    private var parentProcessor: RhythmSpatializationProcessor? = null

    // Spatialization strength (0-1000)
    @Volatile
    private var strength: Short = 0

    @Volatile
    private var enabled: Boolean = false

    fun setParent(parent: RhythmSpatializationProcessor?) {
        this.parentProcessor = parent
    }

    fun setEnabled(enable: Boolean) {
        this.enabled = enable
    }

    fun setStrength(strength: Short) {
        this.strength = strength.coerceIn(0, 1000)
    }

    fun getStrength(): Short = parentProcessor?.getStrength() ?: strength

    override fun isEnabled(): Boolean = parentProcessor?.isEnabled() ?: enabled

    override fun isBypassed(): Boolean {
        return !isEnabled() || getStrength() == 0.toShort() || channelCount != 2
    }

    fun isAvailable(): Boolean = channelCount == 2

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val currentEnabled = isEnabled()
        val currentStrength = getStrength()
        if (!currentEnabled || currentStrength == 0.toShort() || channelCount != 2) {
            return
        }

        val width = when {
            currentStrength == 0.toShort() -> 1.0f
            currentStrength <= 300 -> 1.0f + (currentStrength / 300.0f) * 0.5f
            currentStrength <= 700 -> 1.5f + ((currentStrength - 300) / 400.0f) * 0.8f
            else -> 2.3f + ((currentStrength - 700) / 300.0f) * 0.7f
        }

        for (i in 0 until sampleCount - 1 step 2) {
            val left = samples[i] / 32768.0f
            val right = samples[i + 1] / 32768.0f

            val mid = (left + right) * 0.5f
            val side = (left - right) * 0.5f

            val wideSide = side * width

            val newLeft = mid + wideSide
            val newRight = mid - wideSide

            val clippedLeft = softClip(newLeft)
            val clippedRight = softClip(newRight)

            samples[i] = (clippedLeft * 32767.0f).toInt().toShort()
            samples[i + 1] = (clippedRight * 32767.0f).toInt().toShort()
        }
    }

    private fun softClip(x: Float): Float {
        if (x == 0f) return 0f
        val absValue = kotlin.math.abs(x)
        val threshold = 0.8f
        if (absValue <= threshold) {
            return x
        }
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        return kotlin.math.sign(x) * limited
    }
}
