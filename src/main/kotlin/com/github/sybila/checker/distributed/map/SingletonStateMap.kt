package com.github.sybila.checker.distributed.map

import com.github.sybila.checker.distributed.StateMap

//TODO: Add to string/hashcode/equals methods to all state maps

class SingletonStateMap<out Params : Any>(
        private val state: Int,
        private val value: Params,
        private val default: Params
) : StateMap<Params> {

    private val states = sequenceOf(state)
    private val entries = sequenceOf(state to value)

    override fun states(): Iterator<Int> = states.iterator()

    override fun entries(): Iterator<Pair<Int, Params>> = entries.iterator()

    override fun get(state: Int): Params = if (state == this.state) value else default

    override fun contains(state: Int): Boolean = state == this.state

    override val sizeHint: Int = 1

}