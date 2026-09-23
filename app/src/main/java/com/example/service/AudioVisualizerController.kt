package com.example.service

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Modern Audio Visualizer Controller that extracts real-time PCM audio data
 * directly from ExoPlayer playback via TeeAudioProcessor without using the microphone
 * or requesting any RECORD_AUDIO permissions.
 */
@UnstableApi
class AudioVisualizerController(
    private val scope: CoroutineScope
) {
    private val _rawFftData = MutableStateFlow(FloatArray(32))
    val rawFftData: StateFlow<FloatArray> = _rawFftData.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(32))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var isPlaying = false
    private var currentBandCount = 32
    private var sensitivity = 1.0f
    private var currentChannelCount = 2

    private var smoothedFftValues = FloatArray(32)
    private var smoothedWaveformValues = FloatArray(32)
    private var smoothedAmplitude = 0f

    private var lastBufferTimestamp = 0L
    private var lastEmitTime = 0L
    private var decayJob: Job? = null

    var onRmsCalculated: ((Float) -> Unit)? = null

    val audioBufferSink = object : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            currentChannelCount = channelCount.coerceAtLeast(1)
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            processAudioBuffer(buffer)
        }
    }

    init {
        startDecayMonitor()
    }

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

    /**
     * Stubs kept for interface compatibility with callers.
     * No microphone or audio session permissions are used.
     */
    fun onPermissionGranted() {}
    fun attachToAudioSession(audioSessionId: Int) {}

    fun onPlaybackStateChanged(playing: Boolean) {
        isPlaying = playing
        if (!playing) {
            triggerDecay()
        }
    }

    private fun processAudioBuffer(buffer: ByteBuffer) {
        if (!isPlaying) return
        val remaining = buffer.remaining()
        if (remaining < 16) return

        val now = System.currentTimeMillis()
        lastBufferTimestamp = now

        // Throttle updates to ~60 FPS (16ms) to conserve CPU
        if (now - lastEmitTime < 16) return
        lastEmitTime = now

        try {
            val readOnly = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
            val shorts = readOnly.asShortBuffer()
            val totalShorts = shorts.remaining()
            if (totalShorts < 32) return

            val channels = currentChannelCount.coerceAtLeast(1)
            val targetSamples = 256
            val step = (totalShorts / (targetSamples * channels)).coerceAtLeast(1)
            val pcmMono = FloatArray(targetSamples)

            var sumSquares = 0.0
            var peakSample = 0f

            for (i in 0 until targetSamples) {
                val idx = i * step * channels
                if (idx < totalShorts) {
                    val s1 = shorts.get(idx).toFloat() / 32768f
                    val s2 = if (channels > 1 && idx + 1 < totalShorts) {
                        shorts.get(idx + 1).toFloat() / 32768f
                    } else {
                        s1
                    }
                    val mono = (s1 + s2) * 0.5f
                    pcmMono[i] = mono
                    sumSquares += mono * mono
                    val absM = abs(mono)
                    if (absM > peakSample) peakSample = absM
                }
            }

            // 1. Amplitude (RMS + peak hybrid)
            val rms = sqrt(sumSquares / targetSamples).toFloat()
            onRmsCalculated?.invoke(rms)
            val instantAmp = ((rms * 0.65f + peakSample * 0.35f) * sensitivity * 1.8f).coerceIn(0f, 1f)
            smoothedAmplitude = smoothedAmplitude * 0.6f + instantAmp * 0.4f
            _amplitude.value = smoothedAmplitude

            // 2. Waveform (segmented into currentBandCount points)
            val bands = currentBandCount
            val wave = FloatArray(bands)
            val waveStep = (targetSamples / bands).coerceAtLeast(1)
            for (i in 0 until bands) {
                val sampleIdx = (i * waveStep).coerceIn(0, targetSamples - 1)
                val s = abs(pcmMono[sampleIdx]) * sensitivity * 2.0f
                val clamped = s.coerceIn(0.04f, 1.0f)
                smoothedWaveformValues[i] = smoothedWaveformValues[i] * 0.5f + clamped * 0.5f
                wave[i] = smoothedWaveformValues[i]
            }
            _waveformData.value = wave

            // 3. Spectrum / FFT
            val fftMagnitudes = FloatArray(bands)
            computeFft(pcmMono, fftMagnitudes)
            for (i in 0 until bands) {
                val scaled = (fftMagnitudes[i] * sensitivity).coerceIn(0.03f, 1.0f)
                smoothedFftValues[i] = smoothedFftValues[i] * 0.55f + scaled * 0.45f
                fftMagnitudes[i] = smoothedFftValues[i]
            }
            _rawFftData.value = fftMagnitudes
        } catch (_: Exception) {
            // Buffer concurrent modification safety
        }
    }

    private fun computeFft(samples: FloatArray, outMagnitudes: FloatArray) {
        val n = 256
        val real = FloatArray(n)
        val imag = FloatArray(n)

        // Apply Hann window
        for (i in 0 until n) {
            val window = 0.5f * (1f - cos(2.0 * Math.PI * i / (n - 1)).toFloat())
            real[i] = if (i < samples.size) samples[i] * window else 0f
            imag[i] = 0f
        }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey Radix-2 FFT
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * Math.PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }

        val halfN = n shr 1
        val rawMag = FloatArray(halfN)
        for (i in 0 until halfN) {
            rawMag[i] = kotlin.math.hypot(real[i], imag[i]) / n
        }

        // Map the 128 bins into bands on a logarithmic scale with equal loudness compensation
        val bands = outMagnitudes.size
        for (b in 0 until bands) {
            val lowIdx = (halfN * Math.pow(b.toDouble() / bands, 1.8)).toInt().coerceIn(0, halfN - 1)
            val highIdx = (halfN * Math.pow((b + 1).toDouble() / bands, 1.8)).toInt().coerceIn(lowIdx + 1, halfN)
            var sum = 0f
            var count = 0
            for (bin in lowIdx until highIdx) {
                sum += rawMag[bin]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            val trebleBoost = 1.0f + (b.toFloat() / bands) * 1.6f
            outMagnitudes[b] = (avg * trebleBoost * 8.5f).coerceIn(0.02f, 1.0f)
        }
    }

    private fun startDecayMonitor() {
        decayJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!isPlaying || (now - lastBufferTimestamp > 120)) {
                    triggerDecay()
                }
                delay(35)
            }
        }
    }

    private fun triggerDecay() {
        if (smoothedAmplitude > 0.005f) {
            smoothedAmplitude *= 0.75f
            _amplitude.value = smoothedAmplitude
        } else {
            smoothedAmplitude = 0f
            _amplitude.value = 0f
        }

        val bands = currentBandCount
        var hasActiveFft = false
        var hasActiveWave = false
        val newFft = FloatArray(bands)
        val newWave = FloatArray(bands)

        for (i in 0 until bands) {
            if (smoothedFftValues[i] > 0.005f) {
                smoothedFftValues[i] *= 0.75f
                hasActiveFft = true
            } else {
                smoothedFftValues[i] = 0f
            }
            newFft[i] = smoothedFftValues[i]

            if (smoothedWaveformValues[i] > 0.005f) {
                smoothedWaveformValues[i] *= 0.75f
                hasActiveWave = true
            } else {
                smoothedWaveformValues[i] = 0f
            }
            newWave[i] = smoothedWaveformValues[i]
        }

        if (hasActiveFft || _rawFftData.value.any { it > 0f }) {
            _rawFftData.value = newFft
        }
        if (hasActiveWave || _waveformData.value.any { it > 0f }) {
            _waveformData.value = newWave
        }
    }

    fun release() {
        decayJob?.cancel()
        decayJob = null
    }
}
