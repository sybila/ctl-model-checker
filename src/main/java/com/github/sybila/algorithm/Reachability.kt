package com.github.sybila.algorithm

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateMapContext
import com.github.sybila.coroutines.ChunkDispenser
import com.github.sybila.coroutines.chunks
import com.github.sybila.model.TransitionSystem
import com.github.sybila.solver.Solver
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

interface Reachability<S : Any, P : Any> : Algorithm<S, P>, StateMapContext<S, P>, TransitionSystem<S, P> {

    fun makeReachability(reachJob: Deferred<StateMap<S, P>>, time: Boolean): Deferred<StateMap<S, P>>
        = lazyAsync {
        val reach = reachJob.await()
        val result = reach.toMutable()
        val chunks = ChunkDispenser(meanChunkTime)

        //TODO: Search queue heuristic?
        var recompute = reach.states.toList()

        while (recompute.isNotEmpty()) {
            val r = HashSet<S>()
            recompute.mapChunksInline(algorithm = this@Reachability, fork = fork, executor = executor, chunkDispenser = chunks, action = { state ->
                state.predecessors(time).mapNotNull { p ->
                    p.takeIf { result.increaseKey(p, result[state] and transitionBound(p, state, time)) }
                }
            }).forEach { it?.forEach { r.add(it) } }
            recompute = r.toList()//updated.flatMap { it ?: emptyList() }.toSet().toList()
            println("Recompute: ${recompute.size}")
        }

        result.toReadOnly()
    }

}

/**
 * Map items from this list using [action] in parallel. The consumption uses [fork] independent
 * consumers which operate on given [executor]. The list is consumed in chunks, guided by [chunkDispenser].
 *
 * Use [context] to provide context for each chunk execution.
 */
inline suspend fun <T, R, P:Any> List<T>.mapChunksInline(
        algorithm: Algorithm<*, P>,
        crossinline action: Solver<P>.(T) -> R?,
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
                    algorithm.solver.run {
                        items.forEach {
                            result[it] = action(list[it])
                        }
                    }
                }
                chunkDispenser.adjust(items.last - items.first + 1, chunkTime)
            }
        }
    }.map { it.await() }
    return result
}