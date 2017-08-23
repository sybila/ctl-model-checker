package com.github.sybila.algorithm

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateMapContext
import com.github.sybila.coroutines.lazyAsync
import kotlinx.coroutines.experimental.Deferred

interface BooleanLogic<S: Any, P: Any> : StateMapContext<S, P>, Algorithm<S, P> {

    fun makeAnd(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
        = lazyAsync(executor) {
        val left = leftJob.await()
        val right = rightJob.await()
        val result = left.toMutable()
        left.states.toList().consumeChunks { state ->
            result.lazySet(state, (left[state] and right[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

    fun makeOr(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
        = lazyAsync(executor) {
        val left = leftJob.await()
        val right = rightJob.await()
        val result = left.toMutable()
        right.states.toList().consumeChunks { state ->
            result.lazySet(state, (left[state] or right[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

    fun makeComplement(targetJob: Deferred<StateMap<S, P>>, againstJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
        = lazyAsync(executor) {
        val target = targetJob.await()
        val against = againstJob.await()
        val result = against.toMutable()
        target.states.toList().consumeChunks { state ->
            result.lazySet(state, (target[state] complement against[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

}