package com.github.sybila.algorithm

import com.github.sybila.coroutines.consumeChunks
import com.github.sybila.coroutines.mapChunks
import com.github.sybila.solver.Solver
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

interface Algorithm<S: Any, P: Any> {

    val fork: Int
    val solver: Solver<P>
    val executor: CoroutineContext
    val meanChunkTime: Long

    suspend fun <T> List<T>.consumeChunks(action: Solver<P>.(T) -> Unit)
            = this.consumeChunks({ solver.run { action(it) }}, fork, executor, ChunkDispenser(
            maxChunkSize = (this.size / (2 * fork)).coerceAtLeast(1), meanChunkTime = meanChunkTime
    ))

    suspend fun <T, R> List<T>.mapChunks(chunkDispenser: ChunkDispenser, action: Solver<P>.(T) -> R?): List<R?>
            = this.mapChunks({ solver.run { action(it) }}, fork, executor, chunkDispenser)

    fun <T> lazyAsync(block: suspend CoroutineScope.() -> T): Deferred<T>
            = async(executor, CoroutineStart.LAZY, block)
}