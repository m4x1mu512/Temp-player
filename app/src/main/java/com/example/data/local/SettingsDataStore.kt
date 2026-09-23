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
import com.example.data.model.ThemeMode
import com.example.data.model.VisualizerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "temp_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
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
        val name = preferences[KEY_THEME_MODE] ?: ThemeMode.LIGHT.name
        try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.LIGHT
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

    suspend fun savePlaybackState(trackId: Long, position: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_TRACK_ID] = trackId
            preferences[KEY_LAST_POSITION] = position
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

    suspend fun resetSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
