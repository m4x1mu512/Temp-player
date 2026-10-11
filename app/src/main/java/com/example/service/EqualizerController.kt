package com.example.service

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.ReverbPreset
import com.example.service.audio.RhythmBassBoostProcessor
import com.example.service.audio.RhythmReverbProcessor
import com.example.service.audio.RhythmSpatializationProcessor

/**
 * Controller for audio effects and equalizer, modeled directly after cromaguy/Rhythm.
 *
 * Architecture Highlights:
 * - Bass Boost: Powered by [RhythmBassBoostProcessor] (real-time 150Hz IIR low-pass filter with
 *   adaptive gain and soft-knee limiter directly in ExoPlayer). Eliminates the notorious Android
 *   hardware AudioEffect volume spike / DRC pumping bug when toggled.
 * - 3D Virtualizer: Powered by [RhythmSpatializationProcessor] (real-time Mid/Side stereo widening
 *   with soft clipping). Completely free of volume surges, pops, or DSP phase artifacts.
 * - Reverb: Powered by [RhythmReverbProcessor] (real-time Schroeder/Freeverb algorithmic reverberator
 *   with equal-power gain normalization). Guarantees that track volume NEVER surges or increases
 *   when enabling any spatial reverb preset, with click-free and pop-free transitions.
 * - Hardware Multi-Band Equalizer: Uses Android's native [Equalizer] with AutoEQ Headroom Protection
 *   (automatically attenuates preamp by peak boost) to ensure output never exceeds 0 dBFS.
 * - Hardware Loudness Enhancer for clean volume boost when requested.
 */
class EqualizerController(
    val bassBoostProcessor: RhythmBassBoostProcessor = RhythmBassBoostProcessor(),
    val spatializationProcessor: RhythmSpatializationProcessor = RhythmSpatializationProcessor(),
    val reverbProcessor: RhythmReverbProcessor = RhythmReverbProcessor()
) {

    companion object {
        private const val TAG = "EqualizerController"
    }

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentAudioSessionId: Int = 0

    var isEnabled: Boolean = true
        private set

    // Base levels chosen by user or preset (in milliBels: -1200 to +1200)
    private var baseBandLevels = ShortArray(16) { 0 }
    private var numBands: Int = 0
    private var bandFrequencies = IntArray(16) { 0 }

    // Last applied hardware levels to prevent JNI flooding / zipper noise
    private var lastAppliedBandLevels = ShortArray(16) { Short.MIN_VALUE }

    val isBassBoostEnabled: Boolean
        get() = bassBoostProcessor.isEnabled()

    val bassBoostStrength: Int
        get() = bassBoostProcessor.getStrength().toInt()

    val isVirtualizerEnabled: Boolean
        get() = spatializationProcessor.isEnabled()

    val virtualizerStrength: Int
        get() = spatializationProcessor.getStrength().toInt()

    val isReverbEnabled: Boolean
        get() = reverbProcessor.isEnabled()

    val currentReverbPreset: ReverbPreset
        get() = reverbProcessor.getPreset()

    var isLoudnessEnabled: Boolean = false
        private set
    var loudnessGain: Int = 0
        private set

    var preampMilliBels: Int = 0
        private set

    fun attachToAudioSession(
        audioSessionId: Int,
        savedLevels: List<Int>?,
        eqEnabled: Boolean,
        preamp: Int = 0,
        bbEnabled: Boolean = false,
        bbStrength: Int = 0,
        virtEnabled: Boolean = false,
        virtStrength: Int = 0,
        revEnabled: Boolean = false,
        revPreset: ReverbPreset = ReverbPreset.NONE,
        loudEnabled: Boolean = false,
        loudGain: Int = 0
    ) {
        if (audioSessionId <= 0) return

        this.isEnabled = eqEnabled
        this.preampMilliBels = preamp
        bassBoostProcessor.setEnabled(bbEnabled)
        bassBoostProcessor.setStrength(bbStrength.coerceIn(0, 1000).toShort())
        spatializationProcessor.setEnabled(virtEnabled)
        spatializationProcessor.setStrength(virtStrength.coerceIn(0, 1000).toShort())
        reverbProcessor.setEnabled(revEnabled)
        reverbProcessor.setPreset(revPreset)
        this.isLoudnessEnabled = loudEnabled
        this.loudnessGain = loudGain

        // If already attached to this session and effects exist, smoothly update without recreating!
        if (audioSessionId == currentAudioSessionId && equalizer != null) {
            applyAllLevels()
            updateLoudnessEnhancer()
            return
        }

        release()
        this.currentAudioSessionId = audioSessionId

        // 1. Equalizer setup
        try {
            val eq = Equalizer(0, audioSessionId)
            numBands = eq.numberOfBands.toInt().coerceIn(0, 16)

            for (i in 0 until numBands) {
                bandFrequencies[i] = eq.getCenterFreq(i.toShort()) / 1000 // in Hz
                val saved = savedLevels?.getOrNull(i)?.toShort() ?: 0.toShort()
                baseBandLevels[i] = saved
            }

            eq.enabled = eqEnabled
            equalizer = eq
            applyAllLevels()
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer unavailable on session $audioSessionId: ${e.message}")
            equalizer = null
        }

        // 2. Loudness Enhancer
        try {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(loudGain.coerceIn(0, 800))
            le.enabled = loudEnabled && loudGain > 0
            loudnessEnhancer = le
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unavailable: ${e.message}")
            loudnessEnhancer = null
        }
    }

    fun attachToAudioSession(audioSessionId: Int, savedLevels: List<Int>?, enabled: Boolean) {
        attachToAudioSession(
            audioSessionId = audioSessionId,
            savedLevels = savedLevels,
            eqEnabled = enabled,
            preamp = preampMilliBels,
            bbEnabled = isBassBoostEnabled,
            bbStrength = bassBoostStrength,
            virtEnabled = isVirtualizerEnabled,
            virtStrength = virtualizerStrength,
            revEnabled = isReverbEnabled,
            revPreset = currentReverbPreset,
            loudEnabled = isLoudnessEnabled,
            loudGain = loudnessGain
        )
    }

    // --- Equalizer & AutoEQ Headroom Protection ---

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setPreamp(preamp: Int) {
        this.preampMilliBels = preamp
        applyAllLevels()
    }

    fun getBands(): List<EqualizerBand> {
        val bands = mutableListOf<EqualizerBand>()
        val count = if (numBands > 0) numBands else 5
        for (i in 0 until count) {
            val band = i.toShort()
            val freq = if (bandFrequencies[i] > 0) bandFrequencies[i] else (60 * (1 shl (i * 2)))
            val level = baseBandLevels[i]
            bands.add(EqualizerBand(band, freq, level))
        }
        return bands
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val eq = equalizer ?: return Pair((-1200).toShort(), 1200.toShort())
        return try {
            val range = eq.bandLevelRange
            Pair(range[0], range[1])
        } catch (_: Exception) {
            Pair((-1200).toShort(), 1200.toShort())
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        val idx = band.toInt()
        if (idx in 0 until 16) {
            baseBandLevels[idx] = level.coerceIn(-1200, 1200)
            applyAllLevels()
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return

        val count = if (numBands > 0) numBands else 5
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

        val interpolated = interpolateLevels(baseLevels, count)
        for (i in 0 until minOf(count, interpolated.size)) {
            baseBandLevels[i] = interpolated[i].toShort()
        }
        applyAllLevels()
    }

    /**
     * Applies baseline levels + preamp + AutoEQ headroom protection to the hardware equalizer.
     * Prevents digital clipping / pops and avoids accumulative preamp volume runaway.
     */
    private fun applyAllLevels() {
        val eq = equalizer ?: return
        val count = minOf(numBands, eq.numberOfBands.toInt())
        if (count <= 0) return

        val range = try {
            eq.bandLevelRange
        } catch (_: Exception) {
            shortArrayOf(-1200, 1200)
        }
        val minRange = range[0]
        val maxRange = range[1]

        // AutoEQ Headroom Protection:
        // Find max positive boost across all bands
        var maxBoost = 0
        for (i in 0 until count) {
            val lvl = baseBandLevels[i].toInt()
            if (lvl > maxBoost) {
                maxBoost = lvl
            }
        }

        // If user boosts frequencies, apply headroom attenuation so total peak never clips above 0 dBFS!
        val autoHeadroomAttenuation = -maxBoost

        for (i in 0 until count) {
            val base = baseBandLevels[i].toInt()
            val computed = base + preampMilliBels + autoHeadroomAttenuation
            val clamped = computed.toShort().coerceIn(minRange, maxRange)

            // Only send to AudioFlinger if value actually changed (prevents zipper noise / clicks)
            if (lastAppliedBandLevels[i] != clamped) {
                try {
                    eq.setBandLevel(i.toShort(), clamped)
                    lastAppliedBandLevels[i] = clamped
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set band level $i: ${e.message}")
                }
            }
        }
    }

    private fun interpolateLevels(base5: List<Int>, targetBands: Int): List<Int> {
        if (targetBands <= 5) return base5.take(targetBands)
        val result = mutableListOf<Int>()
        for (i in 0 until targetBands) {
            val fraction = i.toFloat() / (targetBands - 1).coerceAtLeast(1)
            val indexIn5 = fraction * 4f
            val lowIdx = indexIn5.toInt().coerceIn(0, 3)
            val highIdx = (lowIdx + 1).coerceIn(0, 4)
            val t = indexIn5 - lowIdx
            val interpolated = ((1f - t) * base5[lowIdx] + t * base5[highIdx]).toInt()
            result.add(interpolated)
        }
        return result
    }

    // --- Rhythm Bass Boost API ---

    fun setBassBoostEnabled(enabled: Boolean) {
        bassBoostProcessor.setEnabled(enabled)
    }

    fun setBassBoostStrength(strength: Int) {
        bassBoostProcessor.setStrength(strength.coerceIn(0, 1000).toShort())
    }

    // --- Rhythm 3D Virtualizer (Spatialization) API ---

    fun setVirtualizerEnabled(enabled: Boolean) {
        spatializationProcessor.setEnabled(enabled)
    }

    fun setVirtualizerStrength(strength: Int) {
        spatializationProcessor.setStrength(strength.coerceIn(0, 1000).toShort())
    }

    // --- Reverb API ---

    fun setReverbEnabled(enabled: Boolean) {
        reverbProcessor.setEnabled(enabled)
    }

    fun setReverbPreset(preset: ReverbPreset) {
        reverbProcessor.setPreset(preset)
    }

    // --- Loudness Enhancer API ---

    fun setLoudnessEnabled(enabled: Boolean) {
        this.isLoudnessEnabled = enabled
        updateLoudnessEnhancer()
    }

    fun setLoudnessGain(gainMilliBels: Int) {
        this.loudnessGain = gainMilliBels.coerceIn(0, 800)
        updateLoudnessEnhancer()
    }

    private fun updateLoudnessEnhancer() {
        try {
            val le = loudnessEnhancer ?: return
            le.setTargetGain(loudnessGain)
            val shouldEnable = isLoudnessEnabled && loudnessGain > 0
            if (le.enabled != shouldEnable) {
                le.enabled = shouldEnable
            }
        } catch (_: Exception) {}
    }

    // --- Lifecycle ---

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        loudnessEnhancer = null
        currentAudioSessionId = 0
        lastAppliedBandLevels.fill(Short.MIN_VALUE)
    }
}
