package com.example.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timings while scrolling the library - the metric that tells whether the list is
 * actually smooth on a given device, rather than whether startup is fast.
 *
 * Run with: ./gradlew :benchmark:connectedDebugAndroidTest
 * Look at frameDurationCpuMs (p50/p90/p95) and frameOverrunMs in the report.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollLibrary() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            // Let the first frame and the first batch of cover images settle.
            Thread.sleep(3_000)
        }
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val width = device.displayWidth
        val top = (device.displayHeight * 0.25f).toInt()
        val bottom = (device.displayHeight * 0.75f).toInt()
        val centerX = width / 2

        repeat(3) {
            device.swipe(centerX, bottom, centerX, top, 20)
            Thread.sleep(400)
            device.swipe(centerX, top, centerX, bottom, 20)
            Thread.sleep(400)
        }
    }
}
