package com.example

import com.example.service.CrossfadeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test crossfade controller trigger conditions`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val controller = CrossfadeController(scope)

        assertEquals(true, controller.isEnabled)
        assertEquals(4, controller.durationSeconds)
        assertEquals(4000L, controller.durationMs)

        // Test duration clamping
        controller.durationSeconds = 15
        assertEquals(10, controller.durationSeconds)
        controller.durationSeconds = 0
        assertEquals(1, controller.durationSeconds)
        controller.durationSeconds = 4

        // 3-minute track (180_000ms)
        val totalDuration = 180_000L

        // At beginning of track: should not trigger
        assertFalse(
            controller.shouldTriggerCrossfade(
                currentPositionMs = 50_000L,
                totalDurationMs = totalDuration,
                hasNextTrack = true
            )
        )

        // When disabled: should not trigger even near end
        controller.isEnabled = false
        assertFalse(
            controller.shouldTriggerCrossfade(
                currentPositionMs = 178_000L, // 2s remaining
                totalDurationMs = totalDuration,
                hasNextTrack = true
            )
        )
        controller.isEnabled = true

        // When there is no next track: should not trigger
        assertFalse(
            controller.shouldTriggerCrossfade(
                currentPositionMs = 178_000L,
                totalDurationMs = totalDuration,
                hasNextTrack = false
            )
        )

        // When within crossfade window (e.g. 3.5s remaining <= 4s window): should trigger
        assertTrue(
            controller.shouldTriggerCrossfade(
                currentPositionMs = 176_500L, // 3.5s remaining
                totalDurationMs = totalDuration,
                hasNextTrack = true
            )
        )

        // For a very short track (< 6s): should not trigger to avoid premature fade
        assertFalse(
            controller.shouldTriggerCrossfade(
                currentPositionMs = 3_000L,
                totalDurationMs = 5_000L,
                hasNextTrack = true
            )
        )
    }

    @Test
    fun `test audio visualizer controller processes 16bit and float buffers without permissions`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val visualizer = com.example.service.AudioVisualizerController(scope)

        assertEquals(32, visualizer.rawFftData.value.size)
        assertEquals(32, visualizer.waveformData.value.size)

        // Config band count update
        visualizer.updateConfig(bands = 48, sens = 1.5f)
        assertEquals(48, visualizer.rawFftData.value.size)
        assertEquals(48, visualizer.waveformData.value.size)

        // Playback state
        visualizer.onPlaybackStateChanged(true)

        // Test 16-bit PCM buffer
        visualizer.audioBufferSink.flush(44100, 2, androidx.media3.common.C.ENCODING_PCM_16BIT)
        val byteBuf16 = java.nio.ByteBuffer.allocateDirect(1024).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val shortBuf = byteBuf16.asShortBuffer()
        for (i in 0 until 512) {
            shortBuf.put((kotlin.math.sin(i * 0.1) * 16000).toInt().toShort())
        }
        visualizer.audioBufferSink.handleBuffer(byteBuf16)

        assertTrue(visualizer.amplitude.value >= 0f)
        assertEquals(48, visualizer.rawFftData.value.size)

        // Test 32-bit Float PCM buffer
        visualizer.audioBufferSink.flush(48000, 2, androidx.media3.common.C.ENCODING_PCM_FLOAT)
        val byteBufFloat = java.nio.ByteBuffer.allocateDirect(2048).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val floatBuf = byteBufFloat.asFloatBuffer()
        for (i in 0 until 512) {
            floatBuf.put((kotlin.math.sin(i * 0.15) * 0.8).toFloat())
        }
        visualizer.audioBufferSink.handleBuffer(byteBufFloat)

        assertTrue(visualizer.amplitude.value >= 0f)
        assertEquals(48, visualizer.rawFftData.value.size)

        // Clean release
        visualizer.release()
    }

    @Test
    fun `test rhythm reverb processor maintains volume and does not surge on presets`() {
        val reverb = com.example.service.audio.RhythmReverbProcessor()
        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        reverb.configure(format)

        assertEquals(false, reverb.isEnabled())
        assertEquals(com.example.data.model.ReverbPreset.NONE, reverb.getPreset())

        // Test with a stereo test signal
        val sampleCount = 512
        val inputSamples = ShortArray(sampleCount) { i ->
            (kotlin.math.sin(i * 0.05) * 16000).toInt().toShort()
        }

        // Initially bypassed
        val initialCopy = inputSamples.clone()
        reverb.processSamples(initialCopy, sampleCount)
        // Since disabled, samples should be identical
        for (i in 0 until sampleCount) {
            assertEquals(inputSamples[i], initialCopy[i])
        }

        // Enable Reverb with various presets
        for (preset in com.example.data.model.ReverbPreset.entries) {
            reverb.setEnabled(preset != com.example.data.model.ReverbPreset.NONE)
            reverb.setPreset(preset)

            val testSamples = inputSamples.clone()
            reverb.processSamples(testSamples, sampleCount)

            // Verify samples remain within 16-bit PCM bounds and no NaN / infinity
            var maxAmplitude = 0
            for (s in testSamples) {
                val amp = kotlin.math.abs(s.toInt())
                if (amp > maxAmplitude) maxAmplitude = amp
            }
            // Max amplitude should not clip or explode (<= 32767)
            assertTrue("Max amplitude $maxAmplitude must be within PCM range", maxAmplitude <= 32767)
            // Volume shouldn't explode compared to original peak of 16000 (must be < 28000)
            assertTrue("Loudness must not surge uncontrollably (got $maxAmplitude)", maxAmplitude < 28000)
        }

        // Test parent-child synchronization
        val childReverb = com.example.service.audio.RhythmReverbProcessor()
        childReverb.setParent(reverb)
        reverb.setPreset(com.example.data.model.ReverbPreset.MEDIUM_HALL)
        reverb.setEnabled(true)

        assertTrue(childReverb.isEnabled())
        assertEquals(com.example.data.model.ReverbPreset.MEDIUM_HALL, childReverb.getPreset())
    }
}

