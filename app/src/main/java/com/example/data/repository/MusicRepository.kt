package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlin.math.absoluteValue
import androidx.room.withTransaction
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

/**
 * Android 8-11 ship SQLite builds where SQLITE_MAX_VARIABLE_NUMBER is 999: any `IN (:ids)`
 * bind with a larger list throws "too many SQL variables". Long libraries and long queues
 * hit that limit, so every id list is chunked well below it.
 */
private const val SQL_VARIABLE_LIMIT = 900

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val trackDao = database.trackDao()
    private val favoriteDao = database.favoriteDao()
    private val playlistDao = database.playlistDao()

    // Deliberately does not combine with the favourites table. Baking isFavorite into every
    // Track meant that one heart tap re-created a Track object for every row and re-emitted
    // this flow, which made displayedTracks/allTracksOrdered re-sort and the library screen
    // re-group the whole library. The UI reads favourite state from favoriteIds instead.
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
        .map { entities -> entities.map { it.toTrack(isFavorite = false) } }
        .flowOn(Dispatchers.IO)

    val favoriteIds: Flow<Set<Long>> = favoriteDao.getAllFavoriteIds()
        .map { it.toSet() }
        .flowOn(Dispatchers.IO)

    suspend fun getAllFavoriteIdsDirect(): List<Long> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavoriteIdsDirect()
    }

    // Favourites are mapped here rather than inside allTracks: this flow may re-emit on every
    // heart tap, but it only filters an existing list instead of rebuilding every Track.
    val favoriteTracks: Flow<List<Track>> = combine(allTracks, favoriteIds) { tracks, favIds ->
        tracks.filter { favIds.contains(it.id) }
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

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        // Favourites are deliberately not part of this query. Playlist flows are cached per
        // playlist and the screen shows hearts from favoriteIds, so combining with the
        // favourites table made every heart tap re-run the playlist query for every cached
        // playlist. isFavorite is left false and never read from these objects.
        return playlistDao.getTracksForPlaylist(playlistId)
            .map { entities -> entities.map { it.toTrack(isFavorite = false) } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getTracksByIds(ids: List<Long>): List<Track> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val entities = ids.chunked(SQL_VARIABLE_LIMIT).flatMap { trackDao.getTracksByIds(it) }
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

    fun getTracksByIdsDirect(ids: List<Long>): List<Track> {
        val entities = trackDao.getTracksByIdsDirect(ids)
        val favoriteIds = try {
            favoriteDao.getAllFavoriteIdsSync().toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val entityMap = entities.associateBy { it.id }
        return ids.mapNotNull { id ->
            entityMap[id]?.toTrack(isFavorite = favoriteIds.contains(id))
        }
    }

    fun getAllTracksDirect(): List<Track> {
        val entities = trackDao.getAllTracksDirect()
        val favoriteIds = try {
            favoriteDao.getAllFavoriteIdsSync().toSet()
        } catch (_: Exception) {
            emptySet()
        }
        return entities.map { it.toTrack(isFavorite = favoriteIds.contains(it.id)) }
    }

    /**
     * Embedded covers are written to cacheDir, which Android is free to clear. When the
     * cached file is gone the row must be treated as artwork-less so the next scan
     * re-extracts it instead of keeping a dead file:// URI.
     */
    private fun isUsableArtUri(uriString: String?): Boolean {
        if (uriString.isNullOrEmpty()) return false
        if (!uriString.startsWith("file:")) return true
        return try {
            val path = Uri.parse(uriString).path ?: return false
            File(path).exists()
        } catch (_: Exception) {
            true
        }
    }

    suspend fun scanLocalMusic(folderUris: List<String>): Int = withContext(Dispatchers.IO) {
        if (folderUris.isEmpty()) return@withContext 0

        // Keep both SAF-relative locations and legacy filesystem paths. On Android 10+
        // MediaStore's RELATIVE_PATH + VOLUME_NAME is more reliable than DATA, whose path
        // representation can differ between storage providers and Android versions.
        val selectedFolderRelativePaths = folderUris.mapNotNull { rawUri ->
            try {
                val documentId = DocumentsContract.getTreeDocumentId(Uri.parse(rawUri))
                val separator = documentId.indexOf(':')
                if (separator < 0) return@mapNotNull null
                val volume = documentId.substring(0, separator)
                val mediaStoreVolume = if (volume.equals("primary", ignoreCase = true)) {
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                } else {
                    volume
                }
                mediaStoreVolume to documentId.substring(separator + 1).trim('/')
            } catch (_: Exception) {
                null
            }
        }
        val selectedFolderPaths = folderUris.mapNotNull { rawUri ->
            try {
                val documentId = DocumentsContract.getTreeDocumentId(Uri.parse(rawUri))
                val separator = documentId.indexOf(':')
                if (separator < 0) return@mapNotNull null
                val volume = documentId.substring(0, separator)
                val relativePath = documentId.substring(separator + 1).trim('/')
                val root = if (volume.equals("primary", ignoreCase = true)) {
                    "/storage/emulated/0"
                } else {
                    "/storage/$volume"
                }
                if (relativePath.isBlank()) root else "$root/$relativePath"
            } catch (_: Exception) {
                null
            }
        }
        if (selectedFolderRelativePaths.isEmpty()) return@withContext 0

        val scannedTracks = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.YEAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
                add(MediaStore.MediaColumns.VOLUME_NAME)
            }
        }.toTypedArray()

        // Audio.Media already limits the result to audio files. IS_MUSIC is not reliable
        // for newly downloaded files and can be unset even for valid music tracks.
        val selection: String? = null
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val existingTracksMap = try {
            trackDao.getAllTracksDirect().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        try {
            val queryCursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            ) ?: throw IllegalStateException("Android не вернул список аудиофайлов")
            queryCursor.use { cursor ->
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
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                } else -1
                val volumeNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                    val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol) else null
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val relativePath = if (relativePathCol != -1) {
                        cursor.getString(relativePathCol)?.trim('/') ?: ""
                    } else ""
                    val volumeName = if (volumeNameCol != -1) {
                        cursor.getString(volumeNameCol) ?: ""
                    } else ""
                    val belongsToSelectedFolder = if (
                        relativePathCol != -1 && volumeNameCol != -1 && volumeName.isNotBlank()
                    ) {
                        selectedFolderRelativePaths.any { (selectedVolume, selectedPath) ->
                            volumeName.equals(selectedVolume, ignoreCase = true) &&
                                (selectedPath.isBlank() ||
                                    relativePath == selectedPath ||
                                    relativePath.startsWith("$selectedPath/"))
                        }
                    } else {
                        path.isNotBlank() && selectedFolderPaths.any { folder ->
                            path == folder || path.startsWith(folder.trimEnd('/') + "/")
                        }
                    }
                    if (!belongsToSelectedFolder) continue

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

                    val rawYear = if (yearCol != -1) cursor.getString(yearCol) else null
                    val year = if (!rawYear.isNullOrBlank() && rawYear != "0") rawYear else ""

                    var duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    var albumArtUri: String? = null
                    if (albumId != -1L) {
                        val legacyArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                        try {
                            context.contentResolver.openFileDescriptor(legacyArtUri, "r")?.use {
                                albumArtUri = legacyArtUri.toString()
                            }
                        } catch (_: Exception) {}
                    }

                    val folderName = if (path.isNotBlank()) {
                        try {
                            File(path).parentFile?.name ?: "Разное"
                        } catch (_: Exception) {
                            "Разное"
                        }
                    } else "Разное"

                    var genre = ""

                    // Reuse a SAF-discovered row ID when MediaStore later indexes the
                    // same physical file; this preserves playlist and favourite references.
                    val existingRow = existingTracksMap[id] ?: existingTracksMap.values.firstOrNull {
                        path.isNotBlank() && it.path == path
                    }
                    // Reading tags with MediaMetadataRetriever costs 10-50 ms per file, and
                    // the condition below used to be true for every single track on every
                    // scan (genre is always empty here), so rescanning a few thousand files
                    // took minutes even when nothing had changed. Pay for it only for new
                    // rows, and for existing ones that still lack a duration or a usable cover.
                    val needsMetadata = existingRow == null ||
                        duration <= 0L ||
                        (albumArtUri == null && !isUsableArtUri(existingRow.albumArtUriString))

                    if (needsMetadata) {
                        try {
                            MediaMetadataRetriever().use { mmr ->
                                mmr.setDataSource(context, contentUri)
                                if (duration <= 0) {
                                    val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    duration = durStr?.toLongOrNull() ?: 0L
                                }
                                if (genre.isEmpty()) {
                                    genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""
                                }
                                if (albumArtUri == null) {
                                    val pictureBytes = mmr.embeddedPicture
                                    if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                                        val artDir = File(context.cacheDir, "album_arts").apply { mkdirs() }
                                        val artFile = File(artDir, "art_${id}.jpg")
                                        artFile.writeBytes(pictureBytes)
                                        albumArtUri = Uri.fromFile(artFile).toString()
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            if (path.isNotBlank()) {
                                try {
                                    MediaMetadataRetriever().use { mmr ->
                                        mmr.setDataSource(path)
                                        if (duration <= 0) {
                                            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                            duration = durStr?.toLongOrNull() ?: 0L
                                        }
                                        if (genre.isEmpty()) {
                                            genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""
                                        }
                                        if (albumArtUri == null) {
                                            val pictureBytes = mmr.embeddedPicture
                                            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                                                val artDir = File(context.cacheDir, "album_arts").apply { mkdirs() }
                                                val artFile = File(artDir, "art_${id}.jpg")
                                                artFile.writeBytes(pictureBytes)
                                                albumArtUri = Uri.fromFile(artFile).toString()
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    if (duration <= 0 && size > 0) {
                        duration = (size * 8000L / 192_000L).coerceAtLeast(10_000L)
                    }

                    val existing = existingRow
                    if (existing != null && existing.isCustomEdited) {
                        // Preserve user-edited track details
                        scannedTracks.add(
                            existing.copy(
                                duration = if (duration > 0) duration else existing.duration,
                                size = size,
                                dateAdded = dateAdded,
                                folderName = folderName,
                                path = path,
                                uriString = contentUri.toString(),
                                albumArtUriString = albumArtUri ?: existing.albumArtUriString
                            )
                        )
                    } else {
                        scannedTracks.add(
                            TrackEntity(
                                id = existingRow?.id ?: id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                uriString = contentUri.toString(),
                                // Keep artwork discovered on an earlier scan when this scan
                                // skipped the tag reader (it would otherwise be nulled out).
                                albumArtUriString = albumArtUri ?: existing?.albumArtUriString,
                                size = size,
                                dateAdded = dateAdded,
                                folderName = folderName,
                                path = path,
                                genre = if (genre.isNotBlank()) genre else (existing?.genre ?: ""),
                                year = if (year.isNotBlank()) year else (existing?.year ?: ""),
                                isCustomEdited = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Ошибка чтения аудиофайлов из медиатеки Android", e)
        }

        // SAF fallback: MediaStore can lag behind newly downloaded files (especially on
        // Android 10+ and with download providers). Walk each user-granted tree directly,
        // so an MP3 is discoverable even before Android indexes it.
        var safScanComplete = true
        for (rawTreeUri in folderUris) {
            try {
                val treeUri = Uri.parse(rawTreeUri)
                val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
                val pendingDocumentIds = mutableListOf(rootDocumentId)
                val visitedDocumentIds = HashSet<String>()

                while (pendingDocumentIds.isNotEmpty()) {
                    val parentDocumentId = pendingDocumentIds.removeAt(pendingDocumentIds.lastIndex)
                    if (!visitedDocumentIds.add(parentDocumentId)) continue

                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        treeUri,
                        parentDocumentId
                    )
                    val childrenProjection = arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    )
                    val childrenCursor = context.contentResolver.query(
                        childrenUri,
                        childrenProjection,
                        null,
                        null,
                        null
                    ) ?: throw IllegalStateException("SAF не вернул содержимое выбранной папки")

                    childrenCursor.use { cursor ->
                        val documentIdCol = cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID
                        )
                        val displayNameCol = cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        )
                        val mimeTypeCol = cursor.getColumnIndexOrThrow(
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                        )
                        val sizeCol = cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_SIZE
                        )
                        val modifiedCol = cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        )

                        while (cursor.moveToNext()) {
                            val documentId = cursor.getString(documentIdCol) ?: continue
                            val displayName = cursor.getString(displayNameCol) ?: continue
                            val mimeType = cursor.getString(mimeTypeCol) ?: ""
                            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                                pendingDocumentIds.add(documentId)
                                continue
                            }

                            // This fallback is intentionally limited to MP3 files for now.
                            if (!displayName.endsWith(".mp3", ignoreCase = true) &&
                                !mimeType.equals("audio/mpeg", ignoreCase = true)
                            ) continue

                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                documentId
                            )
                            val uriString = documentUri.toString()
                            val relativeDocumentPath = documentId.substringAfter(':', documentId)
                            val volumeName = documentId.substringBefore(':', "primary")
                            val absolutePath = if (volumeName.equals("primary", ignoreCase = true)) {
                                "/storage/emulated/0/$relativeDocumentPath"
                            } else {
                                "/storage/$volumeName/$relativeDocumentPath"
                            }.trimEnd('/')

                            // If MediaStore already supplied this same file, do not add it twice.
                            if (scannedTracks.any {
                                    it.uriString == uriString ||
                                        (absolutePath.isNotBlank() && it.path == absolutePath)
                                }
                            ) continue

                            val existingByUri = existingTracksMap.values.firstOrNull {
                                it.uriString == uriString
                            }
                            val existingByPath = existingTracksMap.values.firstOrNull {
                                absolutePath.isNotBlank() && it.path == absolutePath
                            }
                            val existing = existingByUri ?: existingByPath
                            val id = existing?.id ?: (
                                (uriString.hashCode().toLong() and 0x3FFFFFFFFFFFFFFFL) or
                                    (1L shl 60)
                                )

                            var title = displayName.substringBeforeLast('.')
                            var artist = "Неизвестный исполнитель"
                            var album = "Неизвестный альбом"
                            var duration = 0L
                            var genre = ""
                            var year = ""
                            var albumArtUri: String? = existing?.albumArtUriString

                            try {
                                MediaMetadataRetriever().use { retriever ->
                                    retriever.setDataSource(context, documentUri)
                                    retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_TITLE
                                    )?.takeIf { it.isNotBlank() && it != "<unknown>" }?.let {
                                        title = it
                                    }
                                    retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_ARTIST
                                    )?.takeIf { it.isNotBlank() && it != "<unknown>" }?.let {
                                        artist = it
                                    }
                                    retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_ALBUM
                                    )?.takeIf { it.isNotBlank() && it != "<unknown>" }?.let {
                                        album = it
                                    }
                                    duration = retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_DURATION
                                    )?.toLongOrNull() ?: 0L
                                    genre = retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_GENRE
                                    ) ?: ""
                                    year = retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_YEAR
                                    ) ?: ""
                                    if (albumArtUri == null) {
                                        val pictureBytes = retriever.embeddedPicture
                                        if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                                            val artDir = File(context.cacheDir, "album_arts").apply {
                                                mkdirs()
                                            }
                                            val artFile = File(artDir, "art_$id.jpg")
                                            artFile.writeBytes(pictureBytes)
                                            albumArtUri = Uri.fromFile(artFile).toString()
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // Metadata is optional; the file itself remains a valid library item.
                            }

                            val size = if (sizeCol != -1 && !cursor.isNull(sizeCol)) {
                                cursor.getLong(sizeCol)
                            } else 0L
                            val modified = if (modifiedCol != -1 && !cursor.isNull(modifiedCol)) {
                                cursor.getLong(modifiedCol) / 1000L
                            } else 0L
                            val folderName = relativeDocumentPath.substringBeforeLast(
                                '/',
                                "Разное"
                            ).substringAfterLast('/').ifBlank { "Разное" }

                            scannedTracks.add(
                                if (existing?.isCustomEdited == true) {
                                    existing.copy(
                                        duration = if (duration > 0L) duration else existing.duration,
                                        size = size,
                                        dateAdded = if (modified > 0L) modified else existing.dateAdded,
                                        folderName = folderName,
                                        path = absolutePath,
                                        uriString = uriString,
                                        albumArtUriString = albumArtUri ?: existing.albumArtUriString
                                    )
                                } else {
                                    TrackEntity(
                                        id = id,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = duration,
                                        uriString = uriString,
                                        albumArtUriString = albumArtUri,
                                        size = size,
                                        dateAdded = if (modified > 0L) modified else
                                            System.currentTimeMillis() / 1000L,
                                        folderName = folderName,
                                        path = absolutePath,
                                        genre = genre,
                                        year = year,
                                        isCustomEdited = false
                                    )
                                }
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Never prune the existing library if even one granted tree could not be read.
                safScanComplete = false
            }
        }

        if (scannedTracks.isNotEmpty()) {
            // One transaction for the whole update. Without it every delete chunk invalidated
            // the tracks flow on its own, and each invalidation rebuilt the entire library
            // (a Track object per row, a sort and a UI recomposition) mid-scan.
            database.withTransaction {
                trackDao.insertTracks(scannedTracks)
                // Keep the library aligned with the folders selected for this scan.
                // `DELETE ... WHERE id NOT IN (:allIds)` used to pass one bind per track and threw
                // "too many SQL variables" for libraries above 999 tracks on Android 8-11, aborting
                // the scan. Compute the stale ids and delete them in chunks instead.
                val validIds = scannedTracks.mapTo(HashSet(scannedTracks.size)) { it.id }
                // Only the ids are needed here. Loading every full TrackEntity inside the write
                // transaction copied the whole library (10k+ objects with their strings) into
                // memory on a large scan and held the database write lock for the whole pass.
                val staleIds = if (safScanComplete) {
                    trackDao.getAllTrackIds().filterNot(validIds::contains)
                } else {
                    emptyList()
                }
                staleIds.chunked(SQL_VARIABLE_LIMIT).forEach { chunk ->
                    trackDao.deleteTracksByIds(chunk)
                }
            }
        }
        scannedTracks.size
    }

    suspend fun updateTrackInfo(
        trackId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: String
    ): Track? = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(trackId) ?: return@withContext null
        val updatedTitle = if (title.isNotBlank()) title else existing.title
        val updatedArtist = if (artist.isNotBlank()) artist else "Неизвестный исполнитель"
        val updatedAlbum = if (album.isNotBlank()) album else "Неизвестный альбом"

        trackDao.updateTrackInfo(
            id = trackId,
            title = updatedTitle,
            artist = updatedArtist,
            album = updatedAlbum,
            genre = genre,
            year = year
        )

        val updatedEntity = trackDao.getTrackById(trackId) ?: return@withContext null
        val isFav = favoriteDao.isFavoriteDirect(trackId)
        updatedEntity.toTrack(isFavorite = isFav)
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

    suspend fun reorderFavorites(orderedTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        if (orderedTrackIds.isNotEmpty()) {
            favoriteDao.reorderFavorites(orderedTrackIds, System.currentTimeMillis())
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = maxPos + 1
            )
        )
    }

    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        if (orderedTrackIds.isNotEmpty()) {
            playlistDao.reorderPlaylistTracks(playlistId, orderedTrackIds)
        }
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
