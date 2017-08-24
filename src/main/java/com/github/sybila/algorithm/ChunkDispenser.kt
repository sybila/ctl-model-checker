package com.github.sybila.algorithm

import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread safe class which tries to maintain an appropriate chunk size
 * based on the feedback from the already completed chunks.
 *
 * Useful for dynamic load balancing of parallel computations.
 *
 * @param [maxChunkSize] Upper limit on the chunk size.
 * @param [meanChunkTime] Desired chunk computation time.
 */
class ChunkDispenser(
        private val maxChunkSize: Int = Int.MAX_VALUE,
        private val meanChunkTime: Long = 25
) {

    private val chunkSize = AtomicInteger(1)

    /**
     * Get current recommended chunk time.
     */
    fun next(): Int = chunkSize.get()

    /**
     * Adjust the recommended chunk time based on an already completed chunk.
     *
     * @param [chunk] The size of the completed chunk (> 0).
     * @param [chunkTime] The chunk computation time (in milliseconds).
     */
    fun adjust(chunk: Int, chunkTime: Long) {
        if (chunkTime < 0.8 * meanChunkTime || chunkTime > 1.2 * meanChunkTime) {
            val itemTime = chunkTime / chunk.toDouble()
            val newChunk = if (itemTime == 0.0) {
                // If the chunk is really fast, chunkTime will be zero and hence also itemTime.
                chunk * 2    // In which case, we just increase the size arbitrarily.
            } else {
                // Otherwise, compute how many items it would take to get perfect mean time.
                (meanChunkTime / itemTime).toInt().coerceAtLeast(1)  // But make it at least 1.
            }
            chunkSize.set(newChunk.coerceAtMost(maxChunkSize))
        }
    }

}