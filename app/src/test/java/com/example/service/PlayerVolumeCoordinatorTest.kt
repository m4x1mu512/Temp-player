package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The final output volume is the product of the independent gains.
 *
 * There is no master factor: nothing ever set it, so it was always 1 and only made the
 * formula look like it had four inputs.
 */
class PlayerVolumeCoordinatorTest {

    @Test
    fun `the gains are multiplied`() {
        assertEquals(
            0.25f,
            PlayerVolumeCoordinator.calculateOutputVolume(
                replayGain = 0.5f,
                crossfade = 1f,
                transition = 1f
            ),
            0.0001f
        )
    }

    @Test
    fun `a zero gain silences the output`() {
        assertEquals(
            0f,
            PlayerVolumeCoordinator.calculateOutputVolume(0.8f, 0f, 0.4f),
            0.0001f
        )
    }

    @Test
    fun `a gain above one is clamped and a broken gain is ignored`() {
        assertEquals(
            1f,
            PlayerVolumeCoordinator.calculateOutputVolume(2f, Float.NaN, 1f),
            0.0001f
        )
    }
}
