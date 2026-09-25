package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioTrackSpecs
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.MiniPlayerBgMode
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.SortOrder
import com.example.data.model.ThemeMode
import com.example.data.model.Track
import com.example.data.model.VisualizerMode
import com.example.service.PlaybackManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val playbackManager = PlaybackManager.getInstance(application)
    private val repository = playbackManager.repository
    private val settingsDataStore = playbackManager.settingsDataStore

    // Search and Filter State
    private val _navigateToPlayerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToPlayerEvent: SharedFlow<Unit> = _navigateToPlayerEvent.asSharedFlow()

    fun requestNavigateToPlayer() {
        _navigateToPlayerEvent.tryEmit(Unit)
    }

    fun handleExternalAudioUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val track = repository.getOrCreateTrackFromUri(uri)
                playTrack(track = track, queue = listOf(track), index = 0)
                _navigateToPlayerEvent.tryEmit(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.BY_TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Tracks, 1: Folders, 2: Artists, 3: Albums, 4: Playlists
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    // Library Data
    val rawTracks: StateFlow<List<Track>> = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered and Sorted Tracks
    val displayedTracks: StateFlow<List<Track>> = combine(
        rawTracks,
        searchQuery,
        sortOrder
    ) { tracks, query, sort ->
        val filtered = if (query.isBlank()) {
            tracks
        } else {
            val q = query.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOrder.BY_TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.BY_ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOrder.BY_ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOrder.BY_DURATION -> filtered.sortedByDescending { it.duration }
            SortOrder.BY_DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            rawTracks.collect { tracks ->
                if (tracks.isNotEmpty() && currentQueue.value.isEmpty()) {
                    playbackManager.restoreSavedQueueAndTrack()
                }
            }
        }
    }

    // Groupings
    val folderGroups: StateFlow<Map<String, List<Track>>> = displayedTracks.combine(searchQuery) { tracks, _ ->
        tracks.groupBy { it.folderName }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val artistGroups: StateFlow<Map<String, List<Track>>> = displayedTracks.combine(searchQuery) { tracks, _ ->
        tracks.groupBy { it.artist }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val albumGroups: StateFlow<Map<String, List<Track>>> = displayedTracks.combine(searchQuery) { tracks, _ ->
        tracks.groupBy { it.album }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Playback States from Manager
    val currentTrack: StateFlow<Track?> = playbackManager.currentTrack
    val trackAudioSpecs: StateFlow<AudioTrackSpecs?> = playbackManager.trackAudioSpecs
    val currentQueue: StateFlow<List<Track>> = playbackManager.queue
    val currentQueueIndex: StateFlow<Int> = playbackManager.queueIndex
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val playbackPosition: StateFlow<Long> = playbackManager.playbackPosition
    val duration: StateFlow<Long> = playbackManager.duration
    val repeatMode: StateFlow<RepeatMode> = playbackManager.repeatMode
    val isShuffle: StateFlow<Boolean> = playbackManager.isShuffle
    val sleepTimerRemainingMillis: StateFlow<Long?> = playbackManager.sleepTimerRemainingMillis
    val sleepTimerMode: StateFlow<Int> = playbackManager.sleepTimerMode
    val equalizerBands: StateFlow<List<EqualizerBand>> = playbackManager.equalizerBands
    val equalizerPreset: StateFlow<EqualizerPreset> = playbackManager.equalizerPreset
    val isEqualizerEnabled: StateFlow<Boolean> = playbackManager.isEqualizerEnabled
    val visualizerData: StateFlow<FloatArray> = playbackManager.visualizerData
    val visualizerWaveform: StateFlow<FloatArray> = playbackManager.visualizerWaveform
    val audioAmplitude: StateFlow<Float> = playbackManager.audioAmplitude
    val errorMessage: StateFlow<String?> = playbackManager.errorMessage

    // Settings Flows
    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.LIGHT
    )

    val visualizerEnabled: StateFlow<Boolean> = settingsDataStore.visualizerEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val visualizerMode: StateFlow<VisualizerMode> = settingsDataStore.visualizerModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VisualizerMode.SPECTRUM
    )

    val visualizerBands: StateFlow<Int> = settingsDataStore.visualizerBandsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 32
    )

    val visualizerSensitivity: StateFlow<Float> = settingsDataStore.visualizerSensitivityFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val miniPlayerBgMode: StateFlow<MiniPlayerBgMode> = settingsDataStore.miniPlayerBgModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MiniPlayerBgMode.ALBUM_ART
    )

    val miniPlayerCustomColor: StateFlow<Long> = settingsDataStore.miniPlayerCustomColorFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0xFF1E1F24L
    )

    val autoRotate: StateFlow<Boolean> = settingsDataStore.autoRotateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val replayGainEnabled: StateFlow<Boolean> = playbackManager.isReplayGainEnabled
    val crossfadeEnabled: StateFlow<Boolean> = playbackManager.isCrossfadeEnabled
    val crossfadeDurationSeconds: StateFlow<Int> = playbackManager.crossfadeDurationSeconds

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChanged(order: SortOrder) {
        _sortOrder.value = order
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun scanMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.scanLocalMusic()
                _scanMessage.value = if (count > 0) "Найдено треков: $count" else "Аудиофайлы не найдены"
                if (currentQueue.value.isEmpty()) {
                    playbackManager.restoreSavedQueueAndTrack()
                }
            } catch (e: Exception) {
                _scanMessage.value = "Ошибка сканирования: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    fun playTrack(track: Track, queue: List<Track>? = null, index: Int = -1) {
        playbackManager.playTrack(
            track = track,
            newQueue = queue ?: displayedTracks.value,
            startIndex = index,
            startPaused = false
        )
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun play() {
        playbackManager.play()
    }

    fun pause() {
        playbackManager.pause()
    }

    fun stop() {
        playbackManager.stop()
    }

    fun nextTrack() {
        playbackManager.nextTrack()
    }

    fun previousTrack() {
        playbackManager.previousTrack()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun seekForward10s() {
        playbackManager.seekForward10s()
    }

    fun seekBackward10s() {
        playbackManager.seekBackward10s()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name)
            }
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.renamePlaylist(id, newName)
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun getPlaylistTracks(playlistId: Long): StateFlow<List<Track>> {
        val flow = MutableStateFlow<List<Track>>(emptyList())
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlistId).collect {
                flow.value = it
            }
        }
        return flow
    }

    fun setSleepTimer(minutes: Int) {
        playbackManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackManager.cancelSleepTimer()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        playbackManager.setEqualizerEnabled(enabled)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        playbackManager.setEqualizerPreset(preset)
    }

    fun setEqualizerBandLevel(band: Short, level: Short) {
        playbackManager.setEqualizerBandLevel(band, level)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setVisualizerEnabled(enabled)
        }
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        viewModelScope.launch {
            settingsDataStore.setVisualizerMode(mode)
        }
    }

    fun cycleVisualizerMode() {
        val modes = listOf(VisualizerMode.AMPLITUDE, VisualizerMode.SPECTRUM, VisualizerMode.WAVE)
        val currentIndex = modes.indexOf(visualizerMode.value)
        val nextMode = if (currentIndex >= 0) {
            modes[(currentIndex + 1) % modes.size]
        } else {
            VisualizerMode.AMPLITUDE
        }
        setVisualizerMode(nextMode)
    }

    fun onAudioPermissionGranted() {
        playbackManager.onAudioPermissionGranted()
    }

    fun setVisualizerBands(bands: Int) {
        viewModelScope.launch {
            settingsDataStore.setVisualizerBands(bands)
            playbackManager.visualizerController.updateConfig(bands, visualizerSensitivity.value)
        }
    }

    fun setVisualizerSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsDataStore.setVisualizerSensitivity(sensitivity)
            playbackManager.visualizerController.updateConfig(visualizerBands.value, sensitivity)
        }
    }

    fun setMiniPlayerBgMode(mode: MiniPlayerBgMode) {
        viewModelScope.launch {
            settingsDataStore.setMiniPlayerBgMode(mode)
        }
    }

    fun setMiniPlayerCustomColor(color: Long) {
        viewModelScope.launch {
            settingsDataStore.setMiniPlayerCustomColor(color)
        }
    }

    fun setAutoRotate(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoRotate(enabled)
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playbackManager.setReplayGainEnabled(enabled)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            playbackManager.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            playbackManager.setCrossfadeDurationSeconds(seconds)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsDataStore.resetSettings()
            playbackManager.cancelSleepTimer()
            playbackManager.setRepeatMode(RepeatMode.OFF)
            playbackManager.setEqualizerPreset(EqualizerPreset.FLAT)
            playbackManager.setEqualizerEnabled(true)
            playbackManager.setReplayGainEnabled(true)
            playbackManager.setCrossfadeEnabled(true)
            playbackManager.setCrossfadeDurationSeconds(4)
            playbackManager.visualizerController.updateConfig(32, 1.0f)
        }
    }

    fun clearError() {
        playbackManager.clearError()
    }
}
