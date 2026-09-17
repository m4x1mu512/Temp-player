package com.example.service

import android.content.Context
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

class AudioVisualizerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var visualizer: Visualizer? = null
    private val _rawFftData = MutableStateFlow<FloatArray>(FloatArray(32))
    val rawFftData: StateFlow<FloatArray> = _rawFftData.asStateFlow()

    private var simulationJob: Job? = null
    private var isPlaying = false
    private var currentBandCount = 32
    private var sensitivity = 1.0f

    // Smoothed values to avoid jumpiness
    private var smoothedValues = FloatArray(32)

    fun updateConfig(bands: Int, sens: Float) {
        currentBandCount = bands.coerceIn(16, 64)
        sensitivity = sens.coerceIn(0.2f, 3.0f)
        if (smoothedValues.size != currentBandCount) {
            smoothedValues = FloatArray(currentBandCount)
            _rawFftData.value = FloatArray(currentBandCount)
        }
    }

    fun attachToAudioSession(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId <= 0) {
            startSimulationMode()
            return
        }

        try {
            val vis = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null && isPlaying) {
                                processWaveform(waveform)
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null && isPlaying) {
                                processFft(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            visualizer = vis
            stopSimulationMode()
        } catch (e: Exception) {
            Log.w("AudioVisualizer", "Native visualizer unavailable (${e.message}), using fallback mode")
            startSimulationMode()
        }
    }

    private fun processFft(fft: ByteArray) {
        val bands = currentBandCount
        val newValues = FloatArray(bands)
        val step = (fft.size / 2 / bands).coerceAtLeast(1)

        for (i in 0 until bands) {
            val index = (i * step).coerceIn(0, fft.size - 2)
            val real = fft[index].toFloat()
            val imag = fft[index + 1].toFloat()
            val magnitude = kotlin.math.hypot(real, imag) * (sensitivity * 0.04f)
            val normalized = (magnitude / 128f).coerceIn(0f, 1f)

            // Exponential smoothing
            smoothedValues[i] = smoothedValues[i] * 0.65f + normalized * 0.35f
            newValues[i] = smoothedValues[i]
        }
        _rawFftData.value = newValues
    }

    private fun processWaveform(waveform: ByteArray) {
        // Handled via FFT, but backup if needed
    }

    fun onPlaybackStateChanged(playing: Boolean) {
        isPlaying = playing
        if (!playing) {
            // Decay to zero
            smoothedValues = FloatArray(currentBandCount)
            _rawFftData.value = FloatArray(currentBandCount)
        }
    }

    private fun startSimulationMode() {
        if (simulationJob?.isActive == true) return
        simulationJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive) {
                if (isPlaying) {
                    val bands = currentBandCount
                    val arr = FloatArray(bands)
                    phase += 0.12
                    for (i in 0 until bands) {
                        val harmonic1 = sin(phase * 1.5 + i * 0.35)
                        val harmonic2 = sin(phase * 0.8 - i * 0.15)
                        val harmonic3 = sin(phase * 2.2 + i * 0.6)
                        val combined = abs(harmonic1 * 0.5 + harmonic2 * 0.3 + harmonic3 * 0.2).toFloat()
                        val scaled = (combined * sensitivity).coerceIn(0.05f, 0.98f)

                        smoothedValues[i] = smoothedValues[i] * 0.7f + scaled * 0.3f
                        arr[i] = smoothedValues[i]
                    }
                    _rawFftData.value = arr
                } else {
                    val bands = currentBandCount
                    val arr = FloatArray(bands)
                    for (i in 0 until bands) {
                        smoothedValues[i] *= 0.8f
                        arr[i] = smoothedValues[i]
                    }
                    _rawFftData.value = arr
                }
                delay(33) // ~30 fps
            }
        }
    }

    private fun stopSimulationMode() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun release() {
        releaseVisualizer()
        stopSimulationMode()
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
    }
}
