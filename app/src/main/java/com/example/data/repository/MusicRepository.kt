package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlin.math.absoluteValue
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
import kotlinx.coroutines.flow.first
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

    val favoriteIds: Flow<Set<Long>> = favoriteDao.getAllFavoriteIds()
        .map { it.toSet() }
        .flowOn(Dispatchers.IO)

    suspend fun getAllFavoriteIdsDirect(): List<Long> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavoriteIdsDirect()
    }

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

    suspend fun getTracksByIds(ids: List<Long>): List<Track> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val entities = trackDao.getTracksByIds(ids)
        val favoriteIds = try {
            favoriteDao.getAllFavoriteIds().first().toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val entityMap = entities.associateBy { it.id }
        ids.mapNotNull { id ->
            entityMap[id]?.toTrack(isFavorite = favoriteIds.contains(id))
        }
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
        if (favoriteDao.isFavoriteDirect(trackId)) {
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

    suspend fun getOrCreateTrackFromUri(uri: Uri): Track = withContext(Dispatchers.IO) {
        val uriStr = uri.toString()

        // 1. Check if already present in Room database by URI
        val existingEntity = trackDao.getTrackByUri(uriStr)
        if (existingEntity != null) {
            val favCount = database.openHelper.readableDatabase.compileStatement(
                "SELECT COUNT(*) FROM favorites WHERE trackId = ${existingEntity.id}"
            ).simpleQueryForLong()
            return@withContext existingEntity.toTrack(isFavorite = favCount > 0)
        }

        // 2. If it's a MediaStore content uri, check by parsed ID
        val mediaStoreId = try {
            if (uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY) {
                ContentUris.parseId(uri)
            } else null
        } catch (_: Exception) {
            null
        }

        if (mediaStoreId != null) {
            val trackById = trackDao.getTrackById(mediaStoreId)
            if (trackById != null) {
                val favCount = database.openHelper.readableDatabase.compileStatement(
                    "SELECT COUNT(*) FROM favorites WHERE trackId = ${trackById.id}"
                ).simpleQueryForLong()
                return@withContext trackById.toTrack(isFavorite = favCount > 0)
            }
        }

        // 3. Extract audio metadata using MediaMetadataRetriever
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        var albumArtUriString: String? = null

        try {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(context, uri)
                val metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val metaDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

                if (!metaTitle.isNullOrBlank() && metaTitle != "<unknown>") title = metaTitle
                if (!metaArtist.isNullOrBlank() && metaArtist != "<unknown>") artist = metaArtist
                if (!metaAlbum.isNullOrBlank() && metaAlbum != "<unknown>") album = metaAlbum
                if (metaDuration != null && metaDuration > 0) duration = metaDuration

                val pictureBytes = mmr.embeddedPicture
                if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                    try {
                        val artDir = File(context.cacheDir, "external_arts").apply { mkdirs() }
                        val safeHash = uriStr.hashCode().absoluteValue
                        val artFile = File(artDir, "art_${safeHash}.jpg")
                        artFile.writeBytes(pictureBytes)
                        albumArtUriString = Uri.fromFile(artFile).toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Fallback title from OpenableColumns
        if (title.isNullOrBlank()) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (col != -1) {
                            val displayName = cursor.getString(col)
                            if (!displayName.isNullOrBlank()) {
                                title = displayName.substringBeforeLast(".")
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 5. Fallback title from last path segment
        if (title.isNullOrBlank()) {
            val segment = uri.lastPathSegment?.substringAfterLast("/")
            title = if (!segment.isNullOrBlank()) segment.substringBeforeLast(".") else "Аудиозапись"
        }

        if (artist.isNullOrBlank()) {
            artist = "Внешний файл"
        }
        if (album.isNullOrBlank()) {
            album = "Файловый менеджер"
        }

        val generatedId = mediaStoreId ?: ((uriStr.hashCode().toLong() and 0x3FFFFFFFFFFFFFFFL) or (1L shl 60))

        val newEntity = TrackEntity(
            id = generatedId,
            title = title!!,
            artist = artist!!,
            album = album!!,
            duration = duration,
            uriString = uriStr,
            albumArtUriString = albumArtUriString,
            size = 0L,
            dateAdded = System.currentTimeMillis() / 1000,
            folderName = "Файловый менеджер",
            path = uri.path ?: ""
        )

        try {
            trackDao.insertTrack(newEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        newEntity.toTrack(isFavorite = false)
    }
}
