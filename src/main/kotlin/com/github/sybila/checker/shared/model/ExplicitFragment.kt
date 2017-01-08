package com.github.sybila.checker.shared.model

import com.github.sybila.checker.Transition
import com.github.sybila.checker.negate
import com.github.sybila.checker.shared.*
import com.github.sybila.checker.shared.solver.IntSetSolver
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula

class ExplicitPartition(
        override val stateCount: Int,
        private val successorMap: Map<Int, List<Transition<Params>>>,
        private val validity: Map<Formula.Atom, StateMap>,
        params: Set<Int>
) : TransitionSystem, Solver by IntSetSolver(params) {

    private val predecessorMap = successorMap.asSequence().flatMap {
        //direction is not flipped, because we are not going back in time
        it.value.asSequence().map { t -> t.target to Transition(it.key, t.direction, t.bound) }
    }.groupBy({ it.first }, { it.second })

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Params>> {
        return ((if (timeFlow) successorMap[this] else predecessorMap[this]?.map {
            if (it.direction is DirectionFormula.Atom.Proposition) {
                it.copy(direction = it.direction.negate())
            } else it
        }) ?: listOf()).iterator()
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Params>> {
        return ((if (timeFlow) predecessorMap[this] else successorMap[this]?.map {
            if (it.direction is DirectionFormula.Atom.Proposition) {
                it.copy(direction = it.direction.negate())
            } else it
        }) ?: listOf()).iterator()
    }

    override fun Formula.Atom.Float.eval(): StateMap {
        return validity[this] ?: emptyStateMap()
    }

    override fun Formula.Atom.Transition.eval(): StateMap {
        return validity[this] ?: emptyStateMap()
    }

}