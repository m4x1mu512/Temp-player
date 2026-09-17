package com.example.service

import android.media.audiofx.Equalizer
import android.util.Log
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset

class EqualizerController {
    private var equalizer: Equalizer? = null
    var isEnabled: Boolean = true
        private set

    fun attachToAudioSession(audioSessionId: Int, savedLevels: List<Int>?, enabled: Boolean) {
        release()
        if (audioSessionId <= 0) return
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = enabled
            this.isEnabled = enabled

            // If we have saved levels, apply them
            if (savedLevels != null && savedLevels.size >= eq.numberOfBands) {
                for (i in 0 until eq.numberOfBands) {
                    val level = savedLevels[i].toShort()
                    val range = eq.bandLevelRange
                    val clamped = level.coerceIn(range[0], range[1])
                    eq.setBandLevel(i.toShort(), clamped)
                }
            }
            equalizer = eq
        } catch (e: Exception) {
            Log.w("EqualizerController", "Equalizer unavailable: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {}
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
            equalizer?.setBandLevel(band, level)
        } catch (_: Exception) {}
    }

    fun applyPreset(preset: EqualizerPreset) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val levels = when (preset) {
            EqualizerPreset.FLAT -> listOf(0, 0, 0, 0, 0)
            EqualizerPreset.ROCK -> listOf(500, 300, -100, 300, 600)
            EqualizerPreset.JAZZ -> listOf(400, 200, 100, 300, 400)
            EqualizerPreset.POP -> listOf(-100, 200, 500, 200, -100)
            EqualizerPreset.CLASSICAL -> listOf(500, 300, 0, 200, 400)
            EqualizerPreset.CUSTOM -> return
        }

        val range = eq.bandLevelRange
        for (i in 0 until minOf(numBands, levels.size)) {
            val band = i.toShort()
            val lvl = levels[i].toShort().coerceIn(range[0], range[1])
            try {
                eq.setBandLevel(band, lvl)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
    }
}
