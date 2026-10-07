package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EqualizerPreset
import com.example.data.model.MiniPlayerBgMode
import com.example.data.model.RepeatMode
import com.example.data.model.SortOrder
import com.example.data.model.ThemeMode
import com.example.data.model.VisualizerMode
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "temp_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private const val PREFS_CACHE_NAME = "app_theme_cache"
        private const val KEY_CACHED_THEME_MODE = "cached_theme_mode"
        private const val KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER = "cached_custom_all_tracks_order"
        private const val KEY_CACHED_QUEUE_TRACK_IDS = "cached_queue_track_ids"
        private const val KEY_CACHED_LAST_QUEUE_INDEX = "cached_last_queue_index"
        private const val KEY_CACHED_LAST_TRACK_ID = "cached_last_track_id"
        private const val KEY_CACHED_LAST_POSITION = "cached_last_position"
        private const val KEY_CACHED_SORT_ORDER = "cached_sort_order"

        fun getInitialThemeMode(context: Context): ThemeMode {
            val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_CACHED_THEME_MODE, null)
            if (name != null) {
                return try {
                    ThemeMode.valueOf(name)
                } catch (_: Exception) {
                    ThemeMode.SYSTEM
                }
            }

            // Fallback for existing installations: scan DataStore preferences file synchronously
            try {
                val dataStoreFile = File(context.filesDir, "datastore/temp_settings.preferences_pb")
                if (dataStoreFile.exists()) {
                    val content = dataStoreFile.readBytes().toString(Charsets.ISO_8859_1)
                    if (content.contains("theme_mode")) {
                        val detected = when {
                            content.contains("DARK") -> ThemeMode.DARK
                            content.contains("LIGHT") -> ThemeMode.LIGHT
                            content.contains("SYSTEM") -> ThemeMode.SYSTEM
                            else -> null
                        }
                        if (detected != null) {
                            saveCachedThemeMode(context, detected)
                            return detected
                        }
                    }
                }
            } catch (_: Exception) {}

            return ThemeMode.SYSTEM
        }

        fun saveCachedThemeMode(context: Context, mode: ThemeMode) {
            try {
                context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHED_THEME_MODE, mode.name)
                    .commit()
            } catch (_: Exception) {}
        }

        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        private val KEY_VISUALIZER_MODE = stringPreferencesKey("visualizer_mode")
        private val KEY_VISUALIZER_BANDS = intPreferencesKey("visualizer_bands")
        private val KEY_VISUALIZER_SENSITIVITY = floatPreferencesKey("visualizer_sensitivity")
        private val KEY_LAST_TRACK_ID = longPreferencesKey("last_track_id")
        private val KEY_LAST_POSITION = longPreferencesKey("last_position")
        private val KEY_REPEAT_MODE = stringPreferencesKey("repeat_mode")
        private val KEY_SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        private val KEY_EQ_PRESET = stringPreferencesKey("eq_preset")
        private val KEY_EQ_LEVELS = stringPreferencesKey("eq_levels")
        private val KEY_MINI_PLAYER_BG_MODE = stringPreferencesKey("mini_player_bg_mode")
        private val KEY_MINI_PLAYER_CUSTOM_COLOR = longPreferencesKey("mini_player_custom_color")
        private val KEY_AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        private val KEY_REPLAY_GAIN_ENABLED = booleanPreferencesKey("replay_gain_enabled")
        private val KEY_CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        private val KEY_CROSSFADE_DURATION_SECONDS = intPreferencesKey("crossfade_duration_seconds")
        private val KEY_QUEUE_TRACK_IDS = stringPreferencesKey("queue_track_ids")
        private val KEY_LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        private val KEY_CUSTOM_ALL_TRACKS_ORDER = stringPreferencesKey("custom_all_tracks_order")
    }

    fun getInitialCustomAllTracksOrder(): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER, null) ?: ""
        if (raw.isNotBlank()) {
            return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
        val queueRaw = prefs.getString(KEY_CACHED_QUEUE_TRACK_IDS, null) ?: ""
        return if (queueRaw.isBlank()) emptyList() else queueRaw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun getInitialQueueTrackIds(): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CACHED_QUEUE_TRACK_IDS, null) ?: ""
        if (raw.isNotBlank()) {
            return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
        val customRaw = prefs.getString(KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER, null) ?: ""
        return if (customRaw.isBlank()) emptyList() else customRaw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun getInitialFavoritesOrder(): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("cached_favorites_order", null) ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun saveCachedFavoritesOrder(ids: List<Long>) {
        try {
            context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("cached_favorites_order", ids.joinToString(","))
                .commit()
        } catch (_: Exception) {}
    }

    fun getInitialSortOrder(): SortOrder {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CACHED_SORT_ORDER, null) ?: return SortOrder.BY_TITLE
        return try {
            SortOrder.valueOf(raw)
        } catch (_: Exception) {
            SortOrder.BY_TITLE
        }
    }

    fun saveCachedSortOrder(order: SortOrder) {
        try {
            context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_SORT_ORDER, order.name)
                .commit()
        } catch (_: Exception) {}
    }

    fun getInitialPlaylistOrder(playlistId: Long): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("cached_playlist_order_$playlistId", null) ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun saveCachedPlaylistOrder(playlistId: Long, ids: List<Long>) {
        try {
            context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("cached_playlist_order_$playlistId", ids.joinToString(","))
                .commit()
        } catch (_: Exception) {}
    }

    fun getInitialLastQueueIndex(): Int {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CACHED_LAST_QUEUE_INDEX, -1)
    }

    fun getInitialLastTrackId(): Long {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_CACHED_LAST_TRACK_ID, -1L)
    }

    fun getInitialLastPosition(): Long {
        val prefs = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_CACHED_LAST_POSITION, 0L)
    }

    val customAllTracksOrderFlow: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_CUSTOM_ALL_TRACKS_ORDER] ?: ""
        if (raw.isBlank()) {
            getInitialCustomAllTracksOrder()
        } else {
            raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
    }.onStart {
        val initial = getInitialCustomAllTracksOrder()
        emit(initial)
    }

    suspend fun saveCustomAllTracksOrder(trackIds: List<Long>) {
        val str = trackIds.joinToString(",")
        try {
            context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER, str)
                .putString(KEY_CACHED_QUEUE_TRACK_IDS, str)
                .commit()
        } catch (_: Exception) {}
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_ALL_TRACKS_ORDER] = str
            preferences[KEY_QUEUE_TRACK_IDS] = str
        }
    }

    val queueTrackIdsFlow: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_QUEUE_TRACK_IDS] ?: ""
        if (raw.isBlank()) {
            getInitialQueueTrackIds()
        } else {
            raw.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
    }.onStart {
        val initial = getInitialQueueTrackIds()
        emit(initial)
    }

    val lastQueueIndexFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_QUEUE_INDEX] ?: -1
    }

    val crossfadeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CROSSFADE_ENABLED] ?: true
    }

    val crossfadeDurationSecondsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_CROSSFADE_DURATION_SECONDS] ?: 4
    }

    val replayGainEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REPLAY_GAIN_ENABLED] ?: true
    }

    val autoRotateFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_ROTATE] ?: true
    }

    val miniPlayerBgModeFlow: Flow<MiniPlayerBgMode> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_MINI_PLAYER_BG_MODE] ?: MiniPlayerBgMode.ALBUM_ART.name
        try {
            MiniPlayerBgMode.valueOf(name)
        } catch (_: Exception) {
            MiniPlayerBgMode.ALBUM_ART
        }
    }

    val miniPlayerCustomColorFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_MINI_PLAYER_CUSTOM_COLOR] ?: 0xFF1E1F24L
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_THEME_MODE]
        if (name != null) {
            try {
                saveCachedThemeMode(context, ThemeMode.valueOf(name))
            } catch (_: Exception) {}
        }
        val currentName = name ?: getInitialThemeMode(context).name
        try {
            ThemeMode.valueOf(currentName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val visualizerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_VISUALIZER_ENABLED] ?: true
    }

    val visualizerModeFlow: Flow<VisualizerMode> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_VISUALIZER_MODE] ?: VisualizerMode.AMPLITUDE.name
        try {
            VisualizerMode.valueOf(name)
        } catch (_: Exception) {
            VisualizerMode.AMPLITUDE
        }
    }

    val visualizerBandsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_VISUALIZER_BANDS] ?: 32
    }

    val visualizerSensitivityFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_VISUALIZER_SENSITIVITY] ?: 1.0f
    }

    val lastTrackIdFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_TRACK_ID] ?: -1L
    }

    val lastPositionFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_POSITION] ?: 0L
    }

    val repeatModeFlow: Flow<RepeatMode> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_REPEAT_MODE] ?: RepeatMode.OFF.name
        try {
            RepeatMode.valueOf(name)
        } catch (_: Exception) {
            RepeatMode.OFF
        }
    }

    val shuffleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHUFFLE_ENABLED] ?: false
    }

    val eqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_EQ_ENABLED] ?: true
    }

    val eqPresetFlow: Flow<EqualizerPreset> = context.dataStore.data.map { preferences ->
        val name = preferences[KEY_EQ_PRESET] ?: EqualizerPreset.FLAT.name
        try {
            EqualizerPreset.valueOf(name)
        } catch (_: Exception) {
            EqualizerPreset.FLAT
        }
    }

    val eqLevelsFlow: Flow<List<Int>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_EQ_LEVELS] ?: "0,0,0,0,0"
        raw.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { listOf(0, 0, 0, 0, 0) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        saveCachedThemeMode(context, mode)
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VISUALIZER_ENABLED] = enabled
        }
    }

    suspend fun setVisualizerMode(mode: VisualizerMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VISUALIZER_MODE] = mode.name
        }
    }

    suspend fun setVisualizerBands(bands: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VISUALIZER_BANDS] = bands
        }
    }

    suspend fun setVisualizerSensitivity(sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VISUALIZER_SENSITIVITY] = sensitivity
        }
    }

    suspend fun savePlaybackState(
        trackId: Long,
        position: Long,
        queueIds: List<Long>? = null,
        queueIndex: Int? = null
    ) {
        try {
            val editor = context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE).edit()
            editor.putLong(KEY_CACHED_LAST_TRACK_ID, trackId)
            editor.putLong(KEY_CACHED_LAST_POSITION, position)
            if (queueIds != null) {
                val qStr = queueIds.joinToString(",")
                editor.putString(KEY_CACHED_QUEUE_TRACK_IDS, qStr)
                editor.putString(KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER, qStr)
            }
            if (queueIndex != null) {
                editor.putInt(KEY_CACHED_LAST_QUEUE_INDEX, queueIndex)
            }
            editor.commit()
        } catch (_: Exception) {}
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_TRACK_ID] = trackId
            preferences[KEY_LAST_POSITION] = position
            if (queueIds != null) {
                val qStr = queueIds.joinToString(",")
                preferences[KEY_QUEUE_TRACK_IDS] = qStr
                preferences[KEY_CUSTOM_ALL_TRACKS_ORDER] = qStr
            }
            if (queueIndex != null) {
                preferences[KEY_LAST_QUEUE_INDEX] = queueIndex
            }
        }
    }

    suspend fun saveQueue(trackIds: List<Long>, queueIndex: Int) {
        val qStr = trackIds.joinToString(",")
        try {
            context.getSharedPreferences(PREFS_CACHE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_QUEUE_TRACK_IDS, qStr)
                .putString(KEY_CACHED_CUSTOM_ALL_TRACKS_ORDER, qStr)
                .putInt(KEY_CACHED_LAST_QUEUE_INDEX, queueIndex)
                .commit()
        } catch (_: Exception) {}
        context.dataStore.edit { preferences ->
            preferences[KEY_QUEUE_TRACK_IDS] = qStr
            preferences[KEY_CUSTOM_ALL_TRACKS_ORDER] = qStr
            preferences[KEY_LAST_QUEUE_INDEX] = queueIndex
        }
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REPEAT_MODE] = mode.name
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_ENABLED] = enabled
        }
    }

    suspend fun setEqualizerPreset(preset: EqualizerPreset) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_PRESET] = preset.name
        }
    }

    suspend fun setEqualizerLevels(levels: List<Int>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_LEVELS] = levels.joinToString(",")
        }
    }

    suspend fun setMiniPlayerBgMode(mode: MiniPlayerBgMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MINI_PLAYER_BG_MODE] = mode.name
        }
    }

    suspend fun setMiniPlayerCustomColor(color: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MINI_PLAYER_CUSTOM_COLOR] = color
        }
    }

    suspend fun setAutoRotate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_ROTATE] = enabled
        }
    }

    suspend fun setReplayGainEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REPLAY_GAIN_ENABLED] = enabled
        }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CROSSFADE_ENABLED] = enabled
        }
    }

    suspend fun setCrossfadeDurationSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CROSSFADE_DURATION_SECONDS] = seconds.coerceIn(1, 10)
        }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
