package com.example.service.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI

/**
 * Rhythm Bass Boost Processor - Real-time bass enhancement.
 *
 * Modeled directly after cromaguy/Rhythm:
 * Uses a single-pole IIR low-pass filter (cutoff 150Hz) with adaptive logarithmic gain (1.0x to 4.0x)
 * and soft-knee limiter to prevent digital clipping without touching the hardware DSP.
 *
 * Bypasses buffer processing cleanly when disabled or at strength 0, eliminating
 * volume surges, pops, clicks, and DRC release delays.
 */
@OptIn(UnstableApi::class)
class RhythmBassBoostProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "RhythmBassBoost"
        private const val BASS_CUTOFF_FREQ = 150.0 // Hz
    }

    // Parent processor for dynamic configuration sharing (crossfade / dual player thread safety)
    @Volatile
    private var parentProcessor: RhythmBassBoostProcessor? = null

    // Bass boost strength (0-1000)
    @Volatile
    private var strength: Short = 0

    @Volatile
    private var enabled: Boolean = false

    private var prevSample = FloatArray(2)
    private var filterCoeff = 0f
    private var filterCoeffValid = false

    fun setParent(parent: RhythmBassBoostProcessor?) {
        this.parentProcessor = parent
    }

    fun setEnabled(enable: Boolean) {
        this.enabled = enable
        if (!enable) {
            prevSample.fill(0f)
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val outputFormat = super.configure(inputAudioFormat)
        filterCoeffValid = false
        if (prevSample.size != inputAudioFormat.channelCount) {
            prevSample = FloatArray(inputAudioFormat.channelCount)
        } else {
            prevSample.fill(0f)
        }
        return outputFormat
    }

    override fun flush() {
        super.flush()
        prevSample.fill(0f)
    }

    fun setStrength(strength: Short) {
        this.strength = strength.coerceIn(0, 1000)
        updateFilterCoeff()
    }

    fun getStrength(): Short = parentProcessor?.getStrength() ?: strength

    override fun isEnabled(): Boolean = parentProcessor?.isEnabled() ?: enabled

    override fun isBypassed(): Boolean {
        return !isEnabled() || getStrength() == 0.toShort()
    }

    private fun updateFilterCoeff() {
        if (sampleRate <= 0) {
            filterCoeffValid = false
            return
        }
        val rc = 1.0 / (2.0 * PI * BASS_CUTOFF_FREQ)
        val dt = 1.0 / sampleRate
        filterCoeff = (dt / (rc + dt)).toFloat()
        filterCoeffValid = true
    }

    /**
     * Soft-knee limiter using saturating math to prevent hard clipping distortion.
     * Transitions smoothly from linear to compression without harsh aliasing.
     */
    private fun softLimitSample(sample: Float): Float {
        if (sample == 0f) return 0f
        val absValue = kotlin.math.abs(sample)
        val threshold = 0.8f
        if (absValue <= threshold) {
            return sample
        }
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        return kotlin.math.sign(sample) * limited
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val currentEnabled = isEnabled()
        val currentStrength = getStrength()
        if (!currentEnabled || currentStrength == 0.toShort()) {
            return
        }

        if (filterCoeff == 0f || !filterCoeffValid) {
            updateFilterCoeff()
        }

        val gain = when {
            currentStrength == 0.toShort() -> 1.0f
            currentStrength <= 100 -> 1.0f + (currentStrength / 100.0f) * 0.3f
            currentStrength <= 500 -> 1.3f + ((currentStrength - 100) / 400.0f) * 0.9f
            else -> 2.2f + ((currentStrength - 500) / 500.0f) * 1.8f
        }

        val activeChannels = if (channelCount > 0) channelCount else 1

        for (i in 0 until sampleCount) {
            val channelIdx = i % activeChannels
            val input = samples[i] / 32768.0f
            val lowPass = prevSample.getOrElse(channelIdx) { 0f } + filterCoeff * (input - prevSample.getOrElse(channelIdx) { 0f })
            if (channelIdx < prevSample.size) {
                prevSample[channelIdx] = lowPass
            }

            val bassBoost = lowPass * (gain - 1.0f)
            val output = input + bassBoost
            val limited = softLimitSample(output)
            samples[i] = (limited * 32767.0f).toInt().toShort()
        }
    }
}
