package com.github.sybila.algorithm

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateMapContext
import com.github.sybila.model.TransitionSystem
import com.github.sybila.solver.Solver
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

interface Reachability<S : Any, P : Any> : Algorithm<S, P>, StateMapContext<S, P>, TransitionSystem<S, P> {

    fun makeReachability(reachJob: Deferred<StateMap<S, P>>, time: Boolean): Deferred<StateMap<S, P>>
        = lazyAsync {
        val reach = reachJob.await()
        val result = reach.toMutable()
        //val chunks = ChunkDispenser(meanChunkTime)

        //TODO: Search queue heuristic?
        var recompute: List<Pair<S, S?>> = reach.states.map { it to null }.toList()

        while (recompute.isNotEmpty()) {
            val changed = recompute.mapChunksInline(algorithm = this@Reachability, fork = fork, executor = executor, meanChunkTime = meanChunkTime, action = { (state, dep) ->
                if (dep == null) state else {
                    state.takeIf { result.increaseKey(state, transitionBound(state, dep, time) and result[dep]) }
                }
            }).toSet()
            val r = ArrayList<Pair<S, S?>>(changed.size * 4)
            changed.forEach { it?.let { s -> s.predecessors(time).forEach { p -> r.add(p to s) } } }
            recompute = r
            println("Recompute: ${recompute.size}")
        }
/*
        val queue = TierQueue<S>()
        reach.states.forEach { queue.add(it, null) }

        println("Start reachability!!")
        while (queue.isNotEmpty()) {
            val tier = queue.remove().toList()
            val changed = tier.mapChunksInline(algorithm = this@Reachability, fork = fork,
                    executor = executor, chunkDispenser = chunks, action = { (s, f) ->
                if (f == null) s else {
                    s.takeIf { result.increaseKey(s, result[f] and transitionBound(s, f, time)) }
                }
            })
            println("Changed: ${changed.size}")
            changed.forEach {
                it?.let { s -> s.predecessors(time).forEach { p -> queue.add(p, s) } }
            }
        }
        println("Done!")*/

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
        meanChunkTime: Long
) : List<R?> {
    val chunkSize = AtomicInteger(1)
    // what a nice warning you have here...
    val result = Arrays.asList<R?>(*(arrayOfNulls<Any?>(this.size) as Array<out R>))
    val original = this@mapChunksInline
    val chunks = produce<IntRange>(executor) {
        var chunkStart = 0  //inclusive
        while (chunkStart != original.size) {
            val chunk = chunkSize.get()
            val chunkEnd = Math.min(original.size, chunkStart + chunk)  //exclusive
            send(chunkStart until chunkEnd)
            chunkStart = chunkEnd
        }
        original.take(10)
        close()
    }
    (1..fork).map {
        async(executor) {
            chunks.consumeEach { items ->
                val solver = algorithm.solver
                val elapsed = measureTimeMillis {
                    items.forEach {
                        result[it] = solver.action(original[it])
                    }
                }
                if (elapsed < 0.8 * meanChunkTime || elapsed > 1.2 * meanChunkTime) {
                    val chunk = items.last - items.first + 1    // + 1 for inclusive range
                    val itemTime = elapsed / chunk.toDouble()
                    if (itemTime == 0.0) {
                        // If we are real fast, we can get a zero elapsed time and hence a zero item time
                        chunkSize.set(2 * chunk)    // in which case, just increase the chunk arbitrarily.
                    } else {
                        // On the other hand, we can also be really slow and get a zero chunk size
                        chunkSize.set(Math.max(1, (meanChunkTime / itemTime).toInt()))  // hence use a lower bound.
                    }
                }
            }
        }
    }.forEach { it.await() }
    return result
    /*val list = this
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
    return result*/
}