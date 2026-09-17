package com.example.data.model

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val albumArtUriString: String?,
    val size: Long = 0,
    val dateAdded: Long = 0,
    val folderName: String = "",
    val path: String = "",
    val isFavorite: Boolean = false
) {
    val uri: Uri get() = Uri.parse(uriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }

    fun formattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            val remainingMinutes = minutes % 60
            String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SortOrder(val displayName: String) {
    BY_TITLE("По названию"),
    BY_ARTIST("По исполнителю"),
    BY_ALBUM("По альбому"),
    BY_DURATION("По длительности"),
    BY_DATE_ADDED("Недавно добавленные")
}

enum class VisualizerMode(val displayName: String) {
    SPECTRUM("Спектр"),
    WAVE("Волна"),
    CIRCLE("Круг")
}

enum class ThemeMode(val displayName: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    SYSTEM("Системная")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelMilliBels: Short
)

enum class EqualizerPreset(val displayName: String) {
    FLAT("Обычный"),
    ROCK("Рок"),
    JAZZ("Джаз"),
    POP("Поп"),
    CLASSICAL("Классика"),
    CUSTOM("Пользовательский")
}
