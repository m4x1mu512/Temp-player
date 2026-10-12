package com.example.service

import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.ReverbPreset
import com.example.service.audio.RhythmBassBoostProcessor
import com.example.service.audio.RhythmEqualizerProcessor
import com.example.service.audio.RhythmReverbProcessor
import com.example.service.audio.RhythmSpatializationProcessor

/**
 * Controller for audio effects and equalizer, modeled directly after cromaguy/Rhythm.
 *
 * Architecture Highlights:
 * - Multi-Band Equalizer: Powered by [RhythmEqualizerProcessor] directly in ExoPlayer's
 *   AudioProcessor pipeline. Uses biquad peaking filters with AutoEQ Headroom Protection
 *   and smooth parameter slewing. Eliminates the volume jump / DRC surge bug when turning
 *   off "Equalizer and effects".
 * - Bass Boost: Powered by [RhythmBassBoostProcessor] (real-time 150Hz IIR low-pass filter with
 *   adaptive gain and soft-knee limiter directly in ExoPlayer).
 * - 3D Virtualizer: Powered by [RhythmSpatializationProcessor] (real-time Mid/Side stereo widening
 *   with soft clipping). Completely free of volume surges, pops, or DSP phase artifacts.
 * - Reverb: Powered by [RhythmReverbProcessor] (real-time Schroeder/Freeverb algorithmic reverberator
 *   with equal-power gain normalization). Guarantees that track volume NEVER surges or increases
 *   when enabling any spatial reverb preset, with click-free and pop-free transitions.
 * - Hardware Loudness Enhancer for clean volume boost when explicitly requested.
 */
class EqualizerController(
    val equalizerProcessor: RhythmEqualizerProcessor = RhythmEqualizerProcessor(),
    val bassBoostProcessor: RhythmBassBoostProcessor = RhythmBassBoostProcessor(),
    val spatializationProcessor: RhythmSpatializationProcessor = RhythmSpatializationProcessor(),
    val reverbProcessor: RhythmReverbProcessor = RhythmReverbProcessor()
) {

    companion object {
        private const val TAG = "EqualizerController"
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentAudioSessionId: Int = 0

    val isEnabled: Boolean
        get() = equalizerProcessor.isEnabled()

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

    val preampMilliBels: Int
        get() = equalizerProcessor.getPreamp()

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
        equalizerProcessor.setEnabled(eqEnabled)
        equalizerProcessor.setPreamp(preamp)
        savedLevels?.let { equalizerProcessor.setLevels(it) }

        bassBoostProcessor.setEnabled(bbEnabled)
        bassBoostProcessor.setStrength(bbStrength.coerceIn(0, 1000).toShort())

        spatializationProcessor.setEnabled(virtEnabled)
        spatializationProcessor.setStrength(virtStrength.coerceIn(0, 1000).toShort())

        reverbProcessor.setEnabled(revEnabled)
        reverbProcessor.setPreset(revPreset)

        this.isLoudnessEnabled = loudEnabled
        this.loudnessGain = loudGain

        if (audioSessionId <= 0) return

        if (audioSessionId == currentAudioSessionId && loudnessEnhancer != null) {
            updateLoudnessEnhancer()
            return
        }

        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        currentAudioSessionId = audioSessionId

        try {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(loudGain.coerceIn(0, 800))
            le.enabled = loudEnabled && loudGain > 0
            loudnessEnhancer = le
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unavailable on session $audioSessionId: ${e.message}")
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

    // --- Equalizer & AutoEQ Headroom Protection API ---

    fun setEnabled(enabled: Boolean) {
        equalizerProcessor.setEnabled(enabled)
    }

    fun setPreamp(preamp: Int) {
        equalizerProcessor.setPreamp(preamp)
    }

    fun getBands(): List<EqualizerBand> {
        return equalizerProcessor.getBands()
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        return equalizerProcessor.getBandLevelRange()
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizerProcessor.setBandLevel(band, level)
    }

    fun setLevels(levels: List<Int>) {
        equalizerProcessor.setLevels(levels)
    }

    fun applyPreset(preset: EqualizerPreset) {
        equalizerProcessor.applyPreset(preset)
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
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        currentAudioSessionId = 0
    }
}
