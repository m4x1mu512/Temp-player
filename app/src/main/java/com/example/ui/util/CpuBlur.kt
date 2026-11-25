package com.example.ui.util

/**
 * A CPU blur for the devices that have no GPU blur.
 *
 * Android 12 (API 31) introduced RenderEffect, which is what the platform and the Backdrop
 * library use for the glass surfaces. Android 8 to 11 have no GPU blur at all, so the picture
 * behind the player is blurred on the CPU instead.
 *
 * The filter is three box passes. A box blur is not a gaussian one, but the result is scaled up
 * to fill the screen, and the difference is not visible at that size. The sliding window keeps
 * the cost at O(pixels) instead of O(pixels * radius).
 */
internal object CpuBlur {

    private const val PASSES = 3

    /**
     * Blurs ARGB pixels.
     *
     * @param pixels the pixels in the usual Android ARGB layout, row major. The array is not
     * modified.
     * @param radius the radius in pixels. A radius below one returns the input unchanged.
     * @return a new array with the blurred pixels.
     */
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        if (radius < 1 || width <= 0 || height <= 0 || pixels.size < width * height) return pixels
        var source = pixels.copyOf()
        var target = IntArray(pixels.size)
        repeat(PASSES) {
            boxBlurHorizontal(source, target, width, height, radius)
            boxBlurVertical(target, source, width, height, radius)
        }
        return source
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
        val diameter = radius * 2 + 1
        for (y in 0 until height) {
            val row = y * width
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            // The window of the first pixel reaches outside the picture on the left. Those
            // pixels are replicated from the edge, which keeps the window size constant.
            for (offset in -radius..radius) {
                val color = src[row + offset.coerceIn(0, width - 1)]
                a += (color ushr 24) and 0xFF
                r += (color ushr 16) and 0xFF
                g += (color ushr 8) and 0xFF
                b += color and 0xFF
            }
            for (x in 0 until width) {
                dst[row + x] = pack(a / diameter, r / diameter, g / diameter, b / diameter)
                val leaving = src[row + (x - radius).coerceIn(0, width - 1)]
                val entering = src[row + (x + radius + 1).coerceIn(0, width - 1)]
                a += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                r += ((entering ushr 16) and 0xFF) - ((leaving ushr 16) and 0xFF)
                g += ((entering ushr 8) and 0xFF) - ((leaving ushr 8) and 0xFF)
                b += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
        val diameter = radius * 2 + 1
        for (x in 0 until width) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            for (offset in -radius..radius) {
                val color = src[offset.coerceIn(0, height - 1) * width + x]
                a += (color ushr 24) and 0xFF
                r += (color ushr 16) and 0xFF
                g += (color ushr 8) and 0xFF
                b += color and 0xFF
            }
            for (y in 0 until height) {
                dst[y * width + x] = pack(a / diameter, r / diameter, g / diameter, b / diameter)
                val leaving = src[(y - radius).coerceIn(0, height - 1) * width + x]
                val entering = src[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                a += ((entering ushr 24) and 0xFF) - ((leaving ushr 24) and 0xFF)
                r += ((entering ushr 16) and 0xFF) - ((leaving ushr 16) and 0xFF)
                g += ((entering ushr 8) and 0xFF) - ((leaving ushr 8) and 0xFF)
                b += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private fun pack(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
