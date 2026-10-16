package com.example.ui.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Intelligent hardware detection to auto-tune visual effects, shadows, frame rates,
 * and animation complexity based on the device's capability and Android version.
 * Ensures consistent 60 FPS fluidity on older systems (e.g. Android 8.0/8.1 Oreo, API 26-27).
 */
object DevicePerformanceHelper {
    @Volatile
    private var cachedIsLowPower: Boolean? = null

    fun isLowPowerDevice(context: Context): Boolean {
        cachedIsLowPower?.let { return it }

        // Android 8.0 / 8.1 / 9 (API <= 28) have legacy RenderThread and GC implementations
        val isOldApi = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        val isLowRam = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice == true
        } catch (_: Exception) {
            false
        }
        val lowCpuCores = try {
            Runtime.getRuntime().availableProcessors() <= 4
        } catch (_: Exception) {
            false
        }

        val result = isOldApi || isLowRam || lowCpuCores
        cachedIsLowPower = result
        return result
    }

    /**
     * Adaptive polling step for audio visualizer:
     * 33ms (~30 FPS) on older / low-power hardware saves ~40% CPU cycles,
     * freeing up the frame budget for smooth 60 FPS UI animations.
     * 20ms (~50 FPS) on modern high-power devices.
     */
    fun getVisualizerDelayMs(context: Context): Long {
        return if (isLowPowerDevice(context)) 33L else 20L
    }

    /**
     * Multi-color ambient & spot shadow parameters require hardware layer API 29+ to render
     * without software fallback stalls in OpenGL.
     */
    fun shouldUseSimplifiedShadows(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || isLowPowerDevice(context)
    }

    /**
     * 3D perspective distortion (cameraDistance + rotationY) should be simplified on older systems
     * to avoid vertex buffer rebuilds.
     */
    fun shouldUseSimplifiedFlip(context: Context): Boolean {
        return isLowPowerDevice(context)
    }
}

@Composable
fun rememberIsLowPowerDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        DevicePerformanceHelper.isLowPowerDevice(context)
    }
}

@Composable
fun rememberShouldUseSimplifiedShadows(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        DevicePerformanceHelper.shouldUseSimplifiedShadows(context)
    }
}

@Composable
fun rememberShouldUseSimplifiedFlip(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        DevicePerformanceHelper.shouldUseSimplifiedFlip(context)
    }
}
