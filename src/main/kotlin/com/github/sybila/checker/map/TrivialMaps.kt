package com.github.sybila.checker.map

import com.github.sybila.checker.Params
import com.github.sybila.checker.StateMap

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

fun Params?.asStateMap(stateCount: Int) = this?.let { FullStateMap(stateCount, it) } ?: emptyStateMap()

class SingletonStateMap(
        private val state: Int,
        private val value: Params
) : StateMap {

    override val states: Sequence<Int> = sequenceOf(state)
    override val entries: Sequence<Pair<Int, Params>> = sequenceOf(state to value)
    override fun get(state: Int): Params? = if (state == this.state) value else null
    override fun contains(state: Int): Boolean = state == this.state

}

fun Int.asStateMap(value: Params?) = value?.let {  SingletonStateMap(this, it) } ?: emptyStateMap()

