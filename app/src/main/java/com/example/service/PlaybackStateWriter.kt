package com.example.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Persists the playback state in the order the states were produced.
 *
 * A mutex alone does not guarantee that: the snapshot was read before the coroutine that
 * writes it was scheduled, so two writes could still reach the store in the opposite order
 * and leave an older track/position on disk. A conflated channel with a single writer
 * keeps exactly one pending snapshot - the newest one - and writes them strictly one after
 * another, so the last state produced is the last state stored.
 */
internal class PlaybackStateWriter(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val save: suspend (trackId: Long, position: Long, queueIds: List<Long>?, queueIndex: Int) -> Unit
) {
    private data class Request(
        val trackId: Long,
        val position: Long,
        val queueIds: List<Long>?,
        val queueIndex: Int
    )

    private val requests = Channel<Request>(Channel.CONFLATED)

    /**
     * The queue id list is written separately from the position. It must not be lost when a
     * later snapshot without queue ids replaces an earlier one that still had to be written.
     */
    @Volatile
    private var pendingQueueIds: List<Long>? = null

    /** The queue that is already on disk, so the same list is not written over and over. */
    @Volatile
    private var writtenQueueIds: List<Long>? = null

    private val writerJob: Job = scope.launch(dispatcher) {
        for (request in requests) {
            // Only write the queue when it really differs from what is stored already.
            val queueToWrite = request.queueIds?.takeIf { it != writtenQueueIds }
            try {
                save(request.trackId, request.position, queueToWrite, request.queueIndex)
                if (queueToWrite != null) {
                    writtenQueueIds = request.queueIds
                }
                if (request.queueIds != null && request.queueIds == writtenQueueIds &&
                    pendingQueueIds == request.queueIds
                ) {
                    pendingQueueIds = null
                }
            } catch (_: Exception) {
                // A failed write must not stop the writer: the next snapshot carries the
                // current state anyway.
            }
        }
    }

    /**
     * @param queueIds the full queue id list when it changed since the last write, null when
     * only the position and the current track changed.
     */
    fun submit(trackId: Long, position: Long, queueIds: List<Long>?, queueIndex: Int) {
        if (queueIds != null) pendingQueueIds = queueIds
        requests.trySend(Request(trackId, position, pendingQueueIds, queueIndex))
    }

    fun close() {
        requests.close()
        writerJob.cancel()
    }
}
