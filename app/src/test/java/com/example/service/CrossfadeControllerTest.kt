package com.example.service

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The crossfade completion callback drives the rest of the playback state: the outgoing
 * player is stopped, the incoming one gets its volume back and the track that is now
 * current is restored.
 *
 * It must fire exactly once - never twice and never zero times. Twice would tear the
 * transition down again after it finished, zero times would leave the outgoing player
 * half faded out.
 *
 * The players are only needed as two distinct identities here. The crossfade itself runs
 * on a background thread, where ExoPlayer refuses the volume writes, so the transition
 * ends early - which is exactly the situation the callback has to survive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrossfadeControllerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var controller: CrossfadeController
    private var outgoing: ExoPlayer? = null
    private var incoming: ExoPlayer? = null

    @Before
    fun setUp() {
        controller = CrossfadeController(scope, PlayerVolumeCoordinator())
        outgoing = ExoPlayer.Builder(context).build()
        incoming = ExoPlayer.Builder(context).build()
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        controller.release()
        outgoing?.release()
        incoming?.release()
        outgoing = null
        incoming = null
    }

    @Test
    fun `the same player on both sides completes immediately`() {
        val player = requireNotNull(outgoing)
        val calls = AtomicInteger(0)

        controller.startCrossfade(player, player) { calls.incrementAndGet() }

        assertEquals(1, calls.get())
    }

    @Test
    fun `a cancelled crossfade completes exactly once`() {
        val calls = AtomicInteger(0)

        controller.startCrossfade(requireNotNull(outgoing), requireNotNull(incoming)) {
            calls.incrementAndGet()
        }
        controller.cancelCrossfade()

        assertEquals(1, calls.get())
    }

    @Test
    fun `a finished crossfade completes exactly once`() {
        val calls = AtomicInteger(0)
        val completed = CountDownLatch(1)
        controller.durationSeconds = 1

        controller.startCrossfade(requireNotNull(outgoing), requireNotNull(incoming)) {
            calls.incrementAndGet()
            completed.countDown()
        }

        assertTrue(
            "the crossfade did not finish in time",
            completed.await(20, TimeUnit.SECONDS)
        )
        // Give a second, wrongly fired callback a chance to show up.
        Thread.sleep(200)
        assertEquals(1, calls.get())
    }
}
