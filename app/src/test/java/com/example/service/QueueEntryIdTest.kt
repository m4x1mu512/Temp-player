package com.example.service

import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * A queue entry is identified by its own id, not by the media id.
 *
 * The id must survive a metadata edit (the track object is replaced, the row is the same) and
 * must stay unique when the same track is in the queue twice (a "remove this row" action has
 * to hit exactly one occurrence).
 */
class QueueEntryIdTest {

    private fun track(id: Long, title: String = "Track $id") = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        uriString = "content://media/external/audio/media/$id",
        albumArtUriString = null
    )

    private fun counter(start: Long): () -> Long {
        var next = start
        return { next++ }
    }

    @Test
    fun `a new queue gets a fresh id for every track`() {
        val tracks = listOf(track(1), track(2))
        val ids = computeQueueEntryIds(emptyList(), emptyList(), tracks, counter(100L))

        assertEquals(listOf(100L, 101L), ids)
    }

    @Test
    fun `an unchanged queue keeps its ids`() {
        val tracks = listOf(track(1), track(2))
        val previousIds = listOf(10L, 11L)

        val ids = computeQueueEntryIds(tracks, previousIds, tracks, counter(100L))

        assertEquals(previousIds, ids)
    }

    @Test
    fun `editing the metadata keeps the entry id`() {
        val previous = listOf(track(1), track(2))
        val previousIds = listOf(10L, 11L)
        // The same media, a new title: the row is the same, only its metadata changed.
        val edited = listOf(previous[0].copy(title = "New title"), previous[1])

        val ids = computeQueueEntryIds(previous, previousIds, edited, counter(100L))

        assertEquals(previousIds, ids)
    }

    @Test
    fun `the same track twice gets two different entry ids`() {
        val previous = listOf(track(1), track(1))
        val previousIds = listOf(10L, 11L)
        val next = listOf(track(1), track(1))

        val ids = computeQueueEntryIds(previous, previousIds, next, counter(100L))

        assertEquals(listOf(10L, 11L), ids)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `a new track gets a new entry id and the others keep theirs`() {
        val previous = listOf(track(1))
        val previousIds = listOf(10L)
        val next = listOf(track(1), track(2))

        val ids = computeQueueEntryIds(previous, previousIds, next, counter(100L))

        assertEquals(listOf(10L, 100L), ids)
    }

    @Test
    fun `reordering carries the entry id with the track`() {
        val previous = listOf(track(1), track(2), track(3))
        val previousIds = listOf(10L, 11L, 12L)
        val next = listOf(previous[2], previous[0], previous[1])

        val ids = computeQueueEntryIds(previous, previousIds, next, counter(100L))

        assertEquals(listOf(12L, 10L, 11L), ids)
    }

    @Test
    fun `a removed track does not take its id with it`() {
        val previous = listOf(track(1), track(2))
        val previousIds = listOf(10L, 11L)
        val next = listOf(track(1))

        val ids = computeQueueEntryIds(previous, previousIds, next, counter(100L))

        assertEquals(listOf(10L), ids)
    }
}
