package com.github.sybila.algorithm

import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.StateMap
import com.github.sybila.model.TransitionSystem
import kotlinx.coroutines.experimental.Deferred

interface Reachability<S : Any, P : Any> : Algorithm<S, P>, CollectionContext<S, P>, TransitionSystem<S, P> {

    fun makeReachability(reachJob: Deferred<StateMap<S, P>>, time: Boolean): Deferred<StateMap<S, P>>
        = withDeferred(reachJob) { reach ->
        val result = reach.toMutable()

        var recompute = makeEmptySet()
        result.states.forEach { recompute.lazyAdd(it) }

        while (recompute.iterator().hasNext()) {
            val changed = makeEmptySet()
            val update = recompute.flatMap { s -> s.predecessors(time).map { p -> s to p } }
            println("Recompute: ${update.size}")
            consumeParallel(update) { (s, p) ->
                if (result.increaseKey(p, result[s] and transitionBound(p, s, time))) {
                    changed.lazyAdd(p)
                }
            }
            recompute = changed
        }

        result.toReadOnly()
    }

}