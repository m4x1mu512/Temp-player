package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The band labels come from the equalizer's own band frequencies:
 * 60, 230, 910, 3600 and 14000 Hz.
 *
 * Integer division printed "3 кГц" for the 3600 Hz band, which is the band most users
 * recognise as "3.6 kHz".
 */
class EqualizerFrequencyLabelTest {

    @Test
    fun `the 3600 Hz band keeps one decimal`() {
        assertEquals("3.6 кГц", formatFrequencyLabel(3600))
    }

    @Test
    fun `the 14000 Hz band is labelled 14 kHz`() {
        assertEquals("14 кГц", formatFrequencyLabel(14000))
    }

    @Test
    fun `frequencies below 1000 Hz stay in Hz`() {
        assertEquals("60 Гц", formatFrequencyLabel(60))
        assertEquals("230 Гц", formatFrequencyLabel(230))
        assertEquals("910 Гц", formatFrequencyLabel(910))
    }

    @Test
    fun `a whole kilohertz is written without decimals`() {
        assertEquals("1 кГц", formatFrequencyLabel(1000))
        assertEquals("2 кГц", formatFrequencyLabel(2000))
    }

    @Test
    fun `a fractional kilohertz keeps one decimal`() {
        assertEquals("2.3 кГц", formatFrequencyLabel(2300))
        assertEquals("14.1 кГц", formatFrequencyLabel(14100))
    }
}
