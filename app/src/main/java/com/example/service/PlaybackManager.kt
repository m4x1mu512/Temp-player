package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager private constructor(private val context: Context) {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val database = AppDatabase.getInstance(context)
    val repository = MusicRepository(context, database)
    val settingsDataStore = SettingsDataStore(context)

    var exoPlayer: ExoPlayer? = null
        private set

    val visualizerController = AudioVisualizerController(context, serviceScope)
    val equalizerController = EqualizerController()

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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
        serviceScope.launch {
            // Restore settings
            _repeatMode.value = settingsDataStore.repeatModeFlow.first()
            _isShuffle.value = settingsDataStore.shuffleEnabledFlow.first()
            _isEqualizerEnabled.value = settingsDataStore.eqEnabledFlow.first()
            _equalizerPreset.value = settingsDataStore.eqPresetFlow.first()

            val bands = settingsDataStore.visualizerBandsFlow.first()
            val sens = settingsDataStore.visualizerSensitivityFlow.first()
            visualizerController.updateConfig(bands, sens)
        }
    }

    private data class PendingPlay(val track: Track, val startPaused: Boolean)
    private var pendingPlayTrack: PendingPlay? = null

    @OptIn(UnstableApi::class)
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            visualizerController.onPlaybackStateChanged(isPlaying)
            if (isPlaying) {
                startPositionTracking()
            } else {
                stopPositionTracking()
                saveCurrentState()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
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

                    val currentSessionId = player.audioSessionId
                    if (currentSessionId > 0 && currentSessionId != currentAudioSessionId) {
                        currentAudioSessionId = currentSessionId
                        visualizerController.attachToAudioSession(currentSessionId)
                        serviceScope.launch {
                            val savedLevels = settingsDataStore.eqLevelsFlow.first()
                            equalizerController.attachToAudioSession(currentSessionId, savedLevels, _isEqualizerEnabled.value)
                            _equalizerBands.value = equalizerController.getBands()
                            equalizerController.applyPreset(_equalizerPreset.value)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "Не удалось воспроизвести файл: ${error.localizedMessage ?: "ошибка декодирования"}"
            _isPlaying.value = false
            visualizerController.onPlaybackStateChanged(false)
        }
    }

    fun startServiceIfNeeded() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start PlaybackService", e)
        }
    }

    @OptIn(UnstableApi::class)
    fun attachPlayer(player: ExoPlayer) {
        if (exoPlayer === player) return
        exoPlayer = player
        player.addListener(playerListener)

        val currentSessionId = player.audioSessionId
        if (currentSessionId > 0 && currentSessionId != currentAudioSessionId) {
            currentAudioSessionId = currentSessionId
            visualizerController.attachToAudioSession(currentSessionId)
            serviceScope.launch {
                val savedLevels = settingsDataStore.eqLevelsFlow.first()
                equalizerController.attachToAudioSession(currentSessionId, savedLevels, _isEqualizerEnabled.value)
                _equalizerBands.value = equalizerController.getBands()
                equalizerController.applyPreset(_equalizerPreset.value)
            }
        }

        val pending = pendingPlayTrack
        if (pending != null) {
            pendingPlayTrack = null
            val resumePos = _playbackPosition.value
            executePlay(player, pending.track, pending.startPaused)
            if (resumePos > 0) {
                player.seekTo(resumePos)
            }
        } else {
            val current = _currentTrack.value
            if (current != null) {
                // Restore current track into the newly attached player instance
                executePlay(player, current, startPaused = true)
                if (_playbackPosition.value > 0) {
                    player.seekTo(_playbackPosition.value)
                }
            }
        }
    }

    fun detachPlayer() {
        stopPositionTracking()
        cancelSleepTimer()
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
        visualizerController.release()
        equalizerController.release()
        _isPlaying.value = false
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
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
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
                    exoPlayer?.pause()
                    _isPlaying.value = false
                    exoPlayer?.seekTo(0)
                    _playbackPosition.value = 0
                }
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null, startIndex: Int = -1, startPaused: Boolean = false) {
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
        _playbackPosition.value = 0

        val player = exoPlayer
        if (player == null) {
            pendingPlayTrack = PendingPlay(track, startPaused)
            startServiceIfNeeded()
            return
        }
        executePlay(player, track, startPaused)
    }

    private fun executePlay(player: ExoPlayer, track: Track, startPaused: Boolean) {
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

        player.setMediaItem(mediaItem)
        player.prepare()
        if (startPaused) {
            player.playWhenReady = false
            _isPlaying.value = false
        } else {
            player.playWhenReady = true
            player.play()
            _isPlaying.value = true
        }
    }

    fun playTrackAtIndex(index: Int, autoPlay: Boolean) {
        val q = _queue.value
        if (index in q.indices) {
            _queueIndex.value = index
            playTrack(q[index], startPaused = !autoPlay)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player != null && (player.isPlaying || player.playWhenReady)) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        Log.d("TempPlayer", "play")
        val player = exoPlayer
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
        } else if (player.currentMediaItem == null && track != null) {
            val resumePos = _playbackPosition.value
            executePlay(player, track, startPaused = false)
            if (resumePos > 0) {
                player.seekTo(resumePos)
            }
        } else {
            player.play()
            _isPlaying.value = true
        }
    }

    fun pause() {
        Log.d("TempPlayer", "pause")
        exoPlayer?.pause()
        _isPlaying.value = false
        stopPositionTracking()
        saveCurrentState()
    }

    fun stop() {
        Log.d("TempPlayer", "stop")
        exoPlayer?.stop()
        _isPlaying.value = false
        stopPositionTracking()
        saveCurrentState()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0, _duration.value.coerceAtLeast(0))
        _playbackPosition.value = clamped
        exoPlayer?.seekTo(clamped)
    }

    fun seekForward10s() {
        seekTo(_playbackPosition.value + 10_000L)
    }

    fun seekBackward10s() {
        seekTo(_playbackPosition.value - 10_000L)
    }

    /**
     * Requirement:
     * "Если трек не воспроизводится, при переключении на другой трек, этот другой трек не должен включаться сам."
     */
    fun nextTrack(autoPlayIfPaused: Boolean = false) {
        Log.d("TempPlayer", "next")
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
            _sleepTimerRemainingMillis.value = millis
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

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startPositionTracking() {
        positionProgressJob?.cancel()
        positionProgressJob = serviceScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _playbackPosition.value = player.currentPosition
                    if (player.duration > 0 && _duration.value != player.duration) {
                        _duration.value = player.duration
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopPositionTracking() {
        positionProgressJob?.cancel()
        positionProgressJob = null
        exoPlayer?.let { player ->
            _playbackPosition.value = player.currentPosition
        }
    }

    private fun saveCurrentState() {
        val track = _currentTrack.value ?: return
        val pos = _playbackPosition.value
        serviceScope.launch {
            settingsDataStore.savePlaybackState(track.id, pos)
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
