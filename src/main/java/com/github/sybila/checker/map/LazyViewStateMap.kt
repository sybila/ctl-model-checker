package com.github.sybila.checker.map

import com.github.sybila.checker.Solver
import com.github.sybila.checker.StateMap
import java.util.*

abstract class LazyViewStateMap<out Params: Any> internal constructor(
        internal val states: BitSet
) : StateMap<Params> {

    override fun states(): Iterator<Int>
            = states.stream().iterator()

    override fun entries(): Iterator<Pair<Int, Params>>
            = states.stream().mapToObj { it to get(it) }.iterator()

    override val sizeHint: Int = states.cardinality()

}

class AndStateMap<out Params: Any>(
        private val left: StateMap<Params>, private val right: StateMap<Params>,
        private val solver: Solver<Params>
) : LazyViewStateMap<Params>(run {
    left.asBitSet(clone = true).apply {
        this.and(right.asBitSet(clone = false))
    }
}) {

    override fun get(state: Int): Params = solver.run { left[state] and right[state] }

    override fun contains(state: Int): Boolean = state in left && state in right

}

class OrStateMap<out Params: Any>(
        private val left: StateMap<Params>, private val right: StateMap<Params>,
        private val solver: Solver<Params>
) : LazyViewStateMap<Params>(run {
    left.asBitSet(clone = true).apply {
        this.or(right.asBitSet(clone = false))
    }
}) {

    override fun get(state: Int): Params = solver.run { left[state] or right[state] }

    override fun contains(state: Int): Boolean = state in left || state in right

}

class ComplementStateMap<out Params: Any>(
        private val full: StateMap<Params>, private val inner: StateMap<Params>,
        private val solver: Solver<Params>
) : LazyViewStateMap<Params>(run { full.asBitSet(clone = true) }) {

    override fun get(state: Int): Params = solver.run { full[state] and inner[state].not() }

    override fun contains(state: Int): Boolean = state in full

}

private fun <Params: Any> StateMap<Params>.asBitSet(clone: Boolean): BitSet = when (this) {
    is ConstantStateMap<Params> -> this.states
    is LazyViewStateMap<Params> -> this.states
    else -> BitSet().let { set -> this.states().forEach { set.set(it) }; set }
}.run {
    if (clone) (this.clone() as BitSet) else this
}