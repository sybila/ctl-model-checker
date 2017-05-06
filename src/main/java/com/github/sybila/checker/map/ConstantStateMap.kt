package com.github.sybila.checker.map

import com.github.sybila.checker.StateMap
import java.util.*


class ConstantStateMap<out Params : Any>(
        internal val states: BitSet,
        private val value: Params,
        private val default: Params
) : StateMap<Params> {

    override fun states(): Iterator<Int> = states.stream().iterator()

    override fun entries(): Iterator<Pair<Int, Params>> = states.stream().mapToObj { it to value }.iterator()

    override fun get(state: Int): Params = if (states.get(state)) value else default

    override fun contains(state: Int): Boolean = states.get(state)

    override val sizeHint: Int = states.cardinality()

}