package com.github.sybila.checker.shared

import java.util.*

/**
 * Read only mapping from states to parameter values.
 *
 * StateMap operations should be THREAD SAFE, however the iterators
 * adhere to the usual standards (since the map is read only,
 * there is no concurrent modification, but the iterator itself
 * is not thread safe).
 */
interface StateMap {

    val states: Sequence<Int>
    val entries: Sequence<Pair<Int, Params>>

    operator fun get(state: Int): Params?

    operator fun contains(state: Int): Boolean

}

object EmptyStateMap : StateMap {

    override val states: Sequence<Int> = emptySequence()
    override val entries: Sequence<Pair<Int, Params>> = emptySequence()
    override fun get(state: Int): Params? = null
    override fun contains(state: Int): Boolean = false

}

fun emptyStateMap(): StateMap = EmptyStateMap

class FullStateMap(
        private val stateCount: Int,
        private val value: Params
) : StateMap {

    override val states: Sequence<Int> = (0 until stateCount).asSequence()
    override val entries: Sequence<Pair<Int, Params>> = (0 until stateCount).asSequence().map { it to value }
    override fun get(state: Int): Params? = if (state in this) value else null
    override fun contains(state: Int): Boolean = state >= 0 && state < stateCount

}

fun Params.asStateMap(stateCount: Int) = FullStateMap(stateCount, this)

class SingletonStateMap(
        private val state: Int,
        private val value: Params
) : StateMap {

    override val states: Sequence<Int> = sequenceOf(state)
    override val entries: Sequence<Pair<Int, Params>> = sequenceOf(state to value)
    override fun get(state: Int): Params? = if (state == this.state) value else null
    override fun contains(state: Int): Boolean = state == this.state

}

fun Int.asStateMap(value: Params) = SingletonStateMap(this, value)

class ArrayStateMap(
        private val array: Array<Params?>
) : StateMap {

    override val states: Sequence<Int>
            = array.indices.asSequence().filter { array[it] != null }
    override val entries: Sequence<Pair<Int, Params>>
            = array.indices.asSequence().filter { array[it] != null }.map { it to array[it]!! }

    override fun get(state: Int): Params? = if (checkBounds(state)) array[state] else null

    override fun contains(state: Int): Boolean = checkBounds(state) && array[state] != null

    private fun checkBounds(state: Int): Boolean = state >= 0 && state < array.size

    override fun toString(): String = Arrays.toString(array)
}

fun Array<Params?>.asStateMap() = ArrayStateMap(this)

class StatMapView(
        private val map: Map<Int, Params>
) : StateMap {

    override val states: Sequence<Int> = map.keys.asSequence()
    override val entries: Sequence<Pair<Int, Params>> = map.entries.asSequence().map { it.key to it.value }

    override fun get(state: Int): Params? = map[state]

    override fun contains(state: Int): Boolean = state in map

    override fun toString(): String = map.toString()
}

fun Map<Int, Params>.asStateMap(): StateMap = StatMapView(this)

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