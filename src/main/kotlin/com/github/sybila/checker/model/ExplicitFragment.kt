package com.github.sybila.checker.model

import com.github.sybila.checker.*
import com.github.sybila.checker.map.emptyStateMap
import com.github.sybila.checker.solver.IntSetSolver
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula

class ExplicitPartition(
        override val stateCount: Int,
        private val successorMap: Map<Int, List<Transition>>,
        private val validity: Map<Formula.Atom, StateMap>,
        params: Set<Int>
) : TransitionSystem, Solver by IntSetSolver(params) {

    private val predecessorMap = successorMap.asSequence().flatMap {
        //direction is not flipped, because we are not going back in time
        it.value.asSequence().map { t -> t.target to Transition(it.key, t.direction, t.bound) }
    }.groupBy({ it.first }, { it.second })

    override fun Int.successors(timeFlow: Boolean): Sequence<Transition> {
        return ((if (timeFlow) successorMap[this] else predecessorMap[this]?.map {
            if (it.direction is DirectionFormula.Atom.Proposition) {
                it.copy(direction = it.direction.negate())
            } else it
        }) ?: listOf()).asSequence()
    }

    override fun Int.predecessors(timeFlow: Boolean): Sequence<Transition> {
        return ((if (timeFlow) predecessorMap[this] else successorMap[this]?.map {
            if (it.direction is DirectionFormula.Atom.Proposition) {
                it.copy(direction = it.direction.negate())
            } else it
        }) ?: listOf()).asSequence()
    }

    override fun Formula.Atom.Float.eval(): StateMap {
        return validity[this] ?: emptyStateMap()
    }

    override fun Formula.Atom.Transition.eval(): StateMap {
        return validity[this] ?: emptyStateMap()
    }

}