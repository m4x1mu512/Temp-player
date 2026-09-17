package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playbackManager: PlaybackManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        playbackManager = PlaybackManager.getInstance(this)
        val player = playbackManager.initializePlayer()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        // Restore last track if available
        serviceScope.launch {
            try {
                val lastTrackId = playbackManager.settingsDataStore.lastTrackIdFlow.first()
                val lastPos = playbackManager.settingsDataStore.lastPositionFlow.first()
                if (lastTrackId > 0) {
                    val allTracks = playbackManager.repository.allTracks.first()
                    val track = allTracks.find { it.id == lastTrackId }
                    if (track != null) {
                        playbackManager.playTrack(
                            track = track,
                            newQueue = allTracks,
                            startPaused = true
                        )
                        playbackManager.seekTo(lastPos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playbackManager.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
