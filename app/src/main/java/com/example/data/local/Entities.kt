package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val albumArtUriString: String?,
    val size: Long,
    val dateAdded: Long,
    val folderName: String,
    val path: String
) {
    fun toTrack(isFavorite: Boolean = false): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uriString = uriString,
        albumArtUriString = albumArtUriString,
        size = size,
        dateAdded = dateAdded,
        folderName = folderName,
        path = path,
        isFavorite = isFavorite
    )

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            uriString = track.uriString,
            albumArtUriString = track.albumArtUriString,
            size = track.size,
            dateAdded = track.dateAdded,
            folderName = track.folderName,
            path = track.path
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["trackId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int = 0
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
