package com.github.sybila.coroutines

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

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

/**
 * Consume items from this list in parallel. The consumption uses [fork] independent
 * consumers which operate on given [executor]. The list is consumed in chunks, guided by [chunkDispenser].
 *
 * Use [context] to provide context for each chunk execution.
 */
suspend fun <T> List<T>.consumeChunks(
        action: (T) -> Unit,
        fork: Int = 1,
        executor: CoroutineContext = CommonPool,
        chunkDispenser: ChunkDispenser = ChunkDispenser(maxChunkSize = (this.size / (2*fork)).coerceAtLeast(1))
): Unit {
    val list = this
    val chunks = this.chunks(executor, chunkDispenser)
    (1..fork).map {
        async(executor) {
            chunks.consumeEach { items ->
                val chunkTime = measureTimeMillis {
                    items.forEach { action(list[it]) }
                }
                chunkDispenser.adjust(items.last - items.first + 1, chunkTime)
            }
        }
    }.map { it.await() }
    println("Chunk size: ${chunkDispenser.next()}")
}

/**
 * Map items from this list using [action] in parallel. The consumption uses [fork] independent
 * consumers which operate on given [executor]. The list is consumed in chunks, guided by [chunkDispenser].
 *
 * Use [context] to provide context for each chunk execution.
 */
suspend fun <T, R> List<T>.mapChunks(
        action: (T) -> R?,
        fork: Int = 1,
        executor: CoroutineContext = CommonPool,
        chunkDispenser: ChunkDispenser = ChunkDispenser()
) : List<R?> {
    val list = this
    val result = ArrayList<R?>(list.size).apply { (0 until list.size).forEach { add(null) }  }
    val chunks = this.chunks(executor, chunkDispenser)
    (1..fork).map {
        async(executor) {
            chunks.consumeEach { items ->
                val chunkTime = measureTimeMillis {
                    items.forEach {
                        result[it] = action(list[it])
                    }
                }
                chunkDispenser.adjust(items.last - items.first + 1, chunkTime)
            }
        }
    }.map { it.await() }
    return result
}

/**
 * Build a lazy async task on the given [executor].
 */
fun <T> lazyAsync(executor: CoroutineContext, block: suspend CoroutineScope.() -> T): Deferred<T>
        = async(executor, CoroutineStart.LAZY, block)