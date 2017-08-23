package com.github.sybila.algorithm

import com.github.sybila.coroutines.ChunkDispenser
import com.github.sybila.coroutines.consumeChunks
import com.github.sybila.coroutines.mapChunks
import com.github.sybila.solver.Solver
import kotlin.coroutines.experimental.CoroutineContext

interface Algorithm<S: Any, P: Any> {

    val fork: Int
    val solver: Solver<P>
    val executor: CoroutineContext
    val meanChunkTime: Long

    suspend fun <T> List<T>.consumeChunks(action: Solver<P>.(T) -> Unit)
            = this.consumeChunks({ solver.run { action(it) }}, fork, executor, ChunkDispenser(meanChunkTime))

    suspend fun <T, R> List<T>.mapChunks(chunkDispenser: ChunkDispenser, action: Solver<P>.(T) -> R?): List<R?>
            = this.mapChunks({ solver.run { action(it) }}, fork, executor, chunkDispenser)

}