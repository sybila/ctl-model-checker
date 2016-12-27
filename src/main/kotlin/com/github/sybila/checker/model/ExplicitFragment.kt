package com.github.sybila.checker.model

import com.github.sybila.checker.new.*
import com.github.sybila.huctl.False
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.True


class ExplicitFragment<Colors>(
        partitionFunction: PartitionFunction,
        override val stateCount: Int,
        private val states: Set<Int>,
        private val successorMap: Map<Int, List<Transition<Colors>>>,
        private val validity: Map<Formula.Atom, Map<Int, Colors>>,
        solver: Solver<Colors>
) : PartitionFunction by partitionFunction, Fragment<Colors>, Solver<Colors> by solver {

    private val predecessorMap = successorMap.asSequence().flatMap {
        //direction is not flipped, because we are not going back in time
        it.value.asSequence().map { t -> t.target to Transition(it.key, t.direction, t.bound) }
    }.groupBy({ it.first }, { it.second })

    override fun step(from: Int, future: Boolean): Iterator<Transition<Colors>> {
        return ((if (future) successorMap[from] else predecessorMap[from]) ?: listOf()).iterator()
    }

    override fun eval(atom: Formula.Atom): StateMap<Colors> {
        return when (atom) {
            False -> mapOf<Int, Colors>().asStateMap(ff)
            True -> states.map { it to tt }.toMap().asStateMap(ff)
            else -> (validity[atom] ?: mapOf()).asStateMap(ff)
        }
    }

}