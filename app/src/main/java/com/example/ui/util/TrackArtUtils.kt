package com.example.ui.util

import android.net.Uri
import com.example.data.model.Track

/**
 * First [limit] distinct cover URIs of a group of tracks.
 *
 * Cards used to build their collage with `mapNotNull { it.albumArtUri }.distinct().take(4)`,
 * which parses the cover URI of *every* track in the group - and [Track.albumArtUri] parses
 * its string on each access. A card shows at most four covers, so stop as soon as four
 * distinct ones are found. Order and content match the previous expression.
 */
fun List<Track>.firstDistinctArtUris(limit: Int = 4): List<Uri> {
    if (isEmpty() || limit <= 0) return emptyList()
    val result = ArrayList<Uri>(limit)
    for (track in this) {
        val uri = track.albumArtUri ?: continue
        if (result.none { it == uri }) {
            result.add(uri)
            if (result.size == limit) break
        }
    }
    return result
}
