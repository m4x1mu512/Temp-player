package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val trackDao = database.trackDao()
    private val favoriteDao = database.favoriteDao()
    private val playlistDao = database.playlistDao()

    val allTracks: Flow<List<Track>> = combine(
        trackDao.getAllTracks(),
        favoriteDao.getAllFavoriteIds()
    ) { trackEntities, favoriteIds ->
        val favoriteSet = favoriteIds.toSet()
        trackEntities.map { entity ->
            entity.toTrack(isFavorite = favoriteSet.contains(entity.id))
        }
    }.flowOn(Dispatchers.IO)

    val favoriteTracks: Flow<List<Track>> = allTracks.map { tracks ->
        tracks.filter { it.isFavorite }
    }.flowOn(Dispatchers.IO)

    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return combine(
            playlistDao.getTracksForPlaylist(playlistId),
            favoriteDao.getAllFavoriteIds()
        ) { entities, favoriteIds ->
            val favoriteSet = favoriteIds.toSet()
            entities.map { it.toTrack(isFavorite = favoriteSet.contains(it.id)) }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun scanLocalMusic(): Int = withContext(Dispatchers.IO) {
        val scannedTracks = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                    val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol) else null
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    // Fallback to file display name if title is missing
                    val title = when {
                        !rawTitle.isNullOrBlank() && rawTitle != "<unknown>" -> rawTitle
                        !displayName.isNullOrBlank() -> displayName.substringBeforeLast(".")
                        path.isNotBlank() -> File(path).nameWithoutExtension
                        else -> "Аудиозапись #$id"
                    }

                    val rawArtist = if (artistCol != -1) cursor.getString(artistCol) else null
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                        "Неизвестный исполнитель"
                    } else rawArtist

                    val rawAlbum = if (albumCol != -1) cursor.getString(albumCol) else null
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") {
                        "Неизвестный альбом"
                    } else rawAlbum

                    var duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumArtUri = if (albumId != -1L) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    val folderName = if (path.isNotBlank()) {
                        try {
                            File(path).parentFile?.name ?: "Разное"
                        } catch (_: Exception) {
                            "Разное"
                        }
                    } else "Разное"

                    // If duration is 0, attempt metadata retriever extraction
                    if (duration <= 0 && path.isNotBlank()) {
                        try {
                            MediaMetadataRetriever().use { mmr ->
                                mmr.setDataSource(context, contentUri)
                                val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                duration = durStr?.toLongOrNull() ?: 0L
                            }
                        } catch (_: Exception) {
                            // Safe fallback
                        }
                    }

                    scannedTracks.add(
                        TrackEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            uriString = contentUri.toString(),
                            albumArtUriString = albumArtUri,
                            size = size,
                            dateAdded = dateAdded,
                            folderName = folderName,
                            path = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (scannedTracks.isNotEmpty()) {
            trackDao.insertTracks(scannedTracks)
            trackDao.deleteTracksNotIn(scannedTracks.map { it.id })
        }
        scannedTracks.size
    }

    suspend fun updateTrackDuration(trackId: Long, realDuration: Long) = withContext(Dispatchers.IO) {
        if (realDuration > 0) {
            trackDao.updateTrackDuration(trackId, realDuration)
        }
    }

    suspend fun toggleFavorite(trackId: Long) = withContext(Dispatchers.IO) {
        val favoriteIds = mutableListOf<Long>()
        // Check current favorite state
        val isFav = database.favoriteDao()
        // Simple direct query
        val count = database.openHelper.readableDatabase.compileStatement(
            "SELECT COUNT(*) FROM favorites WHERE trackId = $trackId"
        ).simpleQueryForLong()

        if (count > 0) {
            favoriteDao.removeFavorite(trackId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(trackId = trackId))
        }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(name = newName.trim()))
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = System.currentTimeMillis().toInt()
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }
}
