package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The playback state must reach the disk in the order it was produced.
 *
 * Before the fix every snapshot was written by its own coroutine, so an older track/position
 * could land on disk last and the app then restored the wrong track.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStateWriterTest {

    private data class Write(
        val trackId: Long,
        val position: Long,
        val queueIds: List<Long>?,
        val queueIndex: Int
    )

    @Test
    fun `the newest snapshot is written last`() = runTest {
        val writes = mutableListOf<Write>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val writer = PlaybackStateWriter(scope, dispatcher) { trackId, position, queueIds, queueIndex ->
            writes += Write(trackId, position, queueIds, queueIndex)
        }

        writer.submit(1L, 100L, null, 0)
        writer.submit(2L, 200L, null, 1)
        writer.submit(3L, 300L, null, 2)
        testScheduler.advanceUntilIdle()

        assertEquals(3L, writes.last().trackId)
        assertEquals(300L, writes.last().position)
        assertEquals(2, writes.last().queueIndex)

        writer.close()
        scope.cancel()
    }

    @Test
    fun `the writes keep the order of the snapshots`() = runTest {
        val writes = mutableListOf<Write>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val writer = PlaybackStateWriter(scope, dispatcher) { trackId, position, queueIds, queueIndex ->
            writes += Write(trackId, position, queueIds, queueIndex)
        }

        writer.submit(1L, 10L, null, 0)
        testScheduler.advanceUntilIdle()
        writer.submit(2L, 20L, null, 0)
        testScheduler.advanceUntilIdle()
        writer.submit(3L, 30L, null, 0)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), writes.map { it.trackId })
        assertEquals(listOf(10L, 20L, 30L), writes.map { it.position })

        writer.close()
        scope.cancel()
    }

    @Test
    fun `the queue is written once and only when it changed`() = runTest {
        val writes = mutableListOf<Write>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val writer = PlaybackStateWriter(scope, dispatcher) { trackId, position, queueIds, queueIndex ->
            writes += Write(trackId, position, queueIds, queueIndex)
        }

        writer.submit(1L, 0L, listOf(10L, 20L), 0)
        testScheduler.advanceUntilIdle()
        writer.submit(1L, 1000L, null, 0)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(10L, 20L), writes[0].queueIds)
        // The second write carries no queue: the stored one is still valid.
        assertNull(writes[1].queueIds)

        writer.close()
        scope.cancel()
    }

    @Test
    fun `the queue is not lost when a later snapshot replaces the one that carried it`() = runTest {
        val writes = mutableListOf<Write>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val writer = PlaybackStateWriter(scope, dispatcher) { trackId, position, queueIds, queueIndex ->
            writes += Write(trackId, position, queueIds, queueIndex)
        }

        // Both snapshots are submitted before the writer picks anything up, so the first one
        // is conflated away. The queue must still reach the disk.
        writer.submit(1L, 0L, listOf(10L, 20L), 0)
        writer.submit(1L, 500L, null, 0)
        testScheduler.advanceUntilIdle()

        assertEquals(1, writes.size)
        assertEquals(listOf(10L, 20L), writes[0].queueIds)
        assertEquals(500L, writes[0].position)

        writer.close()
        scope.cancel()
    }

    @Test
    fun `a failed write does not stop the writer`() = runTest {
        val writes = mutableListOf<Write>()
        var failNext = true
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val writer = PlaybackStateWriter(scope, dispatcher) { trackId, position, queueIds, queueIndex ->
            if (failNext) {
                failNext = false
                throw IllegalStateException("disk is full")
            }
            writes += Write(trackId, position, queueIds, queueIndex)
        }

        writer.submit(1L, 10L, null, 0)
        testScheduler.advanceUntilIdle()
        writer.submit(2L, 20L, null, 0)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(2L), writes.map { it.trackId })

        writer.close()
        scope.cancel()
    }
}
