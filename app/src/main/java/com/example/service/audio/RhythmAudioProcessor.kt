package com.example.service.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Rhythm Audio Processor - Base class for real-time audio effects.
 *
 * Direct architecture modeled after cromaguy/Rhythm:
 * Processes audio samples in-place using custom DSP algorithms for zero-latency,
 * click-free, and volume-jump-free playback directly in the ExoPlayer audio pipeline.
 */
@OptIn(UnstableApi::class)
@Suppress("OVERRIDE_DEPRECATION")
abstract class RhythmAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "RhythmAudioProcessor"

        private val bufferPool = mutableListOf<ShortArray>()
        private val byteBufferPool = mutableListOf<ByteBuffer>()
        private const val MAX_POOL_SIZE = 4

        fun acquireShortArray(size: Int): ShortArray {
            synchronized(bufferPool) {
                val buffer = bufferPool.find { it.size >= size }
                if (buffer != null) {
                    bufferPool.remove(buffer)
                    return buffer
                }
            }
            return ShortArray(size)
        }

        fun releaseShortArray(buffer: ShortArray) {
            synchronized(bufferPool) {
                if (bufferPool.size < MAX_POOL_SIZE) {
                    bufferPool.add(buffer)
                }
            }
        }

        fun acquireByteBuffer(size: Int): ByteBuffer {
            synchronized(byteBufferPool) {
                val buffer = byteBufferPool.find { it.capacity() >= size }
                if (buffer != null) {
                    byteBufferPool.remove(buffer)
                    buffer.clear()
                    return buffer
                }
            }
            return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        }

        fun releaseByteBuffer(buffer: ByteBuffer) {
            synchronized(byteBufferPool) {
                if (byteBufferPool.size < MAX_POOL_SIZE) {
                    buffer.clear()
                    byteBufferPool.add(buffer)
                }
            }
        }
    }

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    protected var sampleRate: Int = 44100
    protected var channelCount: Int = 2
    protected var encoding: Int = C.ENCODING_PCM_16BIT

    abstract fun processSamples(samples: ShortArray, sampleCount: Int)
    abstract fun isEnabled(): Boolean
    open fun isBypassed(): Boolean = !isEnabled()

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            this.outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        this.inputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate
        this.channelCount = inputAudioFormat.channelCount
        this.encoding = inputAudioFormat.encoding
        this.outputAudioFormat = inputAudioFormat

        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET &&
                encoding == C.ENCODING_PCM_16BIT
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }

        val size = inputBuffer.remaining()
        if (buffer.capacity() < size) {
            if (buffer !== AudioProcessor.EMPTY_BUFFER) releaseByteBuffer(buffer)
            buffer = acquireByteBuffer(size)
        }
        buffer.clear()
        buffer.put(inputBuffer)
        buffer.flip()
        outputBuffer = buffer

        if (isBypassed()) {
            return
        }

        val sampleCount = size / 2
        val samples = acquireShortArray(sampleCount)
        try {
            buffer.position(0)
            buffer.asShortBuffer().get(samples, 0, sampleCount)

            processSamples(samples, sampleCount)

            buffer.position(0)
            buffer.asShortBuffer().put(samples, 0, sampleCount)
        } finally {
            releaseShortArray(samples)
        }

        buffer.position(0)
        buffer.limit(size)
        outputBuffer = buffer
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
    }
}
