package com.example.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.ReverbPreset

/**
 * Controller for hardware-accelerated audio effects and equalizer,
 * modeled after high-fidelity Android players like Rhythm.
 *
 * Supports:
 * - Multi-band Equalizer with 12 presets and smooth band interpolation
 * - Preamp / Headroom clipping protection
 * - Bass Boost with strength control
 * - Virtualizer (3D audio surround)
 * - Preset Reverb (environmental room simulation)
 * - Loudness Enhancer (dynamic range gain boost)
 */
class EqualizerController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isEnabled: Boolean = true
        private set

    var isBassBoostEnabled: Boolean = false
        private set
    var bassBoostStrength: Int = 0
        private set

    var isVirtualizerEnabled: Boolean = false
        private set
    var virtualizerStrength: Int = 0
        private set

    var isReverbEnabled: Boolean = false
        private set
    var currentReverbPreset: ReverbPreset = ReverbPreset.NONE
        private set

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
        release()
        if (audioSessionId <= 0) return

        this.isEnabled = eqEnabled
        this.preampMilliBels = preamp
        this.isBassBoostEnabled = bbEnabled
        this.bassBoostStrength = bbStrength
        this.isVirtualizerEnabled = virtEnabled
        this.virtualizerStrength = virtStrength
        this.isReverbEnabled = revEnabled
        this.currentReverbPreset = revPreset
        this.isLoudnessEnabled = loudEnabled
        this.loudnessGain = loudGain

        // 1. Equalizer
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = eqEnabled

            if (savedLevels != null && savedLevels.size >= eq.numberOfBands) {
                val range = eq.bandLevelRange
                for (i in 0 until eq.numberOfBands) {
                    val rawLevel = savedLevels[i] + preamp
                    val clamped = rawLevel.toShort().coerceIn(range[0], range[1])
                    eq.setBandLevel(i.toShort(), clamped)
                }
            }
            equalizer = eq
        } catch (e: Exception) {
            Log.w("EqualizerController", "Equalizer unavailable: ${e.message}")
        }

        // 2. Bass Boost
        try {
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = bbEnabled
            if (bb.strengthSupported) {
                bb.setStrength(bbStrength.toShort().coerceIn(0, 1000))
            }
            bassBoost = bb
        } catch (e: Exception) {
            Log.w("EqualizerController", "BassBoost unavailable: ${e.message}")
        }

        // 3. Virtualizer
        try {
            val virt = Virtualizer(0, audioSessionId)
            virt.enabled = virtEnabled
            if (virt.strengthSupported) {
                virt.setStrength(virtStrength.toShort().coerceIn(0, 1000))
            }
            virtualizer = virt
        } catch (e: Exception) {
            Log.w("EqualizerController", "Virtualizer unavailable: ${e.message}")
        }

        // 4. Preset Reverb
        try {
            val reverb = PresetReverb(0, audioSessionId)
            reverb.preset = revPreset.presetValue
            reverb.enabled = revEnabled && revPreset != ReverbPreset.NONE
            presetReverb = reverb
        } catch (e: Exception) {
            Log.w("EqualizerController", "PresetReverb unavailable: ${e.message}")
        }

        // 5. Loudness Enhancer
        try {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(loudGain.coerceIn(0, 1000))
            le.enabled = loudEnabled
            loudnessEnhancer = le
        } catch (e: Exception) {
            Log.w("EqualizerController", "LoudnessEnhancer unavailable: ${e.message}")
        }
    }

    // Overload for backward compatibility
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

    // --- Equalizer API ---

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setPreamp(preamp: Int) {
        this.preampMilliBels = preamp
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        for (i in 0 until eq.numberOfBands) {
            val current = eq.getBandLevel(i.toShort())
            val adjusted = (current + preamp).toShort().coerceIn(range[0], range[1])
            try {
                eq.setBandLevel(i.toShort(), adjusted)
            } catch (_: Exception) {}
        }
    }

    fun getBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        val bands = mutableListOf<EqualizerBand>()
        for (i in 0 until numBands) {
            val band = i.toShort()
            val centerFreq = eq.getCenterFreq(band) / 1000 // In Hz
            val level = eq.getBandLevel(band)
            bands.add(EqualizerBand(band, centerFreq, level))
        }
        return bands
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val eq = equalizer ?: return Pair((-1200).toShort(), 1200.toShort())
        val range = eq.bandLevelRange
        return Pair(range[0], range[1])
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            val eq = equalizer ?: return
            val range = eq.bandLevelRange
            val clamped = level.coerceIn(range[0], range[1])
            eq.setBandLevel(band, clamped)
        } catch (_: Exception) {}
    }

    fun applyPreset(preset: EqualizerPreset) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        if (preset == EqualizerPreset.CUSTOM) return

        // Base 5-band frequency curves in milliBels (-1200 to +1200)
        val baseLevels = when (preset) {
            EqualizerPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqualizerPreset.BASS_BOOST -> listOf(650, 450, 200, 0, 0)
            EqualizerPreset.TREBLE_BOOST -> listOf(0, 0, 150, 450, 650)
            EqualizerPreset.ROCK -> listOf(500, 300, -100, 300, 600)
            EqualizerPreset.JAZZ -> listOf(400, 200, 100, 300, 400)
            EqualizerPreset.POP -> listOf(-100, 200, 500, 200, -100)
            EqualizerPreset.CLASSICAL -> listOf(500, 300, 0, 200, 400)
            EqualizerPreset.ELECTRONIC -> listOf(500, 250, 0, 250, 550)
            EqualizerPreset.HIP_HOP -> listOf(600, 350, 0, 150, 350)
            EqualizerPreset.VOCAL -> listOf(-250, 100, 500, 350, -150)
            EqualizerPreset.ACOUSTIC -> listOf(350, 200, 100, 250, 350)
            EqualizerPreset.CUSTOM -> return
        }

        val levels = interpolateLevels(baseLevels, numBands)
        val range = eq.bandLevelRange
        for (i in 0 until minOf(numBands, levels.size)) {
            val band = i.toShort()
            val targetLevel = (levels[i] + preampMilliBels).toShort().coerceIn(range[0], range[1])
            try {
                eq.setBandLevel(band, targetLevel)
            } catch (_: Exception) {}
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

    // --- Bass Boost API ---

    fun setBassBoostEnabled(enabled: Boolean) {
        this.isBassBoostEnabled = enabled
        try {
            bassBoost?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setBassBoostStrength(strength: Int) {
        this.bassBoostStrength = strength
        try {
            val bb = bassBoost ?: return
            if (bb.strengthSupported) {
                bb.setStrength(strength.toShort().coerceIn(0, 1000))
            }
        } catch (_: Exception) {}
    }

    // --- Virtualizer API ---

    fun setVirtualizerEnabled(enabled: Boolean) {
        this.isVirtualizerEnabled = enabled
        try {
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setVirtualizerStrength(strength: Int) {
        this.virtualizerStrength = strength
        try {
            val virt = virtualizer ?: return
            if (virt.strengthSupported) {
                virt.setStrength(strength.toShort().coerceIn(0, 1000))
            }
        } catch (_: Exception) {}
    }

    // --- Reverb API ---

    fun setReverbEnabled(enabled: Boolean) {
        this.isReverbEnabled = enabled
        try {
            presetReverb?.enabled = enabled && currentReverbPreset != ReverbPreset.NONE
        } catch (_: Exception) {}
    }

    fun setReverbPreset(preset: ReverbPreset) {
        this.currentReverbPreset = preset
        try {
            val reverb = presetReverb ?: return
            reverb.preset = preset.presetValue
            reverb.enabled = isReverbEnabled && preset != ReverbPreset.NONE
        } catch (_: Exception) {}
    }

    // --- Loudness Enhancer API ---

    fun setLoudnessEnabled(enabled: Boolean) {
        this.isLoudnessEnabled = enabled
        try {
            loudnessEnhancer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun setLoudnessGain(gainMilliBels: Int) {
        this.loudnessGain = gainMilliBels
        try {
            loudnessEnhancer?.setTargetGain(gainMilliBels.coerceIn(0, 1000))
        } catch (_: Exception) {}
    }

    // --- Lifecycle ---

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { presetReverb?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
    }
}
