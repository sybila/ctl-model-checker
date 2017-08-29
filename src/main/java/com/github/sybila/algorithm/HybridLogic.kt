package com.github.sybila.algorithm

import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.StateMap
import com.github.sybila.model.TransitionSystem
import kotlinx.coroutines.experimental.Deferred

interface HybridLogic<S: Any, P: Any> : Algorithm<S, P>, CollectionContext<S, P>, TransitionSystem<S, P> {

    fun makeAt(state: S, innerJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = withDeferred(innerJob) { inner ->
        val result = makeEmptyMap()
        for (s in states) {
            result.lazySet(s, inner[state])
        }
        result
    }

    fun makeBind(inner: List<Pair<S, Deferred<StateMap<S, P>>>>): Deferred<StateMap<S, P>> = makeDeferred {
        val result = makeEmptyMap()
        val startIterator = inner.iterator()
        val awaitIterator = inner.iterator()
        repeat(fork) {
            if (startIterator.hasNext()) startIterator.next().second.start()
        }
        while (awaitIterator.hasNext()) {
            val (state, job) = awaitIterator.next()
            val data = job.await()
            result.lazySet(state, data[state])
            if (startIterator.hasNext()) startIterator.next().second.start()
        }
        result
    }

    fun makeExists(boundJob: Deferred<StateMap<S, P>>, inner: List<Pair<S, Deferred<StateMap<S, P>>>>): Deferred<StateMap<S, P>> = withDeferred(boundJob) { bound ->
        solver.run {
            val result = makeEmptyMap()
            val startIterator = inner.iterator()
            val awaitIterator = inner.iterator()
            repeat(fork) {
                if (startIterator.hasNext()) {
                    val (s, j) = startIterator.next()
                    if (bound[s] != null) j.start()
                }
            }
            while (awaitIterator.hasNext()) {
                val (state, job) = awaitIterator.next()
                if (bound[state] != null) {
                    val data = job.await()
                    data.entries.forEach { (s, p) ->
                        result.increaseKey(s, p and bound[state])
                    }
                }
                if (startIterator.hasNext()) {
                    val (s, j) = startIterator.next()
                    if (bound[s] != null) j.start()
                }
            }

            result
        }
    }

}