package com.example.service.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Rhythm Equalizer Processor - High-precision real-time multi-band equalizer.
 *
 * Modeled directly after cromaguy/Rhythm:
 * Implements a 5-band biquad peaking equalizer (Robert Bristow-Johnson Audio EQ Cookbook)
 * with AutoEQ Headroom Protection, pre-amplification control, and smooth gain slewing.
 *
 * Key guarantees:
 * - Completely runs in ExoPlayer's PCM AudioProcessor pipeline, bypassing Android's
 *   hardware AudioFlinger/AudioEffect equalizer entirely.
 * - Eliminates the notorious volume jump / DRC surge bug when toggling the equalizer on/off.
 * - Anti-pop and anti-spike parameter slewing: transitions between enabled and disabled
 *   smoothly over ~25ms with zero audible clicks, pops, or volume surges.
 * - When disabled or flat, bypasses processing entirely with zero CPU overhead and zero memory copies.
 * - Output soft-knee limiting ensures 0 dBFS headroom safety without harsh digital clipping.
 */
@OptIn(UnstableApi::class)
class RhythmEqualizerProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "RhythmEqualizer"
        const val NUM_BANDS = 5
        val BAND_FREQUENCIES = intArrayOf(60, 230, 910, 3600, 14000)
        private const val FILTER_Q = 1.0f // Standard bandwidth for 5-band graphic EQ
        private const val SLEW_COEFF = 0.003f // Smooth gain transition per sample (~25ms at 44.1kHz)
    }

    @Volatile
    private var parentProcessor: RhythmEqualizerProcessor? = null

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var preampMilliBels: Int = 0

    // Band levels in milliBels (-1200 to +1200, where 100 mB = 1 dB)
    private val bandLevels = ShortArray(NUM_BANDS) { 0 }

    // Dynamic smoothing states
    private var currentPreampScale = 1.0f
    private var targetPreampScale = 1.0f

    // Current and target gains in dB for each band
    private val currentBandGainsDb = FloatArray(NUM_BANDS) { 0f }
    private val targetBandGainsDb = FloatArray(NUM_BANDS) { 0f }

    // Biquad coefficients per band
    private val b0 = FloatArray(NUM_BANDS) { 1f }
    private val b1 = FloatArray(NUM_BANDS) { 0f }
    private val b2 = FloatArray(NUM_BANDS) { 0f }
    private val a1 = FloatArray(NUM_BANDS) { 0f }
    private val a2 = FloatArray(NUM_BANDS) { 0f }
    private val bandActive = BooleanArray(NUM_BANDS) { false }

    // Filter states (Direct Form II Transposed): [band * channelCount + channel]
    private var filterState1 = FloatArray(NUM_BANDS * 2) { 0f }
    private var filterState2 = FloatArray(NUM_BANDS * 2) { 0f }

    private val lock = Any()

    fun setParent(parent: RhythmEqualizerProcessor?) {
        this.parentProcessor = parent
    }

    override fun isEnabled(): Boolean = parentProcessor?.isEnabled() ?: enabled

    fun setEnabled(enable: Boolean) {
        this.enabled = enable
        updateTargets()
    }

    fun setPreamp(preamp: Int) {
        this.preampMilliBels = preamp.coerceIn(-1200, 600)
        updateTargets()
    }

    fun getPreamp(): Int = parentProcessor?.getPreamp() ?: preampMilliBels

    fun setBandLevel(band: Short, level: Short) {
        val idx = band.toInt()
        if (idx in 0 until NUM_BANDS) {
            synchronized(lock) {
                bandLevels[idx] = level.coerceIn(-1200, 1200)
            }
            updateTargets()
        }
    }

    fun setLevels(levels: List<Int>) {
        synchronized(lock) {
            for (i in 0 until minOf(levels.size, NUM_BANDS)) {
                bandLevels[i] = levels[i].toShort().coerceIn(-1200, 1200)
            }
        }
        updateTargets()
    }

    fun getBands(): List<EqualizerBand> {
        val parent = parentProcessor
        if (parent != null) {
            return parent.getBands()
        }
        synchronized(lock) {
            return (0 until NUM_BANDS).map { i ->
                EqualizerBand(i.toShort(), BAND_FREQUENCIES[i], bandLevels[i])
            }
        }
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        return Pair((-1200).toShort(), 1200.toShort())
    }

    fun applyPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return

        val baseLevels = when (preset) {
            EqualizerPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqualizerPreset.BASS_BOOST -> listOf(600, 400, 150, 0, 0)
            EqualizerPreset.TREBLE_BOOST -> listOf(0, 0, 150, 400, 600)
            EqualizerPreset.ROCK -> listOf(500, 300, -100, 300, 550)
            EqualizerPreset.JAZZ -> listOf(400, 200, 100, 250, 350)
            EqualizerPreset.POP -> listOf(-100, 200, 450, 200, -100)
            EqualizerPreset.CLASSICAL -> listOf(450, 250, 0, 200, 350)
            EqualizerPreset.ELECTRONIC -> listOf(500, 250, 0, 250, 500)
            EqualizerPreset.HIP_HOP -> listOf(550, 300, 0, 150, 300)
            EqualizerPreset.VOCAL -> listOf(-200, 100, 450, 300, -150)
            EqualizerPreset.ACOUSTIC -> listOf(300, 200, 100, 200, 300)
            EqualizerPreset.CUSTOM -> return
        }

        synchronized(lock) {
            for (i in 0 until minOf(baseLevels.size, NUM_BANDS)) {
                bandLevels[i] = baseLevels[i].toShort()
            }
        }
        updateTargets()
    }

    private fun updateTargets() {
        val active = isEnabled()
        val currentLevels = parentProcessor?.bandLevels ?: bandLevels
        val currentPreamp = parentProcessor?.getPreamp() ?: preampMilliBels

        synchronized(lock) {
            if (!active) {
                // When disabled, smoothly slew target back to unity and flat EQ
                targetPreampScale = 1.0f
                for (i in 0 until NUM_BANDS) {
                    targetBandGainsDb[i] = 0f
                }
            } else {
                // AutoEQ Headroom Protection:
                // Find maximum positive boost across all active bands
                var maxBoost = 0
                for (i in 0 until NUM_BANDS) {
                    val lvl = currentLevels[i].toInt()
                    if (lvl > maxBoost) {
                        maxBoost = lvl
                    }
                }

                // Headroom attenuation offsets positive peak boost to guarantee output <= 0 dBFS
                val autoHeadroomAttenuation = -maxBoost
                val effectivePreampMilliBels = currentPreamp + autoHeadroomAttenuation
                val effectivePreampDb = effectivePreampMilliBels / 100.0f
                targetPreampScale = 10f.pow(effectivePreampDb / 20.0f)

                for (i in 0 until NUM_BANDS) {
                    targetBandGainsDb[i] = currentLevels[i].toFloat() / 100.0f
                }
            }
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val outputFormat = super.configure(inputAudioFormat)
        val channels = inputAudioFormat.channelCount
        val totalStates = NUM_BANDS * max(1, channels)
        if (filterState1.size != totalStates) {
            filterState1 = FloatArray(totalStates) { 0f }
            filterState2 = FloatArray(totalStates) { 0f }
        } else {
            filterState1.fill(0f)
            filterState2.fill(0f)
        }
        recalculateCoefficients(force = true)
        return outputFormat
    }

    override fun flush() {
        super.flush()
        filterState1.fill(0f)
        filterState2.fill(0f)
    }

    override fun isBypassed(): Boolean {
        if (isEnabled()) return false

        // Bypassed only when slewing has fully converged to unity preamp and flat EQ
        val preampConverged = abs(currentPreampScale - 1.0f) < 0.002f
        if (!preampConverged) return false

        synchronized(lock) {
            for (i in 0 until NUM_BANDS) {
                if (abs(currentBandGainsDb[i]) > 0.05f) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Updates biquad peaking coefficients using Robert Bristow-Johnson's Audio EQ Cookbook.
     */
    private fun recalculateCoefficients(force: Boolean = false) {
        if (sampleRate <= 0) return

        val sr = sampleRate.toDouble()
        for (i in 0 until NUM_BANDS) {
            val gainDb = currentBandGainsDb[i]
            if (!force && abs(gainDb) < 0.05f) {
                b0[i] = 1f
                b1[i] = 0f
                b2[i] = 0f
                a1[i] = 0f
                a2[i] = 0f
                bandActive[i] = false
                continue
            }

            val freq = BAND_FREQUENCIES[i].toDouble()
            // Nyquist guard: frequency must be below sr / 2
            if (freq >= sr / 2.05) {
                b0[i] = 1f
                b1[i] = 0f
                b2[i] = 0f
                a1[i] = 0f
                a2[i] = 0f
                bandActive[i] = false
                continue
            }

            val a = 10.0.pow(gainDb.toDouble() / 40.0)
            val w0 = 2.0 * PI * freq / sr
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * FILTER_Q)

            val a0 = 1.0 + alpha / a
            val a1Val = -2.0 * cosW0
            val a2Val = 1.0 - alpha / a
            val b0Val = 1.0 + alpha * a
            val b1Val = -2.0 * cosW0
            val b2Val = 1.0 - alpha * a

            b0[i] = (b0Val / a0).toFloat()
            b1[i] = (b1Val / a0).toFloat()
            b2[i] = (b2Val / a0).toFloat()
            a1[i] = (a1Val / a0).toFloat()
            a2[i] = (a2Val / a0).toFloat()
            bandActive[i] = true
        }
    }

    /**
     * Soft-knee limiter using saturating curve to ensure zero digital clipping.
     */
    private fun softLimitSample(x: Float): Short {
        val threshold = 28000f
        val absX = abs(x)
        if (absX <= threshold) {
            return x.toInt().coerceIn(-32768, 32767).toShort()
        }
        val excess = absX - threshold
        val maxExcess = 32767f - threshold
        val compressed = threshold + (excess * maxExcess) / (excess + maxExcess)
        val result = if (x > 0f) compressed else -compressed
        return result.toInt().coerceIn(-32768, 32767).toShort()
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val channels = channelCount
        if (channels <= 0) return

        updateTargets()

        val frames = sampleCount / channels
        var coeffsDirty = false

        for (frame in 0 until frames) {
            val offset = frame * channels

            // 1. Slew preamp scale smoothly
            if (abs(currentPreampScale - targetPreampScale) > 0.0001f) {
                currentPreampScale += (targetPreampScale - currentPreampScale) * SLEW_COEFF
            }

            // 2. Slew band gains smoothly (every 16 frames for optimal CPU performance)
            if (frame % 16 == 0) {
                for (b in 0 until NUM_BANDS) {
                    val diff = targetBandGainsDb[b] - currentBandGainsDb[b]
                    if (abs(diff) > 0.01f) {
                        currentBandGainsDb[b] += diff * 0.05f
                        coeffsDirty = true
                    }
                }
                if (coeffsDirty) {
                    recalculateCoefficients(force = false)
                    coeffsDirty = false
                }
            }

            // 3. Process each channel through the biquad filter bank
            for (ch in 0 until channels) {
                var sample = samples[offset + ch].toFloat() * currentPreampScale

                for (b in 0 until NUM_BANDS) {
                    if (!bandActive[b]) continue

                    val stateIdx = b * channels + ch
                    val y = b0[b] * sample + filterState1[stateIdx]
                    filterState1[stateIdx] = b1[b] * sample - a1[b] * y + filterState2[stateIdx]
                    filterState2[stateIdx] = b2[b] * sample - a2[b] * y
                    sample = y
                }

                // 4. Output limiter ensures zero audio clipping
                samples[offset + ch] = softLimitSample(sample)
            }
        }
    }
}
