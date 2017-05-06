package com.github.sybila.checker.map

import com.github.sybila.checker.StateMap

class EmptyStateMap<out Params : Any>(
        private val default: Params
) : StateMap<Params> {

    override fun states(): Iterator<Int> = emptySequence<Int>().iterator()

    override fun entries(): Iterator<Pair<Int, Params>> = emptySequence<Pair<Int, Params>>().iterator()

    override fun get(state: Int): Params = default

    override fun contains(state: Int): Boolean = false

    override val sizeHint: Int = 0
}