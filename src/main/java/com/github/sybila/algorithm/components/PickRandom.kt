package com.github.sybila.algorithm.components

import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.StateMap
import com.github.sybila.solver.Solver
import java.util.*


/**
 * Very simple implementation of [PivotSelector] which just chooses first available state.
 *
 * Not very effective, because it fragments the state space significantly.
 */
class PickRandom<S: Any, P: Any>(
        private val solver: Solver<P>,
        private val collections: CollectionContext<S, P>
) : PivotSelector<S, P> {

    override fun StateMap<S, P>.findPivots(): StateMap<S, P> = solver.run {
        val pivots = collections.makeEmptyMap()
        var uncovered = this@findPivots.entries.map { it.second }.fold<P?, P?>(null) { a, b -> a or b }
        val data = entries.toList()
        Collections.shuffle(data)
        for ((s, p) in data) {
            (p and uncovered)?.takeIfNotEmpty()?.let { pivot ->
                pivots.lazySet(s, pivot)
                uncovered = (pivot complement uncovered)?.takeIfNotEmpty()
            }
            if (uncovered == null) break
        }
        return pivots
    }

}