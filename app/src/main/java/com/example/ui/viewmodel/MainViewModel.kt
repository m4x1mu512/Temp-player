package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.model.AudioTrackSpecs
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.MiniPlayerBgMode
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.ReverbPreset
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val playbackManager = PlaybackManager.getInstance(application)
    private val repository = playbackManager.repository
    private val settingsDataStore = playbackManager.settingsDataStore

    // Search and Filter State
    private val _navigateToPlayerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToPlayerEvent: SharedFlow<Unit> = _navigateToPlayerEvent.asSharedFlow()

    private val _navigateToQueueEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToQueueEvent: SharedFlow<Unit> = _navigateToQueueEvent.asSharedFlow()

    private val _openQueueEvent = MutableStateFlow(0)
    val openQueueEvent: StateFlow<Int> = _openQueueEvent.asStateFlow()

    fun requestNavigateToPlayer() {
        _navigateToPlayerEvent.tryEmit(Unit)
    }

    fun openPlaybackQueue() {
        _selectedTab.value = 0
        _openQueueEvent.value++
        _navigateToQueueEvent.tryEmit(Unit)
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

    private val _sortOrder = MutableStateFlow(settingsDataStore.getInitialSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private fun sortTrackList(list: List<Track>, sort: SortOrder): List<Track> {
        return when (sort) {
            SortOrder.BY_TITLE -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SortOrder.BY_ARTIST -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
            SortOrder.BY_ALBUM -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.album })
            SortOrder.BY_DURATION -> list.sortedByDescending { it.duration }
            SortOrder.BY_DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        }
    }

    private val _selectedTab = MutableStateFlow(0) // 0: Tracks, 1: Folders, 2: Artists, 3: Albums, 4: Playlists
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    // Library Data
    val rawTracks: StateFlow<List<Track>> = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _inMemoryFavoriteOrder = MutableStateFlow<List<Long>?>(
        settingsDataStore.getInitialFavoritesOrder().ifEmpty { null }
    )

    val favoriteTracks: StateFlow<List<Track>> = combine(
        repository.favoriteTracks,
        _inMemoryFavoriteOrder,
        sortOrder
    ) { tracks, memOrder, sort ->
        if (tracks.isEmpty()) {
            emptyList()
        } else if (memOrder != null) {
            val map = tracks.associateBy { it.id }
            val ordered = memOrder.mapNotNull { map[it] }.toMutableList()
            val existingIds = ordered.map { it.id }.toSet()
            tracks.forEach { t ->
                if (!existingIds.contains(t.id)) {
                    ordered.add(t)
                }
            }
            ordered
        } else {
            sortTrackList(tracks, sort)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _inMemoryAllTracksOrder = MutableStateFlow<List<Long>?>(
        settingsDataStore.getInitialCustomAllTracksOrder().ifEmpty { null }
    )

    val allTracksOrdered: StateFlow<List<Track>> = combine(
        rawTracks,
        _inMemoryAllTracksOrder,
        sortOrder
    ) { tracks, memOrder, sort ->
        if (tracks.isEmpty()) {
            emptyList()
        } else if (memOrder != null) {
            val map = tracks.associateBy { it.id }
            val ordered = memOrder.mapNotNull { map[it] }.toMutableList()
            val existingIds = ordered.map { it.id }.toSet()
            tracks.forEach { t ->
                if (!existingIds.contains(t.id)) {
                    ordered.add(t)
                }
            }
            ordered
        } else {
            sortTrackList(tracks, sort)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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
        sortTrackList(filtered, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            try {
                val directIds = repository.getAllFavoriteIdsDirect().toSet()
                if (directIds.isNotEmpty()) {
                    _favoriteIds.value = directIds
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.favoriteIds.collect { dbIds ->
                _favoriteIds.value = dbIds
            }
        }

        viewModelScope.launch {
            playbackManager.restoreSavedQueueAndTrack()
            rawTracks.collect { tracks ->
                if (tracks.isNotEmpty() && currentQueue.value.isEmpty()) {
                    playbackManager.restoreSavedQueueAndTrack(tracks)
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
    val eqPreamp: StateFlow<Int> = playbackManager.eqPreamp
    val isBassBoostEnabled: StateFlow<Boolean> = playbackManager.isBassBoostEnabled
    val bassBoostStrength: StateFlow<Int> = playbackManager.bassBoostStrength
    val isVirtualizerEnabled: StateFlow<Boolean> = playbackManager.isVirtualizerEnabled
    val virtualizerStrength: StateFlow<Int> = playbackManager.virtualizerStrength
    val isReverbEnabled: StateFlow<Boolean> = playbackManager.isReverbEnabled
    val reverbPreset: StateFlow<ReverbPreset> = playbackManager.reverbPreset
    val isLoudnessEnabled: StateFlow<Boolean> = playbackManager.isLoudnessEnabled
    val loudnessGain: StateFlow<Int> = playbackManager.loudnessGain
    val playbackSpeed: StateFlow<Float> = playbackManager.playbackSpeed
    val playbackPitch: StateFlow<Float> = playbackManager.playbackPitch
    val visualizerData: StateFlow<FloatArray> = playbackManager.visualizerData
    val visualizerWaveform: StateFlow<FloatArray> = playbackManager.visualizerWaveform
    val audioAmplitude: StateFlow<Float> = playbackManager.audioAmplitude
    val errorMessage: StateFlow<String?> = playbackManager.errorMessage

    // Settings Flows
    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsDataStore.getInitialThemeMode(application)
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
        settingsDataStore.saveCachedSortOrder(order)

        viewModelScope.launch {
            try {
                val currentRaw = if (rawTracks.value.isNotEmpty()) rawTracks.value else repository.allTracks.first()
                if (currentRaw.isNotEmpty()) {
                    val sorted = sortTrackList(currentRaw, order)
                    val sortedIds = sorted.map { it.id }
                    _inMemoryAllTracksOrder.value = sortedIds
                    settingsDataStore.saveCustomAllTracksOrder(sortedIds)
                }

                val queue = playbackManager.queue.value
                if (queue.isNotEmpty()) {
                    val sortedQueue = sortTrackList(queue, order)
                    playbackManager.setQueue(sortedQueue)
                }

                val favs = if (favoriteTracks.value.isNotEmpty()) favoriteTracks.value else repository.favoriteTracks.first()
                if (favs.isNotEmpty()) {
                    val sortedFavs = sortTrackList(favs, order)
                    val sortedFavIds = sortedFavs.map { it.id }
                    _inMemoryFavoriteOrder.value = sortedFavIds
                    settingsDataStore.saveCachedFavoritesOrder(sortedFavIds)
                    repository.reorderFavorites(sortedFavIds)
                }

                val currentPlaylists = repository.playlists.first()
                for (pl in currentPlaylists) {
                    val plTracks = repository.getTracksForPlaylist(pl.id).first()
                    if (plTracks.isNotEmpty()) {
                        val sortedPlTracks = sortTrackList(plTracks, order)
                        val sortedPlIds = sortedPlTracks.map { it.id }
                        _inMemoryPlaylistOrder.getOrPut(pl.id) { MutableStateFlow(null) }.value = sortedPlIds
                        settingsDataStore.saveCachedPlaylistOrder(pl.id, sortedPlIds)
                        repository.reorderPlaylistTracks(pl.id, sortedPlIds)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun playTrackAtIndex(index: Int) {
        playbackManager.playTrackAtIndex(index, autoPlay = isPlaying.value)
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
        val currentSet = _favoriteIds.value
        val isCurrentlyFav = if (currentSet.isNotEmpty() || rawTracks.value.isEmpty()) {
            currentSet.contains(trackId)
        } else {
            rawTracks.value.find { it.id == trackId }?.isFavorite == true
        }
        val newSet = if (isCurrentlyFav) currentSet - trackId else currentSet + trackId
        _favoriteIds.value = newSet

        val currentOrder = _inMemoryFavoriteOrder.value ?: emptyList()
        val updatedOrder = if (isCurrentlyFav) {
            currentOrder.filter { it != trackId }
        } else {
            if (!currentOrder.contains(trackId)) currentOrder + trackId else currentOrder
        }
        _inMemoryFavoriteOrder.value = updatedOrder
        settingsDataStore.saveCachedFavoritesOrder(updatedOrder)

        playbackManager.updateTrackFavorite(trackId, !isCurrentlyFav)

        viewModelScope.launch {
            try {
                repository.toggleFavorite(trackId)
            } catch (e: Exception) {
                _favoriteIds.value = currentSet
                playbackManager.updateTrackFavorite(trackId, isCurrentlyFav)
            }
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
        playlistTracksCache.remove(id)
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            val memFlow = _inMemoryPlaylistOrder.getOrPut(playlistId) {
                MutableStateFlow(settingsDataStore.getInitialPlaylistOrder(playlistId).ifEmpty { null })
            }
            val currentOrder = memFlow.value ?: emptyList()
            if (!currentOrder.contains(trackId)) {
                val newOrder = currentOrder + trackId
                memFlow.value = newOrder
                settingsDataStore.saveCachedPlaylistOrder(playlistId, newOrder)
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            val memFlow = _inMemoryPlaylistOrder.getOrPut(playlistId) {
                MutableStateFlow(settingsDataStore.getInitialPlaylistOrder(playlistId).ifEmpty { null })
            }
            val currentOrder = memFlow.value
            if (currentOrder != null && currentOrder.contains(trackId)) {
                val newOrder = currentOrder.filter { it != trackId }
                memFlow.value = newOrder
                settingsDataStore.saveCachedPlaylistOrder(playlistId, newOrder)
            }
        }
    }

    private val playlistTracksCache = mutableMapOf<Long, StateFlow<List<Track>>>()
    private val _inMemoryPlaylistOrder = mutableMapOf<Long, MutableStateFlow<List<Long>?>>()

    fun getPlaylistTracks(playlistId: Long): StateFlow<List<Track>> {
        return playlistTracksCache.getOrPut(playlistId) {
            val memFlow = _inMemoryPlaylistOrder.getOrPut(playlistId) {
                MutableStateFlow(settingsDataStore.getInitialPlaylistOrder(playlistId).ifEmpty { null })
            }
            combine(
                repository.getTracksForPlaylist(playlistId),
                memFlow,
                sortOrder
            ) { tracks, memOrder, sort ->
                if (tracks.isEmpty()) {
                    emptyList()
                } else if (memOrder != null) {
                    val map = tracks.associateBy { it.id }
                    val ordered = memOrder.mapNotNull { map[it] }.toMutableList()
                    val existingIds = ordered.map { it.id }.toSet()
                    tracks.forEach { t ->
                        if (!existingIds.contains(t.id)) {
                            ordered.add(t)
                        }
                    }
                    ordered
                } else {
                    sortTrackList(tracks, sort)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )
        }
    }

    fun setShuffle(enabled: Boolean) {
        playbackManager.setShuffle(enabled)
    }

    fun playWithShuffle(tracks: List<Track>, startTrack: Track? = null) {
        playbackManager.playWithShuffle(tracks, startTrack)
    }

    fun moveQueueTrack(fromIndex: Int, toIndex: Int) {
        if (playbackManager.queue.value.isEmpty() && allTracksOrdered.value.isNotEmpty()) {
            playbackManager.setQueue(allTracksOrdered.value)
        }
        playbackManager.moveQueueTrack(fromIndex, toIndex)
        val newQueue = playbackManager.queue.value
        if (newQueue.isNotEmpty()) {
            val newIds = newQueue.map { it.id }
            _inMemoryAllTracksOrder.value = newIds
            viewModelScope.launch {
                try {
                    settingsDataStore.saveCustomAllTracksOrder(newIds)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun movePlaylistTrack(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val flow = getPlaylistTracks(playlistId)
        val current = flow.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val newIds = current.map { it.id }
        _inMemoryPlaylistOrder.getOrPut(playlistId) { MutableStateFlow(null) }.value = newIds
        settingsDataStore.saveCachedPlaylistOrder(playlistId, newIds)
        viewModelScope.launch {
            try {
                repository.reorderPlaylistTracks(playlistId, newIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveFavoriteTrack(fromIndex: Int, toIndex: Int) {
        val current = favoriteTracks.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val newIds = current.map { it.id }
        _inMemoryFavoriteOrder.value = newIds
        settingsDataStore.saveCachedFavoritesOrder(newIds)
        viewModelScope.launch {
            try {
                repository.reorderFavorites(newIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveAllTracksTrack(fromIndex: Int, toIndex: Int) {
        val current = allTracksOrdered.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val newIds = current.map { it.id }
        _inMemoryAllTracksOrder.value = newIds
        viewModelScope.launch {
            try {
                settingsDataStore.saveCustomAllTracksOrder(newIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (playbackManager.queue.value.isNotEmpty()) {
            playbackManager.setQueue(current)
        }
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

    fun setEqPreamp(preampMilliBels: Int) {
        playbackManager.setEqPreamp(preampMilliBels)
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        playbackManager.setBassBoostEnabled(enabled)
    }

    fun setBassBoostStrength(strength: Int) {
        playbackManager.setBassBoostStrength(strength)
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        playbackManager.setVirtualizerEnabled(enabled)
    }

    fun setVirtualizerStrength(strength: Int) {
        playbackManager.setVirtualizerStrength(strength)
    }

    fun setReverbEnabled(enabled: Boolean) {
        playbackManager.setReverbEnabled(enabled)
    }

    fun setReverbPreset(preset: ReverbPreset) {
        playbackManager.setReverbPreset(preset)
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        playbackManager.setLoudnessEnabled(enabled)
    }

    fun setLoudnessGain(gainMilliBels: Int) {
        playbackManager.setLoudnessGain(gainMilliBels)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
    }

    fun setPlaybackPitch(pitch: Float) {
        playbackManager.setPlaybackPitch(pitch)
    }

    fun resetAudioEffects() {
        playbackManager.resetAudioEffects()
    }

    fun resetEqualizer() {
        playbackManager.resetEqualizer()
    }

    fun resetPlaybackSpeedAndPitch() {
        playbackManager.resetPlaybackSpeedAndPitch()
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
