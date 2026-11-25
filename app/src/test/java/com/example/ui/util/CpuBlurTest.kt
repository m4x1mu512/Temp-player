package com.example.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The blur stands in for the GPU blur on Android 8 to 11, where RenderEffect does not exist.
 */
class CpuBlurTest {

    private fun argb(alpha: Int = 255, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    private fun red(pixel: Int) = (pixel ushr 16) and 0xFF

    @Test
    fun `a hard edge becomes a gradient`() {
        val width = 16
        val height = 4
        val pixels = IntArray(width * height) { index ->
            if (index % width < width / 2) argb(red = 0, green = 0, blue = 0)
            else argb(red = 255, green = 255, blue = 255)
        }

        val blurred = CpuBlur.blur(pixels, width, height, radius = 3)

        // The edge in the middle must have become a mid tone instead of a hard step.
        val middle = blurred[0 * width + width / 2]
        assertTrue("the edge was not blurred: ${red(middle)}", red(middle) in 60..195)
    }

    @Test
    fun `a uniform picture stays uniform`() {
        val width = 8
        val height = 8
        val pixels = IntArray(width * height) { argb(red = 120, green = 130, blue = 140) }

        val blurred = CpuBlur.blur(pixels, width, height, radius = 4)

        blurred.forEach { pixel ->
            assertEquals(120, red(pixel))
            assertEquals(130, (pixel ushr 8) and 0xFF)
            assertEquals(140, pixel and 0xFF)
        }
    }

    @Test
    fun `the brightness is roughly preserved`() {
        val width = 16
        val height = 16
        val pixels = IntArray(width * height) { index ->
            if (index % width < width / 2) argb(red = 0, green = 0, blue = 0)
            else argb(red = 200, green = 200, blue = 200)
        }
        val before = pixels.sumOf { red(it).toLong() }

        val blurred = CpuBlur.blur(pixels, width, height, radius = 4)
        val after = blurred.sumOf { red(it).toLong() }

        val difference = kotlin.math.abs(after - before).toDouble() / before
        assertTrue("the brightness moved by $difference", difference < 0.12)
    }

    @Test
    fun `a radius below one changes nothing`() {
        val pixels = intArrayOf(argb(red = 10, green = 20, blue = 30))

        val blurred = CpuBlur.blur(pixels, 1, 1, radius = 0)

        assertEquals(pixels[0], blurred[0])
    }

    @Test
    fun `the alpha channel is blurred as well`() {
        val width = 4
        val height = 1
        val pixels = intArrayOf(
            argb(alpha = 0, red = 255, green = 255, blue = 255),
            argb(alpha = 0, red = 255, green = 255, blue = 255),
            argb(alpha = 255, red = 0, green = 0, blue = 0),
            argb(alpha = 255, red = 0, green = 0, blue = 0)
        )

        val blurred = CpuBlur.blur(pixels, width, height, radius = 1)

        val firstAlpha = (blurred[0] ushr 24) and 0xFF
        assertTrue("the transparent pixels did not stay transparent: $firstAlpha", firstAlpha < 255)
    }
}
