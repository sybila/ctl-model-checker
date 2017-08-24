package com.github.sybila.algorithm

import com.github.sybila.solver.Solver
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

fun <R> makeDeferred(context: CoroutineContext, action: suspend () -> R): Deferred<R>
        = async(context, CoroutineStart.LAZY) { action() }

fun <A, R> withDeferred(context: CoroutineContext, j: Deferred<A>, action: suspend (A) -> R): Deferred<R>
        = async(context, CoroutineStart.LAZY) { j.start(); action(j.await()) }

fun <A, B, R> withDeferred(context: CoroutineContext, j1: Deferred<A>, j2: Deferred<B>, action: suspend (A, B) -> R): Deferred<R>
        = async(context, CoroutineStart.LAZY) { j1.start(); j2.start(); action(j1.await(), j2.await()) }

inline suspend fun <S: Any, P: Any, T> Algorithm<S, P>.consumeParallel(data: List<T>, crossinline action: Solver<P>.(T) -> Unit) {
    val dispenser = ChunkDispenser((data.size / (2*fork)).coerceAtLeast(1), meanChunkTime)
    val chunks = data.chunks(executor, dispenser)
    (1..fork).map { async(executor) {
        solver.run {
            chunks.consumeEach { range ->
                val chunkTime = measureTimeMillis {
                    range.forEach { action(data[it]) }
                }
                dispenser.adjust(range.last - range.first + 1, chunkTime)
            }
        }
    } }.map { it.await() }
}

/**
 * Lazily divide this list into chunks based on the given [chunkDispenser].
 */
fun List<*>.chunks(executor: CoroutineContext, chunkDispenser: ChunkDispenser): ProducerJob<IntRange>
        = produce<IntRange>(executor) {
    val limit = this@chunks.size
    var chunkStart = 0
    while (chunkStart != limit) {
        val chunk = chunkDispenser.next()
        val chunkEnd = (chunkStart + chunk).coerceAtMost(limit)
        send(chunkStart until chunkEnd)
        chunkStart = chunkEnd
    }
    close()
}