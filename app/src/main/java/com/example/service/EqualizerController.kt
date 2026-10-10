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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Controller for hardware-accelerated audio effects and equalizer,
 * modeled after high-fidelity Android players like Rhythm.
 *
 * Anti-Clipping & Anti-Click Architecture:
 * - AutoEQ Headroom Protection: Automatically attenuates preamp when EQ bands are boosted,
 *   preventing digital clipping, harsh distortion, volume spikes, and DAC pop/click artifacts.
 * - Strict Separation of Baseline and Applied Levels: Preamp is never accumulatively added to band levels.
 * - Anti-Surge Hardware Effect Transitions: Smoothly ramps Bass Boost & Virtualizer strengths
 *   before changing hardware bypass states, eliminating transient volume surges, pops, and DSP AGC pumping.
 * - Audio Session Persistence: Effects are not needlessly destroyed and recreated on each track change,
 *   preventing audio glitches and pop sounds when starting a track.
 * - JNI Throttling: Avoids redundant AudioFlinger calls when band values haven't changed.
 */
class EqualizerController(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    companion object {
        private const val TAG = "EqualizerController"
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentAudioSessionId: Int = 0

    private var bassBoostRampJob: Job? = null
    private var virtualizerRampJob: Job? = null

    var isEnabled: Boolean = true
        private set

    // Base levels chosen by user or preset (in milliBels: -1200 to +1200)
    private var baseBandLevels = ShortArray(16) { 0 }
    private var numBands: Int = 0
    private var bandFrequencies = IntArray(16) { 0 }

    // Last applied hardware levels to prevent JNI flooding / zipper noise
    private var lastAppliedBandLevels = ShortArray(16) { Short.MIN_VALUE }

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

        // If already attached to this session and effects exist, smoothly update without recreating!
        if (audioSessionId == currentAudioSessionId && equalizer != null) {
            applyAllLevels()
            updateBassBoostImmediate()
            updateVirtualizerImmediate()
            updateReverb()
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

        // 2. Bass Boost - Zero out hardware strength when attached disabled to prevent DSP transients
        try {
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = bbEnabled
            if (bb.strengthSupported) {
                val initStrength = if (bbEnabled) bbStrength.coerceIn(0, 1000) else 0
                bb.setStrength(initStrength.toShort())
            }
            bassBoost = bb
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost unavailable: ${e.message}")
            bassBoost = null
        }

        // 3. Virtualizer - Zero out hardware strength when attached disabled
        try {
            val virt = Virtualizer(0, audioSessionId)
            virt.enabled = virtEnabled
            if (virt.strengthSupported) {
                val initStrength = if (virtEnabled) virtStrength.coerceIn(0, 1000) else 0
                virt.setStrength(initStrength.toShort())
            }
            virtualizer = virt
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unavailable: ${e.message}")
            virtualizer = null
        }

        // 4. Preset Reverb
        try {
            val reverb = PresetReverb(0, audioSessionId)
            reverb.preset = revPreset.presetValue
            reverb.enabled = revEnabled && revPreset != ReverbPreset.NONE
            presetReverb = reverb
        } catch (e: Exception) {
            Log.w(TAG, "PresetReverb unavailable: ${e.message}")
            presetReverb = null
        }

        // 5. Loudness Enhancer
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

    // --- Bass Boost API ---

    fun setBassBoostEnabled(enabled: Boolean, smoothTransition: Boolean = true) {
        this.isBassBoostEnabled = enabled
        bassBoostRampJob?.cancel()

        val bb = bassBoost
        if (bb == null || !smoothTransition) {
            updateBassBoostImmediate()
            return
        }

        bassBoostRampJob = coroutineScope.launch {
            try {
                if (enabled) {
                    // Smooth ramp UP:
                    // 1. Enable hardware effect at strength 0 so no sudden pop or surge occurs
                    if (!bb.enabled) {
                        if (bb.strengthSupported) {
                            bb.setStrength(0)
                        }
                        bb.enabled = true
                    }
                    val targetStrength = bassBoostStrength.coerceIn(0, 1000)
                    if (bb.strengthSupported && targetStrength > 0) {
                        val steps = 6
                        val stepDelay = 15L
                        for (i in 1..steps) {
                            delay(stepDelay)
                            val current = (targetStrength * i / steps).toShort()
                            bb.setStrength(current)
                        }
                    }
                } else {
                    // Smooth ramp DOWN (Anti-Spike Protection):
                    // Gradually attenuate low-end boost to 0 dB, allowing the hardware DSP limiter / AGC
                    // to release smoothly instead of causing a sharp transient volume spike!
                    val currentStrength = bassBoostStrength.coerceIn(0, 1000)
                    if (bb.strengthSupported && currentStrength > 0 && bb.enabled) {
                        val steps = 6
                        val stepDelay = 15L
                        for (i in (steps - 1) downTo 0) {
                            delay(stepDelay)
                            val current = (currentStrength * i / steps).toShort()
                            bb.setStrength(current)
                        }
                    }
                    // Filter response is now completely flat. Allow DSP to settle, then bypass effect cleanly.
                    delay(25L)
                    if (!isBassBoostEnabled) {
                        if (bb.strengthSupported) {
                            bb.setStrength(0)
                        }
                        bb.enabled = false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost transition error: ${e.message}")
            }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        this.bassBoostStrength = strength.coerceIn(0, 1000)
        bassBoostRampJob?.cancel()
        try {
            val bb = bassBoost ?: return
            if (isBassBoostEnabled) {
                if (!bb.enabled) {
                    bb.enabled = true
                }
                if (bb.strengthSupported) {
                    bb.setStrength(bassBoostStrength.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateBassBoostImmediate() {
        try {
            val bb = bassBoost ?: return
            if (isBassBoostEnabled) {
                if (!bb.enabled) bb.enabled = true
                if (bb.strengthSupported) {
                    bb.setStrength(bassBoostStrength.toShort().coerceIn(0, 1000))
                }
            } else {
                if (bb.strengthSupported) {
                    bb.setStrength(0)
                }
                if (bb.enabled) bb.enabled = false
            }
        } catch (_: Exception) {}
    }

    // --- Virtualizer API ---

    fun setVirtualizerEnabled(enabled: Boolean, smoothTransition: Boolean = true) {
        this.isVirtualizerEnabled = enabled
        virtualizerRampJob?.cancel()

        val virt = virtualizer
        if (virt == null || !smoothTransition) {
            updateVirtualizerImmediate()
            return
        }

        virtualizerRampJob = coroutineScope.launch {
            try {
                if (enabled) {
                    // Smooth ramp UP
                    if (!virt.enabled) {
                        if (virt.strengthSupported) {
                            virt.setStrength(0)
                        }
                        virt.enabled = true
                    }
                    val targetStrength = virtualizerStrength.coerceIn(0, 1000)
                    if (virt.strengthSupported && targetStrength > 0) {
                        val steps = 6
                        val stepDelay = 15L
                        for (i in 1..steps) {
                            delay(stepDelay)
                            val current = (targetStrength * i / steps).toShort()
                            virt.setStrength(current)
                        }
                    }
                } else {
                    // Smooth ramp DOWN (Anti-Spike Protection):
                    // Gradually reduce spatial widening to zero, eliminating phase cancellation
                    // and volume surge artifacts before disabling hardware processing.
                    val currentStrength = virtualizerStrength.coerceIn(0, 1000)
                    if (virt.strengthSupported && currentStrength > 0 && virt.enabled) {
                        val steps = 6
                        val stepDelay = 15L
                        for (i in (steps - 1) downTo 0) {
                            delay(stepDelay)
                            val current = (currentStrength * i / steps).toShort()
                            virt.setStrength(current)
                        }
                    }
                    delay(25L)
                    if (!isVirtualizerEnabled) {
                        if (virt.strengthSupported) {
                            virt.setStrength(0)
                        }
                        virt.enabled = false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer transition error: ${e.message}")
            }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        this.virtualizerStrength = strength.coerceIn(0, 1000)
        virtualizerRampJob?.cancel()
        try {
            val virt = virtualizer ?: return
            if (isVirtualizerEnabled) {
                if (!virt.enabled) {
                    virt.enabled = true
                }
                if (virt.strengthSupported) {
                    virt.setStrength(virtualizerStrength.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateVirtualizerImmediate() {
        try {
            val virt = virtualizer ?: return
            if (isVirtualizerEnabled) {
                if (!virt.enabled) virt.enabled = true
                if (virt.strengthSupported) {
                    virt.setStrength(virtualizerStrength.toShort().coerceIn(0, 1000))
                }
            } else {
                if (virt.strengthSupported) {
                    virt.setStrength(0)
                }
                if (virt.enabled) virt.enabled = false
            }
        } catch (_: Exception) {}
    }

    // --- Reverb API ---

    fun setReverbEnabled(enabled: Boolean) {
        this.isReverbEnabled = enabled
        updateReverb()
    }

    fun setReverbPreset(preset: ReverbPreset) {
        this.currentReverbPreset = preset
        updateReverb()
    }

    private fun updateReverb() {
        try {
            val reverb = presetReverb ?: return
            reverb.preset = currentReverbPreset.presetValue
            val shouldEnable = isReverbEnabled && currentReverbPreset != ReverbPreset.NONE
            if (reverb.enabled != shouldEnable) {
                reverb.enabled = shouldEnable
            }
        } catch (_: Exception) {}
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
        bassBoostRampJob?.cancel()
        virtualizerRampJob?.cancel()
        bassBoostRampJob = null
        virtualizerRampJob = null
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
        currentAudioSessionId = 0
        lastAppliedBandLevels.fill(Short.MIN_VALUE)
    }
}
