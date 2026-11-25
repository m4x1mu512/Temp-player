package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackServiceTest {

  @Test
  fun `the service starts and plays a track`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = org.robolectric.Robolectric.buildService(com.example.service.PlaybackService::class.java).create()
    val pm = com.example.service.PlaybackManager.getInstance(context)
    val track = com.example.data.model.Track(
      id = 1L,
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      duration = 215000L,
      uriString = "https://example.com/test.mp3",
      albumArtUriString = null
    )
    pm.playTrack(track)

    // Simulate audio visualizer RMS callback from background thread
    val thread = Thread {
      repeat(21) {
        pm.visualizerController.onRmsCalculated?.invoke(0.3f)
      }
    }
    thread.start()
    thread.join()

    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    controller.destroy()
  }
}
