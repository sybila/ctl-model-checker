package com.github.sybila.algorithm.components

import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.Counter
import com.github.sybila.collection.MutableStateMap
import com.github.sybila.collection.StateMap
import com.github.sybila.model.TransitionSystem
import com.github.sybila.solver.Solver
import java.util.*

class PickStructure<S: Any, P: PickCardinality.Cardinality>(
        private val solver: Solver<P>,
        private val collections: CollectionContext<S, P>,
        private val model: TransitionSystem<S, P>
) : PivotSelector<S, P> {

    data class MagicResult<S: Any, P: Any>(
            val state: S,
            val params: P?,
            val magic: Int,
            val magicWeight: Double
    )

    // Ugly, wrong cast!
    private val magics = (collections.makeEmptyMap() as MutableStateMap<S, ArrayList<P?>>).apply { solver.run { model.run {
        states.forEach { state ->
            //println("Magic: $state")
            fun MutableList<P?>.setOrUnion(index: Int, value: P?) {
                while (this.size <= index) add(null)
                this[index] = this[index] or value
            }
            val successors = state.successors().fold(Counter<P>(solver)) { count, s ->
                if (s != state) {
                    transitionBound(state, s)?.let(count::increment)
                }
                count
            }
            val predecessors = state.predecessors().asSequence().fold(Counter<P>(solver)) { count, s ->
                if (s != state) {
                    transitionBound(s, state)?.let(count::increment)
                }
                count
            }
            val result = ArrayList<P?>()
            for (pI in (0 until predecessors.size)) {
                val p = predecessors[pI]
                for (sI in (0 until successors.size)) {
                    val s = successors[sI]
                    if (pI >= sI) {
                        (p and s)?.takeIfNotEmpty()?.let { k ->
                            result.setOrUnion(pI - sI, k)
                        }
                    }
                }
            }
            lazySet(state, result)
        }
    } } }

    private fun MagicResult<S, P>.fight(state: S, params: P?, uncovered: P?): MagicResult<S, P> {
        val result = this
        solver.run {
            model.run {
                val stateMagic = magics[state] ?: emptyList<P?>()
                for (m in (stateMagic.indices.reversed())) {
                    // if magic of this state is smaller, we don't even have to try...
                    if (m < result.magic) return result
                    val stateParams = params and uncovered
                    (stateParams and stateMagic[m])?.takeIfNotEmpty()?.let { magic ->
                        if (m > result.magic || magic.cardinality > result.magicWeight) {
                            return MagicResult(state, stateParams, m, magic.cardinality)
                        }
                    }
                }
                if (result.magic < 0) {
                    return MagicResult(state, params and uncovered, -1, 0.0)
                }
            }
        }
        return result
    }

    override fun StateMap<S, P>.findPivots(): StateMap<S, P> = solver.run {
        val pivots = collections.makeEmptyMap()
        var uncovered = this@findPivots.entries.map { it.second }.fold<P?, P?>(null) { a, b -> a or b }
        while (uncovered != null) {
            var result = entries.first().let { (s, p) -> MagicResult(s, p, -1, 0.0) }
            for ((state, params) in entries) {
                result = result.fight(state, params, uncovered)
            }
            pivots.lazySet(result.state, result.params)
            uncovered = (result.params complement uncovered)?.takeIfNotEmpty()
        }
        return pivots
    }

}