package com.github.sybila.checker.map

import com.github.sybila.checker.Params
import com.github.sybila.checker.StateMap
import java.util.*


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

class MapStateMap(
        private val map: Map<Int, Params>
) : StateMap {

    override val states: Sequence<Int> = map.keys.asSequence()
    override val entries: Sequence<Pair<Int, Params>> = map.entries.asSequence().map { it.key to it.value }

    override fun get(state: Int): Params? = map[state]

    override fun contains(state: Int): Boolean = state in map

    override fun toString(): String = map.toString()
}

fun Map<Int, Params>.asStateMap(): StateMap = MapStateMap(this)
