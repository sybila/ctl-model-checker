package com.github.sybila.algorithm.components

import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.StateMap
import com.github.sybila.solver.Solver

/**
 * An implementation of [PivotSelector] based on a cardinality heuristic provided by the
 * parameter set.
 *
 * The assumption is that the higher the cardinality, the bigger portion of parameter space
 * is covered by that pivot.
 *
 */
class PickCardinality<S: Any, P: PickCardinality.Cardinality>(
        private val solver: Solver<P>,
        private val collections: CollectionContext<S, P>
) : PivotSelector<S, P> {

    override fun StateMap<S, P>.findPivots(): StateMap<S, P> = solver.run {
        val pivots = collections.makeEmptyMap()
        var uncovered = this@findPivots.entries.map { it.second }.fold<P?, P?>(null) { a, b -> a or b }
        println("To cover: ${uncovered?.cardinality}")
        val sorted = entries.map { it.first to it.second.cardinality }.sortedBy { it.second }
        for ((s, _) in sorted) {
            val p = this@findPivots[s]
            (p and uncovered)?.takeIfNotEmpty()?.let { pivot ->
                pivots.lazySet(s, pivot)
                uncovered = (pivot complement uncovered)?.takeIfNotEmpty()
            }
            if (uncovered == null) break
        }
        return pivots
    }

    interface Cardinality {
        val cardinality: Double
    }

}