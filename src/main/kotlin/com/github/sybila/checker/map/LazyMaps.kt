package com.github.sybila.checker.map

import com.github.sybila.checker.And
import com.github.sybila.checker.Or
import com.github.sybila.checker.Params
import com.github.sybila.checker.StateMap


class OrMap(
        private val left: StateMap,
        private val right: StateMap
) : StateMap {
    override val states: Sequence<Int> = (left.states + right.states).toSet().asSequence()
    override val entries: Sequence<Pair<Int, Params>> = states.map { it to get(it)!! }

    override fun get(state: Int): Params? {
        val l = left[state]
        val r = right[state]
        return if (l != null && r != null) {
            Or(listOf(l, r))
        } else l ?: r
    }

    override fun contains(state: Int): Boolean = state in left || state in right

}

infix fun StateMap.lazyOr(other: StateMap): StateMap = OrMap(this, other)

class AndMap(
        private val left: StateMap,
        private val right: StateMap
) : StateMap {

    override val states: Sequence<Int> = run {
        val s = left.states.toMutableSet()
        s.retainAll(right.states)
        s.asSequence()
    }

    override val entries: Sequence<Pair<Int, Params>> = states.map { it to get(it)!! }

    override fun get(state: Int): Params? {
        val l = left[state]
        val r = right[state]
        return if (l != null && r != null) {
            And(listOf(l, r))
        } else null
    }

    override fun contains(state: Int): Boolean = state in left && state in right

}

infix fun StateMap.lazyAnd(other: StateMap): StateMap = AndMap(this, other)

class FunMap(
        stateCount: Int,
        private val compute: (Int) -> Params?
) : StateMap {

    override val states: Sequence<Int> = (0 until stateCount).asSequence()
    override val entries: Sequence<Pair<Int, Params>> = states
            .map { state -> compute(state)?.run { state to this } }.filterNotNull()

    override fun get(state: Int): Params? = compute(state)

    override fun contains(state: Int): Boolean = compute(state) != null

}

fun Int.lazyMap(compute: (Int) -> Params?): StateMap = FunMap(this, compute)