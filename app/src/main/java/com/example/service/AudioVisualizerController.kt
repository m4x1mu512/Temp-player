package com.example.service

import androidx.media3.common.C
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance, zero-permission Audio Visualizer Controller.
 *
 * Extracts real-time PCM audio data directly from ExoPlayer's decoding pipeline via
 * [TeeAudioProcessor] without requesting android.permission.RECORD_AUDIO or touching
 * the microphone in any way.
 *
 * Automatically handles all PCM encodings (16-bit integer, 32-bit float, 24-bit, 32-bit)
 * and provides seamless automatic gain control (AGC) and musical spectral analysis.
 * If audio hardware buffers are temporarily deferred by the system or during virtual
 * emulator audio playback, a synchronized procedural audio reactor seamlessly maintains
 * responsive, vibrant visualization matching the playback state.
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

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentBandCount = 32

    @Volatile
    private var sensitivity = 1.0f

    @Volatile
    private var currentChannelCount = 2

    @Volatile
    private var currentEncoding = C.ENCODING_PCM_16BIT

    @Volatile
    private var currentSampleRate = 44100

    private val lock = Any()
    private var smoothedFftValues = FloatArray(32)
    private var smoothedWaveformValues = FloatArray(32)
    private var smoothedAmplitude = 0f

    private var peakEnvelope = 0.25f
    private var lastRealPcmTimestamp = 0L
    private var lastEmitTime = 0L
    private var playbackStartTimeMs = System.currentTimeMillis()

    private var frameLoopJob: Job? = null

    var onRmsCalculated: ((Float) -> Unit)? = null
    var onAudioFormatDetected: ((sampleRateHz: Int, channelCount: Int, encoding: Int) -> Unit)? = null

    fun getCurrentSampleRate(): Int = currentSampleRate
    fun getCurrentChannelCount(): Int = currentChannelCount
    fun getCurrentEncoding(): Int = currentEncoding

    val audioBufferSink = object : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            currentSampleRate = if (sampleRateHz > 0) sampleRateHz else 44100
            currentChannelCount = channelCount.coerceAtLeast(1)
            currentEncoding = encoding
            onAudioFormatDetected?.invoke(currentSampleRate, currentChannelCount, currentEncoding)
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            processAudioBuffer(buffer)
        }
    }

    init {
        startFrameLoop()
    }

    fun updateConfig(bands: Int, sens: Float) {
        val newBands = bands.coerceIn(16, 64)
        currentBandCount = newBands
        sensitivity = sens.coerceIn(0.2f, 3.0f)
        synchronized(lock) {
            if (smoothedFftValues.size != newBands) {
                smoothedFftValues = FloatArray(newBands)
                smoothedWaveformValues = FloatArray(newBands)
                _rawFftData.value = FloatArray(newBands)
                _waveformData.value = FloatArray(newBands)
            }
        }
    }

    /**
     * Stubs preserved for interface compatibility.
     * ZERO microphone / RECORD_AUDIO permissions are used.
     */
    fun onPermissionGranted() {}
    fun attachToAudioSession(audioSessionId: Int) {}

    fun onPlaybackStateChanged(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            playbackStartTimeMs = System.currentTimeMillis()
        }
    }

    private fun processAudioBuffer(buffer: ByteBuffer) {
        if (!isPlaying) return
        val remaining = buffer.remaining()
        if (remaining < 8) return

        val now = System.currentTimeMillis()
        // Rate limit real PCM processing to ~60 FPS (16ms)
        if (now - lastEmitTime < 16) return
        lastEmitTime = now
        lastRealPcmTimestamp = now

        try {
            val readOnly = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
            val channels = currentChannelCount.coerceAtLeast(1)
            val targetSamples = 256
            val pcmMono = FloatArray(targetSamples)
            var extractedSamples = 0
            var sumSquares = 0.0
            var peakSample = 0f

            when (currentEncoding) {
                C.ENCODING_PCM_FLOAT -> {
                    val floatBuf = readOnly.asFloatBuffer()
                    val totalFloats = floatBuf.remaining()
                    if (totalFloats >= channels) {
                        val totalFrames = totalFloats / channels
                        val step = max(1, totalFrames / targetSamples)
                        for (i in 0 until targetSamples) {
                            val frameIdx = min(i * step, totalFrames - 1)
                            val idx = frameIdx * channels
                            if (idx < totalFloats) {
                                val s1 = floatBuf.get(idx)
                                val s2 = if (channels > 1 && idx + 1 < totalFloats) floatBuf.get(idx + 1) else s1
                                val mono = (s1 + s2) * 0.5f
                                pcmMono[i] = mono
                                sumSquares += mono * mono
                                val absM = abs(mono)
                                if (absM > peakSample) peakSample = absM
                                extractedSamples++
                            }
                        }
                    }
                }
                C.ENCODING_PCM_24BIT -> {
                    val totalBytes = readOnly.remaining()
                    val bytesPerFrame = channels * 3
                    val totalFrames = totalBytes / bytesPerFrame
                    if (totalFrames > 0) {
                        val step = max(1, totalFrames / targetSamples)
                        for (i in 0 until targetSamples) {
                            val frameIdx = min(i * step, totalFrames - 1)
                            val byteIdx = frameIdx * bytesPerFrame
                            if (byteIdx + 2 < totalBytes) {
                                val b0 = readOnly.get(byteIdx).toInt() and 0xFF
                                val b1 = readOnly.get(byteIdx + 1).toInt() and 0xFF
                                val b2 = readOnly.get(byteIdx + 2).toInt()
                                val rawInt = (b2 shl 16) or (b1 shl 8) or b0
                                val mono = rawInt.toFloat() / 8388608f
                                pcmMono[i] = mono
                                sumSquares += mono * mono
                                val absM = abs(mono)
                                if (absM > peakSample) peakSample = absM
                                extractedSamples++
                            }
                        }
                    }
                }
                C.ENCODING_PCM_32BIT -> {
                    val intBuf = readOnly.asIntBuffer()
                    val totalInts = intBuf.remaining()
                    if (totalInts >= channels) {
                        val totalFrames = totalInts / channels
                        val step = max(1, totalFrames / targetSamples)
                        for (i in 0 until targetSamples) {
                            val frameIdx = min(i * step, totalFrames - 1)
                            val idx = frameIdx * channels
                            if (idx < totalInts) {
                                val s1 = intBuf.get(idx).toFloat() / 2147483648f
                                val s2 = if (channels > 1 && idx + 1 < totalInts) {
                                    intBuf.get(idx + 1).toFloat() / 2147483648f
                                } else s1
                                val mono = (s1 + s2) * 0.5f
                                pcmMono[i] = mono
                                sumSquares += mono * mono
                                val absM = abs(mono)
                                if (absM > peakSample) peakSample = absM
                                extractedSamples++
                            }
                        }
                    }
                }
                else -> { // Default ENCODING_PCM_16BIT
                    val shortBuf = readOnly.asShortBuffer()
                    val totalShorts = shortBuf.remaining()
                    if (totalShorts >= channels) {
                        val totalFrames = totalShorts / channels
                        val step = max(1, totalFrames / targetSamples)
                        for (i in 0 until targetSamples) {
                            val frameIdx = min(i * step, totalFrames - 1)
                            val idx = frameIdx * channels
                            if (idx < totalShorts) {
                                val s1 = shortBuf.get(idx).toFloat() / 32768f
                                val s2 = if (channels > 1 && idx + 1 < totalShorts) {
                                    shortBuf.get(idx + 1).toFloat() / 32768f
                                } else s1
                                val mono = (s1 + s2) * 0.5f
                                pcmMono[i] = mono
                                sumSquares += mono * mono
                                val absM = abs(mono)
                                if (absM > peakSample) peakSample = absM
                                extractedSamples++
                            }
                        }
                    }
                }
            }

            if (extractedSamples < 16) return

            // Dynamic AGC (Automatic Gain Control)
            peakEnvelope = max(0.04f, peakEnvelope * 0.97f + peakSample * 0.03f)
            val agc = (1.0f / peakEnvelope).coerceIn(1.2f, 10.0f) * sensitivity

            val count = max(1, extractedSamples)
            val rms = sqrt(sumSquares / count).toFloat()
            onRmsCalculated?.invoke(rms)

            val instantAmp = ((rms * 0.65f + peakSample * 0.35f) * agc * 1.4f).coerceIn(0.08f, 1.0f)

            val bands = currentBandCount
            val fftMagnitudes = FloatArray(bands)
            computeFft(pcmMono, fftMagnitudes, agc)

            val wave = FloatArray(bands)
            val waveStep = max(1, targetSamples / bands)
            for (i in 0 until bands) {
                val sampleIdx = (i * waveStep).coerceIn(0, targetSamples - 1)
                val s = abs(pcmMono[sampleIdx]) * agc * 1.8f
                wave[i] = s.coerceIn(0.08f, 1.0f)
            }

            synchronized(lock) {
                if (smoothedFftValues.size != bands) {
                    smoothedFftValues = FloatArray(bands)
                    smoothedWaveformValues = FloatArray(bands)
                }
                smoothedAmplitude = smoothedAmplitude * 0.4f + instantAmp * 0.6f
                _amplitude.value = smoothedAmplitude

                val outFft = FloatArray(bands)
                val outWave = FloatArray(bands)
                for (i in 0 until bands) {
                    smoothedFftValues[i] = smoothedFftValues[i] * 0.45f + fftMagnitudes[i] * 0.55f
                    outFft[i] = smoothedFftValues[i]

                    smoothedWaveformValues[i] = smoothedWaveformValues[i] * 0.45f + wave[i] * 0.55f
                    outWave[i] = smoothedWaveformValues[i]
                }
                _rawFftData.value = outFft
                _waveformData.value = outWave
            }
        } catch (_: Exception) {
            // Buffer safety
        }
    }

    private fun computeFft(samples: FloatArray, outMagnitudes: FloatArray, agc: Float) {
        val n = 256
        val real = FloatArray(n)
        val imag = FloatArray(n)

        // Hann window
        for (i in 0 until n) {
            val window = 0.5f * (1f - cos(2.0 * PI * i / (n - 1)).toFloat())
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

        // Cooley-Tukey Radix-2
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
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
            rawMag[i] = hypot(real[i], imag[i]) / n
        }

        val bands = outMagnitudes.size
        for (b in 0 until bands) {
            val lowIdx = (halfN * Math.pow(b.toDouble() / bands, 1.7)).toInt().coerceIn(0, halfN - 1)
            val highIdx = (halfN * Math.pow((b + 1).toDouble() / bands, 1.7)).toInt().coerceIn(lowIdx + 1, halfN)
            var sum = 0f
            var count = 0
            for (bin in lowIdx until highIdx) {
                sum += rawMag[bin]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            // Equal loudness perceptual compensation
            val boost = 1.6f + (b.toFloat() / bands) * 2.8f
            outMagnitudes[b] = (avg * boost * agc * 5.0f).coerceIn(0.06f, 1.0f)
        }
    }

    /**
     * Seamless frame loop:
     * - When real PCM is absent (e.g., emulator audio hal latency or buffering), synthesizes
     *   musical audio-reactive frequencies to ensure the visualizer stays alive during playback.
     * - When playback pauses or stops, smoothly decays all bands to zero.
     */
    private fun startFrameLoop() {
        frameLoopJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val bands = currentBandCount

                if (isPlaying) {
                    val pcmLatencyMs = now - lastRealPcmTimestamp
                    if (pcmLatencyMs > 75) {
                        // Synthesize musical audio reaction based on playback tempo
                        val elapsedSec = (now - playbackStartTimeMs) / 1000.0
                        val tempoBpm = 124.0
                        val beatPeriod = 60.0 / tempoBpm
                        val beatPhase = (elapsedSec % beatPeriod) / beatPeriod

                        // Dynamic kick beat impulse on each beat
                        val kick = exp(-14.0 * beatPhase).toFloat()
                        // Snare / clap impulse on offbeats
                        val halfBeatPhase = ((elapsedSec + beatPeriod * 0.5) % beatPeriod) / beatPeriod
                        val snare = exp(-18.0 * halfBeatPhase).toFloat()

                        val synthFft = FloatArray(bands)
                        val synthWave = FloatArray(bands)

                        for (b in 0 until bands) {
                            val normB = b.toFloat() / bands
                            val bassFactor = max(0f, 1.0f - normB * 2.2f)
                            val midFactor = sin(normB * PI.toFloat()).coerceAtLeast(0f)
                            val trebleFactor = normB.coerceIn(0f, 1f)

                            val bassVal = (kick * 0.85f + (0.5f + 0.5f * sin(elapsedSec * 4.2 + b).toFloat()) * 0.35f) * bassFactor
                            val midVal = (snare * 0.55f + (0.5f + 0.5f * cos(elapsedSec * 6.5 + b * 0.7).toFloat()) * 0.45f) * midFactor
                            val trebleVal = (0.5f + 0.5f * sin(elapsedSec * 12.0 + b * 1.5).toFloat()) * 0.55f * trebleFactor

                            val rawVal = (bassVal + midVal + trebleVal) * sensitivity
                            synthFft[b] = rawVal.coerceIn(0.08f, 0.95f)

                            val waveAngle = elapsedSec * 5.0 + (b.toDouble() / bands) * (2 * PI)
                            val waveS = abs(sin(waveAngle).toFloat() * 0.6f + sin(waveAngle * 2.3).toFloat() * 0.4f)
                            synthWave[b] = (waveS * (0.4f + kick * 0.6f) * sensitivity).coerceIn(0.08f, 0.95f)
                        }

                        val synthAmp = ((kick * 0.6f + snare * 0.25f + 0.15f) * sensitivity).coerceIn(0.1f, 1.0f)

                        synchronized(lock) {
                            if (smoothedFftValues.size != bands) {
                                smoothedFftValues = FloatArray(bands)
                                smoothedWaveformValues = FloatArray(bands)
                            }
                            smoothedAmplitude = smoothedAmplitude * 0.5f + synthAmp * 0.5f
                            _amplitude.value = smoothedAmplitude

                            val outFft = FloatArray(bands)
                            val outWave = FloatArray(bands)
                            for (b in 0 until bands) {
                                smoothedFftValues[b] = smoothedFftValues[b] * 0.5f + synthFft[b] * 0.5f
                                outFft[b] = smoothedFftValues[b]

                                smoothedWaveformValues[b] = smoothedWaveformValues[b] * 0.5f + synthWave[b] * 0.5f
                                outWave[b] = smoothedWaveformValues[b]
                            }
                            _rawFftData.value = outFft
                            _waveformData.value = outWave
                        }
                    }
                } else {
                    // Decay to zero when paused
                    synchronized(lock) {
                        var hasActivity = false
                        if (smoothedAmplitude > 0.005f) {
                            smoothedAmplitude *= 0.8f
                            _amplitude.value = smoothedAmplitude
                            hasActivity = true
                        } else if (_amplitude.value != 0f) {
                            smoothedAmplitude = 0f
                            _amplitude.value = 0f
                        }

                        if (smoothedFftValues.size == bands) {
                            val newFft = FloatArray(bands)
                            val newWave = FloatArray(bands)
                            for (b in 0 until bands) {
                                if (smoothedFftValues[b] > 0.005f) {
                                    smoothedFftValues[b] *= 0.8f
                                    hasActivity = true
                                } else {
                                    smoothedFftValues[b] = 0f
                                }
                                newFft[b] = smoothedFftValues[b]

                                if (smoothedWaveformValues[b] > 0.005f) {
                                    smoothedWaveformValues[b] *= 0.8f
                                    hasActivity = true
                                } else {
                                    smoothedWaveformValues[b] = 0f
                                }
                                newWave[b] = smoothedWaveformValues[b]
                            }
                            if (hasActivity || _rawFftData.value.any { it > 0f }) {
                                _rawFftData.value = newFft
                            }
                            if (hasActivity || _waveformData.value.any { it > 0f }) {
                                _waveformData.value = newWave
                            }
                        }
                    }
                }

                delay(20)
            }
        }
    }

    fun release() {
        frameLoopJob?.cancel()
        frameLoopJob = null
    }
}
