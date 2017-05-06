package com.github.sybila.checker.map

import com.github.sybila.checker.Partition
import com.github.sybila.checker.StateMap

/**
 * A wrapper around a state map that filters to entries of specified partition.
 */
class PartitionStateMap<out Params : Any>(
        private val innerMap: StateMap<Params>,
        private val partition: Partition<Params>
) : StateMap<Params> {

    override fun states(): Iterator<Int>
            = innerMap.states().asSequence().filter { it in partition }.iterator()

    override fun entries(): Iterator<Pair<Int, Params>>
            = innerMap.entries().asSequence().filter { it.first in partition }.iterator()

    override fun get(state: Int): Params
            = if (state in partition) innerMap[state] else partition.ff

    override fun contains(state: Int): Boolean = state in partition && state in innerMap

    override val sizeHint: Int = innerMap.sizeHint

}