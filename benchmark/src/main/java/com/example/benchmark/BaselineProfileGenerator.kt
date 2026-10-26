package com.example.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the app on a real device.
 *
 * Run with: ./gradlew :benchmark:connectedDebugAndroidTest
 * The generated profile is written to benchmark/build/outputs/managed_device_android_test_additional_output/
 * by the macrobenchmark library; copy it into app/src/main/baseline-prof.txt and rebuild
 * release. That profile is what ProfileInstaller installs on Android 8-11.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        // Let the library compose, measure and draw so its classes are hot too.
        Thread.sleep(2_000)
    }
}
