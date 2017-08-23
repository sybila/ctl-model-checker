package com.github.sybila.algorithm

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateMapContext
import com.github.sybila.coroutines.ChunkDispenser
import com.github.sybila.model.TransitionSystem
import kotlinx.coroutines.experimental.Deferred

interface Reachability<S : Any, P : Any> : Algorithm<S, P>, StateMapContext<S, P>, TransitionSystem<S, P> {

    fun makeReachability(reachJob: Deferred<StateMap<S, P>>, time: Boolean): Deferred<StateMap<S, P>>
        = lazyAsync {
        val reach = reachJob.await()
        val result = reach.toMutable()
        val chunks = ChunkDispenser(meanChunkTime)

        //TODO: Search queue heuristic?
        var recompute = reach.states.toList()

        while (recompute.isNotEmpty()) {
            val updated = recompute.mapChunks(chunks) { state ->
                state.predecessors(time).mapNotNull { p ->
                    p.takeIf { result.increaseKey(p, result[state] and transitionBound(p, state, time)) }
                }
            }
            recompute = updated.flatMap { it ?: emptyList() }.toSet().toList()
            println("Recompute: ${recompute.size}")
        }

        result.toReadOnly()
    }

}