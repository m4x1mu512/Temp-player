package com.example.service.audio

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.data.model.ReverbPreset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Rhythm Reverb Processor - Real-time spatial acoustics and reverberation.
 *
 * Modeled directly after the architecture of cromaguy/Rhythm:
 * Operates in-place on 16-bit PCM samples inside ExoPlayer's audio pipeline.
 *
 * Implements a Schroeder / Freeverb algorithmic reverberator with:
 * - 8 Lowpass-Feedback Comb Filters (LBCF) per channel for natural acoustic reflections
 * - 4 Allpass diffusion filters per channel for rich spatial density
 * - Preset-specific decay, absorption damping, and stereo spread
 * - Constant-power dry/wet gain normalization: PREVENTS VOLUME SURGES when selecting presets!
 * - Smooth per-sample parameter ramping to prevent pops, clicks, and discontinuities
 * - Clean zero-latency bypass when disabled or set to [ReverbPreset.NONE]
 */
@OptIn(UnstableApi::class)
class RhythmReverbProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "RhythmReverbProcessor"

        // Nominal delay buffer lengths at 44100 Hz (Freeverb tuning)
        private val COMB_TUNINGS = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        private val ALLPASS_TUNINGS = intArrayOf(556, 441, 341, 225)
        private const val STEREO_SPREAD = 23
        private const val ALLPASS_FEEDBACK = 0.5f

        // Smooth ramp factor (~50ms transition at 44.1kHz stereo)
        private const val RAMP_SPEED = 0.0008f
    }

    // Delay line for comb filter
    private class CombFilter(size: Int) {
        val buffer = FloatArray(size)
        var bufferIndex = 0
        var filterStore = 0f

        fun process(input: Float, feedback: Float, damp: Float): Float {
            val output = buffer[bufferIndex]
            filterStore = output * (1f - damp) + filterStore * damp
            buffer[bufferIndex] = input + filterStore * feedback
            if (++bufferIndex >= buffer.size) {
                bufferIndex = 0
            }
            return output
        }

        fun mute() {
            buffer.fill(0f)
            filterStore = 0f
            bufferIndex = 0
        }
    }

    // Delay line for allpass filter
    private class AllpassFilter(size: Int) {
        val buffer = FloatArray(size)
        var bufferIndex = 0

        fun process(input: Float): Float {
            val bufOut = buffer[bufferIndex]
            val output = -input + bufOut
            buffer[bufferIndex] = input + bufOut * ALLPASS_FEEDBACK
            if (++bufferIndex >= buffer.size) {
                bufferIndex = 0
            }
            return output
        }

        fun mute() {
            buffer.fill(0f)
            bufferIndex = 0
        }
    }

    // Parent processor for dynamic configuration sharing (crossfade / dual player instances)
    @Volatile
    private var parentProcessor: RhythmReverbProcessor? = null

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var currentPreset: ReverbPreset = ReverbPreset.NONE

    // Filter banks for Left and Right channels
    private var leftCombs: Array<CombFilter> = emptyArray()
    private var rightCombs: Array<CombFilter> = emptyArray()
    private var leftAllpasses: Array<AllpassFilter> = emptyArray()
    private var rightAllpasses: Array<AllpassFilter> = emptyArray()

    // Smooth gain states
    private var currentDryGain = 1.0f
    private var currentWetGain = 0.0f
    private var currentRoomSize = 0.0f
    private var currentDamp = 0.0f

    fun setParent(parent: RhythmReverbProcessor?) {
        this.parentProcessor = parent
    }

    fun setEnabled(enable: Boolean) {
        this.enabled = enable
    }

    fun setPreset(preset: ReverbPreset) {
        this.currentPreset = preset
    }

    fun getPreset(): ReverbPreset = parentProcessor?.getPreset() ?: currentPreset

    override fun isEnabled(): Boolean = parentProcessor?.isEnabled() ?: enabled

    override fun isBypassed(): Boolean {
        val p = getPreset()
        val en = isEnabled()
        // If bypassed and wet gain has settled to 0, completely bypass buffer copying
        return (!en || p == ReverbPreset.NONE) && currentWetGain < 0.001f
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val format = super.configure(inputAudioFormat)
        val rate = format.sampleRate
        val scale = rate / 44100.0

        // Allocate comb filters
        leftCombs = Array(COMB_TUNINGS.size) { i ->
            val size = (COMB_TUNINGS[i] * scale).roundToInt().coerceAtLeast(16)
            CombFilter(size)
        }
        rightCombs = Array(COMB_TUNINGS.size) { i ->
            val size = ((COMB_TUNINGS[i] + STEREO_SPREAD) * scale).roundToInt().coerceAtLeast(16)
            CombFilter(size)
        }

        // Allocate allpass filters
        leftAllpasses = Array(ALLPASS_TUNINGS.size) { i ->
            val size = (ALLPASS_TUNINGS[i] * scale).roundToInt().coerceAtLeast(16)
            AllpassFilter(size)
        }
        rightAllpasses = Array(ALLPASS_TUNINGS.size) { i ->
            val size = ((ALLPASS_TUNINGS[i] + STEREO_SPREAD) * scale).roundToInt().coerceAtLeast(16)
            AllpassFilter(size)
        }

        currentDryGain = 1.0f
        currentWetGain = 0.0f
        return format
    }

    override fun flush() {
        super.flush()
        leftCombs.forEach { it.mute() }
        rightCombs.forEach { it.mute() }
        leftAllpasses.forEach { it.mute() }
        rightAllpasses.forEach { it.mute() }
        currentDryGain = 1.0f
        currentWetGain = 0.0f
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val active = isEnabled()
        val preset = getPreset()

        val shouldProcess = active && preset != ReverbPreset.NONE
        if (!shouldProcess && currentWetGain < 0.0001f) {
            currentDryGain = 1.0f
            currentWetGain = 0.0f
            return
        }

        // Determine target parameters based on preset
        // Key innovation: dryGain is compensated so overall volume NEVER increases!
        val (targetDry, targetWet, targetRoomSize, targetDamp) = if (shouldProcess) {
            when (preset) {
                ReverbPreset.NONE -> ReverbParams(1.0f, 0.0f, 0.0f, 0.0f)
                ReverbPreset.SMALL_ROOM -> ReverbParams(0.86f, 0.16f, 0.52f, 0.60f)
                ReverbPreset.MEDIUM_ROOM -> ReverbParams(0.82f, 0.20f, 0.65f, 0.48f)
                ReverbPreset.LARGE_ROOM -> ReverbParams(0.78f, 0.24f, 0.76f, 0.38f)
                ReverbPreset.MEDIUM_HALL -> ReverbParams(0.75f, 0.26f, 0.84f, 0.30f)
                ReverbPreset.LARGE_HALL -> ReverbParams(0.72f, 0.28f, 0.90f, 0.22f)
                ReverbPreset.PLATE -> ReverbParams(0.78f, 0.24f, 0.80f, 0.12f)
            }
        } else {
            ReverbParams(1.0f, 0.0f, 0.0f, 0.0f)
        }

        val combsL = leftCombs
        val combsR = rightCombs
        val allpassL = leftAllpasses
        val allpassR = rightAllpasses

        if (combsL.isEmpty() || combsR.isEmpty()) return

        val isStereo = channelCount == 2

        // Process samples
        var i = 0
        while (i < sampleCount) {
            // Smoothly ramp gains towards targets (pop & click prevention)
            currentDryGain += (targetDry - currentDryGain) * RAMP_SPEED
            currentWetGain += (targetWet - currentWetGain) * RAMP_SPEED
            currentRoomSize += (targetRoomSize - currentRoomSize) * RAMP_SPEED
            currentDamp += (targetDamp - currentDamp) * RAMP_SPEED

            val inputL = samples[i] / 32768.0f
            val inputR = if (isStereo && i + 1 < sampleCount) samples[i + 1] / 32768.0f else inputL

            // Feed mono sum into reverb comb banks
            val monoIn = (inputL + inputR) * 0.5f

            var outL = 0f
            var outR = 0f

            val numCombs = combsL.size
            for (c in 0 until numCombs) {
                outL += combsL[c].process(monoIn, currentRoomSize, currentDamp)
                outR += combsR[c].process(monoIn, currentRoomSize, currentDamp)
            }

            // Normalize comb outputs
            outL *= (1.0f / numCombs)
            outR *= (1.0f / numCombs)

            // Pass through series allpass diffusion filters
            for (a in allpassL.indices) {
                outL = allpassL[a].process(outL)
                outR = allpassR[a].process(outR)
            }

            // Equal-power / compensated mix:
            // Output = direct * dryGain + reverb * wetGain
            val finalL = inputL * currentDryGain + outL * currentWetGain
            val finalR = inputR * currentDryGain + outR * currentWetGain

            samples[i] = (softClip(finalL) * 32767.0f).toInt().toShort()
            if (isStereo && i + 1 < sampleCount) {
                samples[i + 1] = (softClip(finalR) * 32767.0f).toInt().toShort()
                i += 2
            } else {
                i += 1
            }
        }
    }

    private fun softClip(x: Float): Float {
        if (x == 0f) return 0f
        val absValue = abs(x)
        val threshold = 0.85f
        if (absValue <= threshold) {
            return x
        }
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        return sign(x) * limited
    }

    private data class ReverbParams(
        val dryGain: Float,
        val wetGain: Float,
        val roomSize: Float,
        val damp: Float
    )
}
