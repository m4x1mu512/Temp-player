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
}

