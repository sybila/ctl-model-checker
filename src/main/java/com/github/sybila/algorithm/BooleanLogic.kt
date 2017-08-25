package com.github.sybila.algorithm

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.CollectionContext
import kotlinx.coroutines.experimental.Deferred

interface BooleanLogic<S: Any, P: Any> : CollectionContext<S, P>, Algorithm<S, P> {

    fun makeAnd(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
            = withDeferred(leftJob, rightJob) { left, right ->
        val result = left.toMutable()
        consumeParallel(left.states.toList()) { state ->
            result.lazySet(state, (left[state] and right[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

    fun makeOr(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
            = withDeferred(leftJob, rightJob) { left, right ->
        val result = left.toMutable()
        consumeParallel(right.states.toList()) { state ->
            result.lazySet(state, (left[state] or right[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

    fun makeComplement(targetJob: Deferred<StateMap<S, P>>, againstJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>>
            = withDeferred(targetJob, againstJob) { target, against ->
        val result = against.toMutable()
        consumeParallel(target.states.toList()) { state ->
            result.lazySet(state, (target[state] complement against[state])?.takeIfNotEmpty())
        }
        result.toReadOnly()
    }

}