package com.example.service

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AudioTrackSpecs
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackManager private constructor(private val context: Context) {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val database = AppDatabase.getInstance(context)
    val repository = MusicRepository(context, database)
    val settingsDataStore = SettingsDataStore(context)

    // Dual-player architecture for seamless crossfading
    private var playerA: ExoPlayer? = null
    private var playerB: ExoPlayer? = null
    private var activePlayerIndex: Int = 0 // 0 -> playerA, 1 -> playerB

    val activePlayer: ExoPlayer?
        get() = if (activePlayerIndex == 0) playerA else playerB

    val secondaryPlayer: ExoPlayer?
        get() = if (activePlayerIndex == 0) playerB else playerA

    val exoPlayer: ExoPlayer?
        get() = activePlayer

    var onActivePlayerChanged: ((ExoPlayer) -> Unit)? = null
    var onRequestAudioFocus: (() -> Unit)? = null

    val visualizerController = AudioVisualizerController(serviceScope)
    val equalizerController = EqualizerController()
    val replayGainController = ReplayGainController(context)
    val crossfadeController = CrossfadeController(serviceScope)

    // Playback States
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _sleepTimerRemainingMillis = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMillis: StateFlow<Long?> = _sleepTimerRemainingMillis.asStateFlow()

    private val _sleepTimerMode = MutableStateFlow(0) // 0 = off, -1 = end of track, > 0 minutes
    val sleepTimerMode: StateFlow<Int> = _sleepTimerMode.asStateFlow()

    private val _equalizerBands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val equalizerBands: StateFlow<List<EqualizerBand>> = _equalizerBands.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(EqualizerPreset.FLAT)
    val equalizerPreset: StateFlow<EqualizerPreset> = _equalizerPreset.asStateFlow()

    private val _isEqualizerEnabled = MutableStateFlow(true)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _isReplayGainEnabled = MutableStateFlow(true)
    val isReplayGainEnabled: StateFlow<Boolean> = _isReplayGainEnabled.asStateFlow()

    private val _isCrossfadeEnabled = MutableStateFlow(true)
    val isCrossfadeEnabled: StateFlow<Boolean> = _isCrossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(4)
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _trackAudioSpecs = MutableStateFlow<AudioTrackSpecs?>(null)
    val trackAudioSpecs: StateFlow<AudioTrackSpecs?> = _trackAudioSpecs.asStateFlow()

    val visualizerData: StateFlow<FloatArray> get() = visualizerController.rawFftData
    val visualizerWaveform: StateFlow<FloatArray> get() = visualizerController.waveformData
    val audioAmplitude: StateFlow<Float> get() = visualizerController.amplitude

    fun onAudioPermissionGranted() {
        visualizerController.onPermissionGranted()
    }

    private var positionProgressJob: Job? = null
    private var sleepCountDownTimer: CountDownTimer? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    init {
        visualizerController.onRmsCalculated = { rms ->
            serviceScope.launch {
                try {
                    replayGainController.onAudioBufferRms(rms)
                } catch (t: Throwable) {
                    Log.w("PlaybackManager", "Error in onAudioBufferRms: ${t.message}")
                }
            }
        }

        visualizerController.onAudioFormatDetected = { sampleRate, channels, encoding ->
            serviceScope.launch {
                try {
                    val currentSpecs = _trackAudioSpecs.value
            val bitDepth = when (encoding) {
                C.ENCODING_PCM_24BIT -> 24
                C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
                else -> 16
            }
            if (currentSpecs != null) {
                _trackAudioSpecs.value = currentSpecs.copy(
                    sampleRateHz = if (sampleRate > 0) sampleRate else currentSpecs.sampleRateHz,
                    channelCount = if (channels > 0) channels else currentSpecs.channelCount,
                    bitDepth = if (bitDepth > 0) bitDepth else currentSpecs.bitDepth
                )
            }
        } catch (t: Throwable) {
            Log.w("PlaybackManager", "Error in onAudioFormatDetected: ${t.message}")
        }
    }
}

        serviceScope.launch {
            // Restore settings
            _repeatMode.value = settingsDataStore.repeatModeFlow.first()
            _isShuffle.value = settingsDataStore.shuffleEnabledFlow.first()
            _isEqualizerEnabled.value = settingsDataStore.eqEnabledFlow.first()
            _equalizerPreset.value = settingsDataStore.eqPresetFlow.first()
            _isReplayGainEnabled.value = settingsDataStore.replayGainEnabledFlow.first()
            replayGainController.setEnabled(_isReplayGainEnabled.value)

            _isCrossfadeEnabled.value = settingsDataStore.crossfadeEnabledFlow.first()
            _crossfadeDurationSeconds.value = settingsDataStore.crossfadeDurationSecondsFlow.first()
            crossfadeController.isEnabled = _isCrossfadeEnabled.value
            crossfadeController.durationSeconds = _crossfadeDurationSeconds.value

            launch {
                combine(
                    settingsDataStore.visualizerBandsFlow,
                    settingsDataStore.visualizerSensitivityFlow
                ) { bands, sens ->
                    visualizerController.updateConfig(bands, sens)
                }.collect()
            }

            restoreSavedQueueAndTrack()
        }
    }

    private data class PendingPlay(val track: Track, val startPaused: Boolean)
    private var pendingPlayTrack: PendingPlay? = null

    private fun createPlayerListener(isPlayerA: Boolean) = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val isThisPlayerActive = (isPlayerA && activePlayerIndex == 0) || (!isPlayerA && activePlayerIndex == 1)
            if (isThisPlayerActive) {
                _isPlaying.value = isPlaying
                visualizerController.onPlaybackStateChanged(isPlaying)
                if (isPlaying) {
                    startPositionTracking()
                } else if (!crossfadeController.isCrossfading) {
                    stopPositionTracking()
                    saveCurrentState()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isThisPlayerActive = (isPlayerA && activePlayerIndex == 0) || (!isPlayerA && activePlayerIndex == 1)
            val player = if (isPlayerA) playerA else playerB

            if (isThisPlayerActive && player != null) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val realDuration = player.duration
                        if (realDuration > 0) {
                            _duration.value = realDuration
                            // Synchronize real duration with database & current track
                            _currentTrack.value?.let { track ->
                                if (track.duration != realDuration) {
                                    _currentTrack.value = track.copy(duration = realDuration)
                                    serviceScope.launch {
                                        repository.updateTrackDuration(track.id, realDuration)
                                    }
                                }
                            }
                        }
                        updateAudioSpecsFromPlayer(player)

                        val currentSessionId = player.audioSessionId
                        if (currentSessionId > 0 && currentSessionId != currentAudioSessionId) {
                            currentAudioSessionId = currentSessionId
                            visualizerController.attachToAudioSession(currentSessionId)
                            replayGainController.attachToAudioSession(currentSessionId, _isReplayGainEnabled.value)
                            serviceScope.launch {
                                val savedLevels = settingsDataStore.eqLevelsFlow.first()
                                equalizerController.attachToAudioSession(currentSessionId, savedLevels, _isEqualizerEnabled.value)
                                _equalizerBands.value = equalizerController.getBands()
                                equalizerController.applyPreset(_equalizerPreset.value)
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (!crossfadeController.isCrossfading) {
                            handleTrackEnded()
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onMetadata(metadata: Metadata) {
            val isThisPlayerActive = (isPlayerA && activePlayerIndex == 0) || (!isPlayerA && activePlayerIndex == 1)
            if (isThisPlayerActive) {
                replayGainController.onMetadata(metadata)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val isThisPlayerActive = (isPlayerA && activePlayerIndex == 0) || (!isPlayerA && activePlayerIndex == 1)
            val player = if (isPlayerA) playerA else playerB
            if (isThisPlayerActive && player != null) {
                updateAudioSpecsFromPlayer(player)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val isThisPlayerActive = (isPlayerA && activePlayerIndex == 0) || (!isPlayerA && activePlayerIndex == 1)
            if (isThisPlayerActive) {
                _errorMessage.value = "Не удалось воспроизвести файл: ${error.localizedMessage ?: "ошибка декодирования"}"
                _isPlaying.value = false
                visualizerController.onPlaybackStateChanged(false)
            }
        }
    }

    private val playerAListener = createPlayerListener(isPlayerA = true)
    private val playerBListener = createPlayerListener(isPlayerA = false)

    fun startServiceIfNeeded() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start PlaybackService", e)
        }
    }

    fun attachPlayers(player1: ExoPlayer, player2: ExoPlayer? = null) {
        if (playerA === player1 && playerB === player2) return

        playerA?.removeListener(playerAListener)
        playerB?.removeListener(playerBListener)

        playerA = player1
        playerB = player2
        activePlayerIndex = 0

        player1.addListener(playerAListener)
        player2?.addListener(playerBListener)

        replayGainController.attachPlayer(player1)

        val currentSessionId = player1.audioSessionId
        if (currentSessionId > 0 && currentSessionId != currentAudioSessionId) {
            currentAudioSessionId = currentSessionId
            visualizerController.attachToAudioSession(currentSessionId)
            replayGainController.attachToAudioSession(currentSessionId, _isReplayGainEnabled.value)
            serviceScope.launch {
                val savedLevels = settingsDataStore.eqLevelsFlow.first()
                equalizerController.attachToAudioSession(currentSessionId, savedLevels, _isEqualizerEnabled.value)
                _equalizerBands.value = equalizerController.getBands()
                equalizerController.applyPreset(_equalizerPreset.value)
            }
        }

        // Restore pending playback if service was started on-demand or restored on launch
        val pending = pendingPlayTrack
        if (pending != null) {
            pendingPlayTrack = null
            executePlay(player1, pending.track, startPaused = pending.startPaused)
            serviceScope.launch {
                val lastPos = settingsDataStore.lastPositionFlow.first()
                if (lastPos > 0) {
                    player1.seekTo(lastPos)
                    _playbackPosition.value = lastPos
                }
            }
        } else if (player1.currentMediaItem == null && _currentTrack.value != null) {
            val track = _currentTrack.value!!
            executePlay(player1, track, startPaused = !_isPlaying.value)
            val pos = _playbackPosition.value
            if (pos > 0) {
                player1.seekTo(pos)
            }
        } else {
            // Restore playback position if returning
            serviceScope.launch {
                val lastPos = settingsDataStore.lastPositionFlow.first()
                if (lastPos > 0 && _playbackPosition.value == 0L) {
                    _playbackPosition.value = lastPos
                    player1.seekTo(lastPos)
                }
            }
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        attachPlayers(player, null)
    }

    fun detachPlayer() {
        stopPositionTracking()
        cancelSleepTimer()
        crossfadeController.release()
        playerA?.removeListener(playerAListener)
        playerB?.removeListener(playerBListener)
        playerA = null
        playerB = null
        visualizerController.release()
        equalizerController.release()
        replayGainController.release()
        _isPlaying.value = false
    }

    private fun createSecondaryPlayer(isPlayerB: Boolean): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioOffloadSupportProvider { _, _ -> AudioOffloadSupport.DEFAULT_UNSUPPORTED }
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(visualizerController.audioBufferSink)))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, false) // Note: false to not compete for audio focus
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        if (isPlayerB) {
            player.addListener(playerBListener)
        } else {
            player.addListener(playerAListener)
        }
        return player
    }

    private fun getOrCreateSecondaryPlayer(): ExoPlayer {
        val existing = secondaryPlayer
        if (existing != null) return existing
        val isForPlayerB = (activePlayerIndex == 0)
        val newPlayer = createSecondaryPlayer(isPlayerB = isForPlayerB)
        if (isForPlayerB) {
            playerB = newPlayer
        } else {
            playerA = newPlayer
        }
        return newPlayer
    }

    private fun handleTrackEnded() {
        if (_sleepTimerMode.value == -1) {
            // Stop on end of current track
            pause()
            cancelSleepTimer()
            return
        }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                activePlayer?.seekTo(0)
                activePlayer?.play()
            }
            RepeatMode.ALL -> {
                nextTrack(autoPlayIfPaused = true)
            }
            RepeatMode.OFF -> {
                val nextIdx = _queueIndex.value + 1
                if (nextIdx < _queue.value.size) {
                    playTrackAtIndex(nextIdx, autoPlay = true)
                } else {
                    // Reached end of queue
                    activePlayer?.pause()
                    _isPlaying.value = false
                    activePlayer?.seekTo(0)
                    _playbackPosition.value = 0
                }
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null, startIndex: Int = -1, startPaused: Boolean = false) {
        try {
            crossfadeController.cancelCrossfade()

            if (newQueue != null && newQueue.isNotEmpty()) {
                _queue.value = newQueue
                _queueIndex.value = if (startIndex >= 0) startIndex else newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            } else if (!_queue.value.any { it.id == track.id }) {
                _queue.value = listOf(track)
                _queueIndex.value = 0
            } else {
                _queueIndex.value = _queue.value.indexOfFirst { it.id == track.id }
            }

            _currentTrack.value = track
            _duration.value = track.duration
            if (!startPaused) {
                _playbackPosition.value = 0
            }
            visualizerController.resetBuffers()
            updateAudioSpecsForTrack(track, activePlayer)

            val player = activePlayer
            if (player == null) {
                pendingPlayTrack = PendingPlay(track, startPaused)
                saveCurrentState()
                startServiceIfNeeded()
                return
            }
            // Stop secondary player to prevent concurrent playback
            secondaryPlayer?.let { sec ->
                try {
                    sec.stop()
                    sec.clearMediaItems()
                    sec.volume = 1f
                } catch (_: Exception) {}
            }
            executePlay(player, track, startPaused)
            saveCurrentState()
        } catch (t: Throwable) {
            Log.e("PlaybackManager", "Error in playTrack: ${t.message}", t)
            _errorMessage.value = "Ошибка воспроизведения: ${t.localizedMessage ?: "неизвестная ошибка"}"
        }
    }

    private fun executePlay(player: ExoPlayer, track: Track, startPaused: Boolean) {
        try {
            player.volume = 1f
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.albumArtUri)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(mediaMetadata)
                .build()

            replayGainController.attachPlayer(player)
            replayGainController.onTrackChanged(track)
            player.setMediaItem(mediaItem)
            player.prepare()
            if (startPaused) {
                player.playWhenReady = false
                _isPlaying.value = false
                visualizerController.onPlaybackStateChanged(false)
            } else {
                onRequestAudioFocus?.invoke()
                player.playWhenReady = true
                player.play()
                _isPlaying.value = true
                visualizerController.onPlaybackStateChanged(true)
            }
            onActivePlayerChanged?.invoke(player)
        } catch (t: Throwable) {
            Log.e("PlaybackManager", "Error in executePlay: ${t.message}", t)
            _errorMessage.value = "Ошибка воспроизведения: ${t.localizedMessage ?: "неизвестная ошибка"}"
        }
    }

    fun playTrackAtIndex(index: Int, autoPlay: Boolean) {
        crossfadeController.cancelCrossfade()
        val q = _queue.value
        if (index in q.indices) {
            _queueIndex.value = index
            playTrack(q[index], startPaused = !autoPlay)
        }
    }

    fun togglePlayPause() {
        val player = activePlayer
        if (player != null && (player.isPlaying || player.playWhenReady)) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        Log.d("TempPlayer", "play")
        onRequestAudioFocus?.invoke()
        val player = activePlayer
        val track = _currentTrack.value
        if (player == null) {
            if (track != null) {
                pendingPlayTrack = PendingPlay(track, startPaused = false)
            }
            startServiceIfNeeded()
            return
        }
        if ((player.playbackState == Player.STATE_IDLE || player.playerError != null) && track != null) {
            val resumePos = _playbackPosition.value
            executePlay(player, track, startPaused = false)
            if (resumePos > 0) {
                player.seekTo(resumePos)
            }
        } else if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
            _isPlaying.value = true
            visualizerController.onPlaybackStateChanged(true)
        } else if (player.currentMediaItem == null && track != null) {
            val resumePos = _playbackPosition.value
            executePlay(player, track, startPaused = false)
            if (resumePos > 0) {
                player.seekTo(resumePos)
            }
        } else {
            player.play()
            _isPlaying.value = true
            visualizerController.onPlaybackStateChanged(true)
        }
    }

    fun pause() {
        Log.d("TempPlayer", "pause")
        crossfadeController.cancelCrossfade()
        activePlayer?.pause()
        secondaryPlayer?.pause()
        _isPlaying.value = false
        visualizerController.onPlaybackStateChanged(false)
        stopPositionTracking()
        saveCurrentState()
    }

    fun stop() {
        Log.d("TempPlayer", "stop")
        crossfadeController.cancelCrossfade()
        playerA?.stop()
        playerB?.stop()
        _isPlaying.value = false
        visualizerController.onPlaybackStateChanged(false)
        stopPositionTracking()
        saveCurrentState()
    }

    fun seekTo(positionMs: Long) {
        crossfadeController.cancelCrossfade()
        val clamped = positionMs.coerceIn(0, _duration.value.coerceAtLeast(0))
        _playbackPosition.value = clamped
        visualizerController.resetBuffers()
        activePlayer?.seekTo(clamped)
    }

    fun seekForward10s() {
        seekTo(_playbackPosition.value + 10_000L)
    }

    fun seekBackward10s() {
        seekTo(_playbackPosition.value - 10_000L)
    }

    fun nextTrack(autoPlayIfPaused: Boolean = false) {
        Log.d("TempPlayer", "next")
        crossfadeController.cancelCrossfade()
        val q = _queue.value
        if (q.isEmpty()) return
        val currentIdx = _queueIndex.value
        val shouldPlay = if (autoPlayIfPaused) true else _isPlaying.value

        val nextIdx = if (_isShuffle.value && q.size > 1) {
            var randomIdx = (q.indices).random()
            while (randomIdx == currentIdx) {
                randomIdx = (q.indices).random()
            }
            randomIdx
        } else {
            if (currentIdx + 1 < q.size) currentIdx + 1 else 0
        }

        playTrackAtIndex(nextIdx, autoPlay = shouldPlay)
    }

    fun previousTrack(autoPlayIfPaused: Boolean = false) {
        Log.d("TempPlayer", "previous")
        crossfadeController.cancelCrossfade()
        val q = _queue.value
        if (q.isEmpty()) return
        val currentIdx = _queueIndex.value
        val shouldPlay = if (autoPlayIfPaused) true else _isPlaying.value

        // If played more than 3 seconds, replay track from beginning
        if (_playbackPosition.value > 3000L) {
            seekTo(0)
            return
        }

        val prevIdx = if (currentIdx - 1 >= 0) currentIdx - 1 else q.size - 1
        playTrackAtIndex(prevIdx, autoPlay = shouldPlay)
    }

    fun updateTrackFavorite(trackId: Long, isFavorite: Boolean) {
        val curr = _currentTrack.value
        if (curr != null && curr.id == trackId) {
            _currentTrack.value = curr.copy(isFavorite = isFavorite)
        }
        val q = _queue.value
        if (q.isNotEmpty()) {
            val updatedQueue = q.map { track ->
                if (track.id == trackId) track.copy(isFavorite = isFavorite) else track
            }
            _queue.value = updatedQueue
        }
    }

    private fun getNextTrackIndex(): Int? {
        val q = _queue.value
        if (q.isEmpty()) return null
        if (_repeatMode.value == RepeatMode.ONE) return null
        val currentIdx = _queueIndex.value
        return if (_isShuffle.value && q.size > 1) {
            var randomIdx = (q.indices).random()
            while (randomIdx == currentIdx) {
                randomIdx = (q.indices).random()
            }
            randomIdx
        } else {
            if (currentIdx + 1 < q.size) {
                currentIdx + 1
            } else if (_repeatMode.value == RepeatMode.ALL) {
                0
            } else {
                null
            }
        }
    }

    private fun hasNextTrack(): Boolean {
        return getNextTrackIndex() != null
    }

    private fun triggerCrossfadeTransition() {
        val nextIdx = getNextTrackIndex() ?: return
        val nextTrack = _queue.value.getOrNull(nextIdx) ?: return
        val outgoingPlayer = activePlayer ?: return
        val incomingPlayer = getOrCreateSecondaryPlayer()

        if (outgoingPlayer === incomingPlayer) {
            Log.w("PlaybackManager", "Crossfade aborted: active and secondary players are the same instance.")
            return
        }

        Log.d("PlaybackManager", "Triggering crossfade from index $_queueIndex to $nextIdx: ${nextTrack.title}")

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(nextTrack.title)
            .setArtist(nextTrack.artist)
            .setAlbumTitle(nextTrack.album)
            .setArtworkUri(nextTrack.albumArtUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(nextTrack.id.toString())
            .setUri(nextTrack.uri)
            .setMediaMetadata(mediaMetadata)
            .build()

        // Prepare incoming player silently for equal-power fade-in
        incomingPlayer.volume = 0f
        incomingPlayer.setMediaItem(mediaItem)
        incomingPlayer.prepare()
        incomingPlayer.playWhenReady = true
        incomingPlayer.play()

        // Switch active index to incomingPlayer
        activePlayerIndex = if (activePlayerIndex == 0) 1 else 0
        _currentTrack.value = nextTrack
        _queueIndex.value = nextIdx
        _duration.value = nextTrack.duration
        _playbackPosition.value = 0L
        updateAudioSpecsForTrack(nextTrack, incomingPlayer)

        replayGainController.attachPlayer(incomingPlayer)
        replayGainController.onTrackChanged(nextTrack)

        onActivePlayerChanged?.invoke(incomingPlayer)

        crossfadeController.startCrossfade(
            outgoingPlayer = outgoingPlayer,
            incomingPlayer = incomingPlayer,
            onCrossfadeComplete = {
                replayGainController.applyCurrentGain()
            }
        )
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        serviceScope.launch {
            settingsDataStore.setRepeatMode(mode)
        }
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(next)
    }

    fun toggleShuffle() {
        val newValue = !_isShuffle.value
        _isShuffle.value = newValue
        serviceScope.launch {
            settingsDataStore.setShuffleEnabled(newValue)
        }
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerMode.value = minutes

        if (minutes > 0) {
            val millis = minutes * 60 * 1000L
            sleepCountDownTimer = object : CountDownTimer(millis, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    _sleepTimerRemainingMillis.value = millisUntilFinished
                }

                override fun onFinish() {
                    _sleepTimerRemainingMillis.value = null
                    _sleepTimerMode.value = 0
                    pause()
                }
            }.start()
        }
    }

    fun cancelSleepTimer() {
        sleepCountDownTimer?.cancel()
        sleepCountDownTimer = null
        _sleepTimerRemainingMillis.value = null
        _sleepTimerMode.value = 0
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        equalizerController.setEnabled(enabled)
        serviceScope.launch {
            settingsDataStore.setEqualizerEnabled(enabled)
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerPreset.value = preset
        equalizerController.applyPreset(preset)
        _equalizerBands.value = equalizerController.getBands()
        serviceScope.launch {
            settingsDataStore.setEqualizerPreset(preset)
            val levels = _equalizerBands.value.map { it.levelMilliBels.toInt() }
            settingsDataStore.setEqualizerLevels(levels)
        }
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMilliBels: Short) {
        equalizerController.setBandLevel(bandIndex, levelMilliBels)
        _equalizerPreset.value = EqualizerPreset.CUSTOM
        _equalizerBands.value = equalizerController.getBands()
        serviceScope.launch {
            settingsDataStore.setEqualizerPreset(EqualizerPreset.CUSTOM)
            val levels = _equalizerBands.value.map { it.levelMilliBels.toInt() }
            settingsDataStore.setEqualizerLevels(levels)
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        _isReplayGainEnabled.value = enabled
        replayGainController.setEnabled(enabled)
        serviceScope.launch {
            settingsDataStore.setReplayGainEnabled(enabled)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _isCrossfadeEnabled.value = enabled
        crossfadeController.isEnabled = enabled
        serviceScope.launch {
            settingsDataStore.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        _crossfadeDurationSeconds.value = clamped
        crossfadeController.durationSeconds = clamped
        serviceScope.launch {
            settingsDataStore.setCrossfadeDurationSeconds(clamped)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startPositionTracking() {
        positionProgressJob?.cancel()
        positionProgressJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                activePlayer?.let { player ->
                    _playbackPosition.value = player.currentPosition
                    if (player.duration > 0 && _duration.value != player.duration) {
                        _duration.value = player.duration
                    }

                    if (crossfadeController.shouldTriggerCrossfade(
                            currentPositionMs = player.currentPosition,
                            totalDurationMs = _duration.value,
                            hasNextTrack = hasNextTrack()
                        )
                    ) {
                        triggerCrossfadeTransition()
                    }
                }
                tickCount++
                if (tickCount % 20 == 0) {
                    saveCurrentState()
                }
                delay(250)
            }
        }
    }

    private fun stopPositionTracking() {
        positionProgressJob?.cancel()
        positionProgressJob = null
        activePlayer?.let { player ->
            _playbackPosition.value = player.currentPosition
        }
    }

    suspend fun restoreSavedQueueAndTrack() {
        try {
            if (_queue.value.isNotEmpty() || _currentTrack.value != null) return

            val savedQueueIds = settingsDataStore.queueTrackIdsFlow.first()
            val savedQueueIndex = settingsDataStore.lastQueueIndexFlow.first()
            val lastTrackId = settingsDataStore.lastTrackIdFlow.first()
            val lastPos = settingsDataStore.lastPositionFlow.first()

            val restoredQueue = if (savedQueueIds.isNotEmpty()) {
                repository.getTracksByIds(savedQueueIds)
            } else {
                emptyList()
            }

            if (restoredQueue.isNotEmpty()) {
                _queue.value = restoredQueue
                val targetIndex = if (savedQueueIndex in restoredQueue.indices) {
                    savedQueueIndex
                } else if (lastTrackId > 0) {
                    restoredQueue.indexOfFirst { it.id == lastTrackId }.coerceAtLeast(0)
                } else {
                    0
                }
                _queueIndex.value = targetIndex

                val trackToRestore = if (lastTrackId > 0) {
                    restoredQueue.find { it.id == lastTrackId } ?: restoredQueue.getOrNull(targetIndex)
                } else {
                    restoredQueue.getOrNull(targetIndex)
                }

                if (trackToRestore != null && _currentTrack.value == null) {
                    _currentTrack.value = trackToRestore
                    _duration.value = trackToRestore.duration
                    _playbackPosition.value = lastPos
                    updateAudioSpecsForTrack(trackToRestore, activePlayer)
                    val player = activePlayer
                    if (player != null) {
                        executePlay(player, trackToRestore, startPaused = true)
                        if (lastPos > 0) {
                            player.seekTo(lastPos)
                        }
                    } else {
                        pendingPlayTrack = PendingPlay(trackToRestore, startPaused = true)
                    }
                }
            } else if (lastTrackId > 0) {
                // If no saved queue IDs, fallback to finding last track in allTracks
                val allTracks = repository.allTracks.first()
                val track = allTracks.find { it.id == lastTrackId }
                if (track != null && _currentTrack.value == null) {
                    _queue.value = allTracks
                    val idx = allTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    _queueIndex.value = idx
                    _currentTrack.value = track
                    _duration.value = track.duration
                    _playbackPosition.value = lastPos
                    updateAudioSpecsForTrack(track, activePlayer)
                    val player = activePlayer
                    if (player != null) {
                        executePlay(player, track, startPaused = true)
                        if (lastPos > 0) {
                            player.seekTo(lastPos)
                        }
                    } else {
                        pendingPlayTrack = PendingPlay(track, startPaused = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error restoring saved queue: ${e.message}", e)
        }
    }

    private fun saveCurrentState() {
        val track = _currentTrack.value
        val pos = _playbackPosition.value
        val q = _queue.value
        val idx = _queueIndex.value
        if (track != null || q.isNotEmpty()) {
            serviceScope.launch {
                settingsDataStore.savePlaybackState(
                    trackId = track?.id ?: -1L,
                    position = pos,
                    queueIds = if (q.isNotEmpty()) q.map { it.id } else null,
                    queueIndex = if (idx >= 0) idx else null
                )
            }
        }
    }

    private fun updateAudioSpecsFromPlayer(player: ExoPlayer?) {
        val track = _currentTrack.value ?: return
        val audioFormat = player?.audioFormat
        val currentSpecs = _trackAudioSpecs.value ?: buildInitialSpecs(track)

        var sampleRate = audioFormat?.sampleRate ?: 0
        if (sampleRate <= 0) sampleRate = visualizerController.getCurrentSampleRate()
        if (sampleRate <= 0) sampleRate = currentSpecs.sampleRateHz

        var bitrateKbps = if ((audioFormat?.bitrate ?: 0) > 0) audioFormat!!.bitrate / 1000 else currentSpecs.bitrateKbps
        var channels = if ((audioFormat?.channelCount ?: 0) > 0) audioFormat!!.channelCount else visualizerController.getCurrentChannelCount()
        if (channels <= 0) channels = currentSpecs.channelCount

        val encoding = visualizerController.getCurrentEncoding()
        val bitDepth = when (encoding) {
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            else -> currentSpecs.bitDepth
        }

        val format = detectFormat(track, audioFormat?.sampleMimeType)
        val isLossless = format in listOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")

        _trackAudioSpecs.value = AudioTrackSpecs(
            format = format,
            sampleRateHz = sampleRate,
            bitrateKbps = bitrateKbps,
            bitDepth = bitDepth,
            channelCount = channels,
            isLossless = isLossless
        )
    }

    private fun detectFormat(track: Track, sampleMimeType: String? = null): String {
        sampleMimeType?.let { mime ->
            when {
                mime.contains("flac", ignoreCase = true) -> return "FLAC"
                mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> return "MP3"
                mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> return "AAC"
                mime.contains("wav", ignoreCase = true) || mime.contains("raw", ignoreCase = true) -> return "WAV"
                mime.contains("ogg", ignoreCase = true) || mime.contains("vorbis", ignoreCase = true) -> return "OGG"
                mime.contains("opus", ignoreCase = true) -> return "OPUS"
                mime.contains("alac", ignoreCase = true) -> return "ALAC"
                mime.contains("wma", ignoreCase = true) -> return "WMA"
            }
        }

        val raw = if (track.path.isNotBlank()) track.path else track.uriString
        val ext = raw.substringBefore('?').substringAfterLast('.', "").uppercase()
        if (ext.isNotBlank() && ext.length in 2..5) {
            return ext
        }

        return "MP3"
    }

    private fun buildInitialSpecs(track: Track): AudioTrackSpecs {
        val format = detectFormat(track)
        val isLossless = format in listOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")

        var calcBitrate = 0
        if (track.size > 0 && track.duration > 0) {
            calcBitrate = ((track.size * 8L) / track.duration).toInt()
        }
        if (calcBitrate !in 32..9216) {
            calcBitrate = if (isLossless) (if (format == "WAV") 1411 else 850) else 320
        }

        val sampleRate = if (isLossless && (format == "FLAC" || format == "WAV")) 48000 else 44100
        val bitDepth = if (isLossless && calcBitrate > 2000) 24 else 16

        return AudioTrackSpecs(
            format = format,
            sampleRateHz = sampleRate,
            bitrateKbps = calcBitrate,
            bitDepth = bitDepth,
            channelCount = 2,
            isLossless = isLossless
        )
    }

    private fun updateAudioSpecsForTrack(track: Track, player: ExoPlayer?) {
        val initial = buildInitialSpecs(track)
        _trackAudioSpecs.value = initial

        serviceScope.launch(Dispatchers.IO) {
            var sampleRate = initial.sampleRateHz
            var bitrate = initial.bitrateKbps
            var detectedFormat = initial.format
            var isLossless = initial.isLossless

            try {
                val mmr = MediaMetadataRetriever()
                if (track.path.isNotBlank() && File(track.path).exists()) {
                    mmr.setDataSource(track.path)
                } else {
                    mmr.setDataSource(context, track.uri)
                }

                val mmrBitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                if (mmrBitrate != null && mmrBitrate > 0) {
                    bitrate = mmrBitrate / 1000
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val mmrSampleRate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                    if (mmrSampleRate != null && mmrSampleRate > 0) {
                        sampleRate = mmrSampleRate
                    }
                }

                val mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                if (!mime.isNullOrBlank()) {
                    detectedFormat = detectFormat(track, mime)
                    isLossless = detectedFormat in listOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")
                }

                mmr.release()
            } catch (e: Exception) {
                // Fallback to initial
            }

            val playerFormat = withContext(Dispatchers.Main) { player?.audioFormat }
            if (playerFormat != null) {
                if (playerFormat.sampleRate > 0) sampleRate = playerFormat.sampleRate
                if (playerFormat.bitrate > 0) bitrate = playerFormat.bitrate / 1000
                if (!playerFormat.sampleMimeType.isNullOrBlank()) {
                    detectedFormat = detectFormat(track, playerFormat.sampleMimeType)
                    isLossless = detectedFormat in listOf("FLAC", "WAV", "ALAC", "AIFF", "DSD")
                }
            }

            _trackAudioSpecs.value = AudioTrackSpecs(
                format = detectedFormat,
                sampleRateHz = sampleRate,
                bitrateKbps = bitrate,
                bitDepth = if (isLossless && (sampleRate > 48000 || bitrate > 2000)) 24 else 16,
                channelCount = playerFormat?.channelCount ?: 2,
                isLossless = isLossless
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
