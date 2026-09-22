package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
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
import kotlin.math.cos
import kotlin.math.sin

class AudioVisualizerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var visualizer: Visualizer? = null
    private var lastAudioSessionId: Int = 0

    private val _rawFftData = MutableStateFlow<FloatArray>(FloatArray(32))
    val rawFftData: StateFlow<FloatArray> = _rawFftData.asStateFlow()

    private val _waveformData = MutableStateFlow<FloatArray>(FloatArray(32))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _amplitude = MutableStateFlow<Float>(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var simulationJob: Job? = null
    private var isPlaying = false
    private var currentBandCount = 32
    private var sensitivity = 1.0f

    // Smoothed values to avoid jumpiness
    private var smoothedFftValues = FloatArray(32)
    private var smoothedWaveformValues = FloatArray(32)
    private var smoothedAmplitude = 0f

    fun updateConfig(bands: Int, sens: Float) {
        currentBandCount = bands.coerceIn(16, 64)
        sensitivity = sens.coerceIn(0.2f, 3.0f)
        if (smoothedFftValues.size != currentBandCount) {
            smoothedFftValues = FloatArray(currentBandCount)
            smoothedWaveformValues = FloatArray(currentBandCount)
            _rawFftData.value = FloatArray(currentBandCount)
            _waveformData.value = FloatArray(currentBandCount)
        }
    }

    fun onPermissionGranted() {
        if (lastAudioSessionId > 0) {
            attachToAudioSession(lastAudioSessionId)
        }
    }

    fun attachToAudioSession(audioSessionId: Int) {
        lastAudioSessionId = audioSessionId
        releaseVisualizer()
        if (audioSessionId <= 0) {
            startSimulationMode()
            return
        }

        // Verify RECORD_AUDIO permission before instantiating native Visualizer (AudioEffect).
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startSimulationMode()
            return
        }

        try {
            val range = Visualizer.getCaptureSizeRange()
            val preferredSize = 256.coerceIn(range[0], range[1])
            val vis = Visualizer(audioSessionId).apply {
                captureSize = preferredSize
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
                    Visualizer.getMaxCaptureRate().coerceAtLeast(10000) / 2,
                    true,
                    true
                )
                enabled = true
            }
            visualizer = vis
            stopSimulationMode()
        } catch (t: Throwable) {
            Log.w("AudioVisualizer", "Native visualizer unavailable (${t.message}), using fallback mode")
            startSimulationMode()
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        val size = waveform.size
        if (size == 0) return

        // 1. Calculate overall RMS and peak amplitude from 8-bit unsigned PCM samples
        var sumSquares = 0.0
        var maxPeak = 0f
        for (i in 0 until size) {
            val sample = ((waveform[i].toInt() and 0xFF) - 128) / 128f
            sumSquares += sample * sample
            val absSample = abs(sample)
            if (absSample > maxPeak) {
                maxPeak = absSample
            }
        }
        val rms = kotlin.math.sqrt(sumSquares / size).toFloat()
        val instantAmp = ((rms * 0.7f + maxPeak * 0.3f) * sensitivity * 1.6f).coerceIn(0f, 1f)
        smoothedAmplitude = smoothedAmplitude * 0.65f + instantAmp * 0.35f
        _amplitude.value = smoothedAmplitude

        // 2. Calculate segmented amplitude bars across waveform
        val bands = currentBandCount
        val step = (size / bands).coerceAtLeast(1)
        val bandAmps = FloatArray(bands)
        for (i in 0 until bands) {
            val start = i * step
            val end = (start + step).coerceAtMost(size)
            var segSum = 0.0
            var segPeak = 0f
            for (j in start until end) {
                val s = abs(((waveform[j].toInt() and 0xFF) - 128) / 128f)
                segSum += s
                if (s > segPeak) segPeak = s
            }
            val count = (end - start).coerceAtLeast(1)
            val segAvg = (segSum / count).toFloat()
            val combined = (segAvg * 0.6f + segPeak * 0.4f) * sensitivity * 2.2f
            val normalized = combined.coerceIn(0.04f, 1.0f)
            smoothedWaveformValues[i] = smoothedWaveformValues[i] * 0.55f + normalized * 0.45f
            bandAmps[i] = smoothedWaveformValues[i]
        }
        _waveformData.value = bandAmps
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
            smoothedFftValues[i] = smoothedFftValues[i] * 0.65f + normalized * 0.35f
            newValues[i] = smoothedFftValues[i]
        }
        _rawFftData.value = newValues
    }

    fun onPlaybackStateChanged(playing: Boolean) {
        isPlaying = playing
        if (!playing) {
            // Smooth decay to zero
            smoothedAmplitude = 0f
            _amplitude.value = 0f
            smoothedFftValues = FloatArray(currentBandCount)
            smoothedWaveformValues = FloatArray(currentBandCount)
            _rawFftData.value = FloatArray(currentBandCount)
            _waveformData.value = FloatArray(currentBandCount)
        }
    }

    private fun startSimulationMode() {
        if (simulationJob?.isActive == true) return
        simulationJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive) {
                if (isPlaying) {
                    val bands = currentBandCount
                    val fftArr = FloatArray(bands)
                    val waveArr = FloatArray(bands)
                    phase += 0.12

                    val simAmp = (abs(sin(phase * 1.4) * 0.45 + sin(phase * 2.5) * 0.3 + sin(phase * 0.6) * 0.25).toFloat() * sensitivity).coerceIn(0.08f, 0.96f)
                    smoothedAmplitude = smoothedAmplitude * 0.7f + simAmp * 0.3f
                    _amplitude.value = smoothedAmplitude

                    for (i in 0 until bands) {
                        // FFT simulation
                        val harmonic1 = sin(phase * 1.5 + i * 0.35)
                        val harmonic2 = sin(phase * 0.8 - i * 0.15)
                        val harmonic3 = sin(phase * 2.2 + i * 0.6)
                        val combinedFft = abs(harmonic1 * 0.5 + harmonic2 * 0.3 + harmonic3 * 0.2).toFloat()
                        val scaledFft = (combinedFft * sensitivity).coerceIn(0.05f, 0.98f)
                        smoothedFftValues[i] = smoothedFftValues[i] * 0.7f + scaledFft * 0.3f
                        fftArr[i] = smoothedFftValues[i]

                        // Waveform amplitude simulation
                        val centerDist = abs(i - bands / 2f) / (bands / 2f)
                        val waveBase = sin(phase * 2.0 + i * 0.28) * 0.5f + cos(phase * 1.2 - i * 0.4) * 0.35f
                        val waveAmp = (abs(waveBase).toFloat() * (1.1f - centerDist * 0.4f) * sensitivity * simAmp).coerceIn(0.06f, 0.98f)
                        smoothedWaveformValues[i] = smoothedWaveformValues[i] * 0.65f + waveAmp * 0.35f
                        waveArr[i] = smoothedWaveformValues[i]
                    }
                    _rawFftData.value = fftArr
                    _waveformData.value = waveArr
                } else {
                    val bands = currentBandCount
                    val fftArr = FloatArray(bands)
                    val waveArr = FloatArray(bands)
                    smoothedAmplitude *= 0.8f
                    _amplitude.value = smoothedAmplitude
                    for (i in 0 until bands) {
                        smoothedFftValues[i] *= 0.8f
                        smoothedWaveformValues[i] *= 0.8f
                        fftArr[i] = smoothedFftValues[i]
                        waveArr[i] = smoothedWaveformValues[i]
                    }
                    _rawFftData.value = fftArr
                    _waveformData.value = waveArr
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
        } catch (_: Throwable) {}
        visualizer = null
    }
}
