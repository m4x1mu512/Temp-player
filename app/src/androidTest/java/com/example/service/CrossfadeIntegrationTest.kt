package com.example.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * Device-level integration tests: both sides of the transition are real Media3 ExoPlayer
 * instances decoding the same generated PCM fixture. These tests deliberately do not mock
 * Player.volume, playback state, media preparation, or the crossfade coroutine.
 */
@RunWith(AndroidJUnit4::class)
class CrossfadeIntegrationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var outgoing: ExoPlayer? = null
    private var incoming: ExoPlayer? = null
    private var controller: CrossfadeController? = null

    @Before
    fun setUp() {
        instrumentation.runOnMainSync {
            outgoing = ExoPlayer.Builder(context).build()
            incoming = ExoPlayer.Builder(context).build()
            // The controller needs the shared volume owner: it no longer writes
            // ExoPlayer.volume directly.
            controller = CrossfadeController(scope, PlayerVolumeCoordinator()).apply { durationSeconds = 1 }
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            controller?.release()
            outgoing?.release()
            incoming?.release()
            controller = null
            outgoing = null
            incoming = null
        }
    }

    @Test
    fun crossfadeOverlapsTwoRealPlayersAndFinishesOnIncomingPlayer() {
        val first = requireNotNull(outgoing)
        val second = requireNotNull(incoming)
        assertNotSame(first, second)

        prepareAndStart(first, "outgoing")
        prepareAndStart(second, "incoming")
        awaitCondition("both players must be decoding and playing") {
            first.isPlaying && second.isPlaying
        }

        val completed = CountDownLatch(1)
        instrumentation.runOnMainSync {
            requireNotNull(controller).startCrossfade(first, second) { completed.countDown() }
        }

        awaitCondition("incoming player should start at silence") {
            second.volume == 0f && requireNotNull(controller).isCrossfading
        }
        awaitCondition("both players must remain active during the overlap") {
            first.isPlaying && second.isPlaying &&
                second.volume > 0f && second.volume < 1f &&
                first.volume < 1f && first.volume > 0f
        }

        assertTrue("crossfade completion callback was not invoked", completed.await(3, TimeUnit.SECONDS))
        instrumentation.runOnMainSync {
            assertEquals(Player.STATE_IDLE, first.playbackState)
            assertFalse(first.playWhenReady)
            assertTrue(second.isPlaying)
            assertEquals(1f, second.volume, 0.001f)
            assertFalse(requireNotNull(controller).isCrossfading)
        }
    }

    @Test
    fun cancellingCrossfadeStopsOutgoingAndRestoresIncomingVolume() {
        val first = requireNotNull(outgoing)
        val second = requireNotNull(incoming)
        prepareAndStart(first, "cancel-outgoing")
        prepareAndStart(second, "cancel-incoming")

        val cancelled = CountDownLatch(1)
        val callbackCalls = AtomicInteger(0)
        instrumentation.runOnMainSync {
            requireNotNull(controller).startCrossfade(first, second) {
                callbackCalls.incrementAndGet()
                cancelled.countDown()
            }
        }
        awaitCondition("crossfade should begin") {
            requireNotNull(controller).isCrossfading && second.volume == 0f
        }
        awaitCondition("fade-in should make progress") { second.volume > 0f }

        instrumentation.runOnMainSync {
            requireNotNull(controller).cancelCrossfade()
            assertFalse(requireNotNull(controller).isCrossfading)
            assertEquals(Player.STATE_IDLE, first.playbackState)
            assertEquals(1f, second.volume, 0.001f)
            assertTrue(second.isPlaying)
        }
        assertTrue(
            "cancellation must invoke the completion hook exactly once",
            cancelled.await(1, TimeUnit.SECONDS)
        )
        assertEquals("completion hook must run exactly once", 1, callbackCalls.get())
    }

    private fun prepareAndStart(player: ExoPlayer, id: String) {
        val ready = CountDownLatch(1)
        instrumentation.runOnMainSync {
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) ready.countDown()
                }
            })
            val source = ProgressiveMediaSource.Factory(
                DataSource.Factory { ByteArrayDataSource(createPcmWav()) }
            ).createMediaSource(MediaItem.fromUri("test://crossfade/$id"))
            player.setMediaSource(source)
            player.prepare()
        }
        assertTrue("player did not prepare its PCM fixture", ready.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { player.play() }
        awaitCondition("player did not enter playing state") { player.isPlaying }
    }

    private fun awaitCondition(message: String, timeoutMs: Long = 3_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var satisfied = false
            instrumentation.runOnMainSync { satisfied = condition() }
            if (satisfied) return
            Thread.sleep(20)
        }
        throw AssertionError(message)
    }

    /** Three seconds of 440 Hz, 16-bit, 44.1 kHz mono PCM in a valid RIFF/WAVE container. */
    private fun createPcmWav(): ByteArray {
        val sampleRate = 44_100
        val sampleCount = sampleRate * 3
        val pcm = ByteArrayOutputStream(sampleCount * 2)
        for (index in 0 until sampleCount) {
            val sample = (sin(2.0 * PI * 440.0 * index / sampleRate) * 8_000.0).toInt()
            pcm.write(sample and 0xff)
            pcm.write((sample shr 8) and 0xff)
        }
        val audio = pcm.toByteArray()
        val wav = ByteArrayOutputStream(44 + audio.size)
        fun ascii(value: String) = wav.write(value.toByteArray(Charsets.US_ASCII))
        fun littleEndian(value: Int, bytes: Int) {
            repeat(bytes) { offset -> wav.write((value shr (8 * offset)) and 0xff) }
        }
        ascii("RIFF")
        littleEndian(36 + audio.size, 4)
        ascii("WAVE")
        ascii("fmt ")
        littleEndian(16, 4)
        littleEndian(1, 2) // PCM
        littleEndian(1, 2) // mono
        littleEndian(sampleRate, 4)
        littleEndian(sampleRate * 2, 4)
        littleEndian(2, 2) // block align
        littleEndian(16, 2)
        ascii("data")
        littleEndian(audio.size, 4)
        wav.write(audio)
        return wav.toByteArray()
    }
}
