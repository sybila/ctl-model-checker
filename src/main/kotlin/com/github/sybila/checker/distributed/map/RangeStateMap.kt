package com.github.sybila.checker.distributed.map

import com.github.sybila.checker.distributed.StateMap

class RangeStateMap<out Params : Any>(
        private val range: IntRange,
        private val value: Params,
        private val default: Params
) : StateMap<Params> {

    override fun states(): Iterator<Int> = range.iterator()

    override fun entries(): Iterator<Pair<Int, Params>> = range.asSequence().map { it to value }.iterator()

    override fun get(state: Int): Params = if (state in range) value else default

    override fun contains(state: Int): Boolean = state in range

    override val sizeHint: Int = range.count()

}