package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsDataStoreTest {

  @Test
  fun `the app name is set`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Темп", appName)
  }

  @Test
  fun `the track duration is formatted`() {
    val track = com.example.data.model.Track(
      id = 1L,
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      duration = 215000L, // 3:35
      uriString = "content://test/1",
      albumArtUriString = null
    )
    assertEquals("3:35", track.formattedDuration())

    val longTrack = track.copy(duration = 3665000L) // 1:01:05
    assertEquals("1:01:05", longTrack.formattedDuration())
  }

  @Test
  fun `the auto rotate setting is persisted`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = com.example.data.local.SettingsDataStore(context)
    val initial = settings.autoRotateFlow.first()
    assertEquals(true, initial)

    settings.setAutoRotate(false)
    val afterDisable = settings.autoRotateFlow.first()
    assertEquals(false, afterDisable)

    settings.setAutoRotate(true)
    val afterEnable = settings.autoRotateFlow.first()
    assertEquals(true, afterEnable)
  }

  @Test
  fun `the crossfade settings are persisted and clamped`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = com.example.data.local.SettingsDataStore(context)
    val initialEnabled = settings.crossfadeEnabledFlow.first()
    assertEquals(true, initialEnabled)

    val initialDuration = settings.crossfadeDurationSecondsFlow.first()
    assertEquals(4, initialDuration)

    settings.setCrossfadeEnabled(false)
    assertEquals(false, settings.crossfadeEnabledFlow.first())

    settings.setCrossfadeDurationSeconds(6)
    assertEquals(6, settings.crossfadeDurationSecondsFlow.first())

    // Test bounds clamping
    settings.setCrossfadeDurationSeconds(20)
    assertEquals(10, settings.crossfadeDurationSecondsFlow.first())

    settings.setCrossfadeDurationSeconds(-5)
    assertEquals(1, settings.crossfadeDurationSecondsFlow.first())
  }
}
