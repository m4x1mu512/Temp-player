package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import androidx.media3.exoplayer.audio.AudioOffloadSupport
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

    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playbackManager: PlaybackManager

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playbackManager.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Focus regained
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                playbackManager.pause()
            }
        }
    }

    companion object {
        private const val TAG = "TempPlayer"
        const val NOTIFICATION_CHANNEL_ID = "temp_playback_channel"
    }

    @OptIn(UnstableApi::class)
    private fun buildExoPlayer(): ExoPlayer {
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
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioOffloadSupportProvider { _, _ -> AudioOffloadSupport.DEFAULT_UNSUPPORTED }
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(playbackManager.visualizerController.audioBufferSink)))
                    .build()
            }
        }

        return ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, false) // Controlled at service level for simultaneous crossfade
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    private fun requestSystemAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AndroidAudioAttributes.Builder()
                        .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                        .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build().also { audioFocusRequest = it }
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonSystemAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun createForwardingPlayer(player: Player): ForwardingPlayer {
        return object : ForwardingPlayer(player) {
            override fun play() {
                Log.d(TAG, "play")
                requestSystemAudioFocus()
                playbackManager.play()
            }

            override fun pause() {
                Log.d(TAG, "pause")
                playbackManager.pause()
            }

            override fun stop() {
                Log.d(TAG, "stop")
                playbackManager.stop()
                abandonSystemAudioFocus()
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
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PlaybackService onCreate")

        playbackManager = PlaybackManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        createNotificationChannel()

        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(noisyReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register noisy receiver: ${e.message}")
        }

        playbackManager.onRequestAudioFocus = {
            requestSystemAudioFocus()
        }

        Log.d(TAG, "Creating dual ExoPlayers for crossfade support")
        val p1 = buildExoPlayer()
        val p2 = buildExoPlayer()
        player1 = p1
        player2 = p2

        playbackManager.attachPlayers(p1, p2)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, createForwardingPlayer(p1))
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        playbackManager.onActivePlayerChanged = { newActivePlayer ->
            try {
                val currentForwarding = mediaSession?.player as? ForwardingPlayer
                if (currentForwarding?.wrappedPlayer !== newActivePlayer) {
                    mediaSession?.setPlayer(createForwardingPlayer(newActivePlayer))
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error updating player in mediaSession: ${t.message}")
            }
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
        )

        // Restore last track and queue if available and no current track in PlaybackManager
        if (playbackManager.currentTrack.value == null) {
            serviceScope.launch {
                try {
                    playbackManager.restoreSavedQueueAndTrack()
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
        val active = playbackManager.activePlayer
        if (active == null || (!active.playWhenReady && !active.isPlaying)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        try {
            unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        abandonSystemAudioFocus()
        serviceScope.cancel()
        playbackManager.detachPlayer()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        Log.d(TAG, "RELEASING PLAYERS IN SERVICE")
        player1?.release()
        player2?.release()
        player1 = null
        player2 = null
        super.onDestroy()
    }
}
