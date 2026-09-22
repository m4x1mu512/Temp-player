package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.DefaultMediaNotificationProvider
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

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playbackManager: PlaybackManager

    companion object {
        private const val TAG = "TempPlayer"
        const val NOTIFICATION_CHANNEL_ID = "temp_playback_channel"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PlaybackService onCreate")

        playbackManager = PlaybackManager.getInstance(this)

        createNotificationChannel()

        Log.d(TAG, "Creating ExoPlayer")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(playbackManager.visualizerController.audioBufferSink)))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true) // Audio focus enabled, pauses on transient focus loss
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer = player

        // Connect player to PlaybackManager so UI & StateFlows observe real state
        playbackManager.attachPlayer(player)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ForwardingPlayer delegates media session commands (notification, lock screen, bluetooth)
        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun play() {
                Log.d(TAG, "play")
                playbackManager.play()
            }

            override fun pause() {
                Log.d(TAG, "pause")
                playbackManager.pause()
            }

            override fun stop() {
                Log.d(TAG, "stop")
                playbackManager.stop()
            }

            override fun seekToNext() {
                Log.d(TAG, "next")
                playbackManager.nextTrack(autoPlayIfPaused = true)
            }

            override fun seekToNextMediaItem() {
                Log.d(TAG, "next")
                playbackManager.nextTrack(autoPlayIfPaused = true)
            }

            override fun seekToPrevious() {
                Log.d(TAG, "previous")
                playbackManager.previousTrack(autoPlayIfPaused = true)
            }

            override fun seekToPreviousMediaItem() {
                Log.d(TAG, "previous")
                playbackManager.previousTrack(autoPlayIfPaused = true)
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_STOP,
                    Player.COMMAND_PLAY_PAUSE -> true
                    else -> super.isCommandAvailable(command)
                }
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
        )

        // Restore last track if available and no current track in PlaybackManager
        if (playbackManager.currentTrack.value == null) {
            serviceScope.launch {
                try {
                    val lastTrackId = playbackManager.settingsDataStore.lastTrackIdFlow.first()
                    val lastPos = playbackManager.settingsDataStore.lastPositionFlow.first()
                    if (lastTrackId > 0 && playbackManager.currentTrack.value == null) {
                        val allTracks = playbackManager.repository.allTracks.first()
                        val track = allTracks.find { it.id == lastTrackId }
                        if (track != null && playbackManager.currentTrack.value == null) {
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
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Воспроизведение музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление и управление воспроизведением плеера «Темп»"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved")
        val player = exoPlayer
        if (player == null || (!player.playWhenReady && !player.isPlaying)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()
        val player = exoPlayer
        playbackManager.detachPlayer()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        Log.d(TAG, "PLAYER RELEASE IN SERVICE")
        player?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
