package com.github.sybila.collection

import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * A simple immutable [StateMap] which is backed by the given [data] array.
 *
 * Only allows indexation using [Int] states.
 */
class ArrayStateMap<P : Any>(private val data: Array<P?>) : StateMap<Int, P> {

    override val states: Sequence<Int>
        get() = data.indices.asSequence().filter { data[it] != null }

    override val entries: Sequence<Pair<Int, P>>
        get() = data.asSequence()
                .mapIndexed { i, item -> if (item != null) i to item else null }
                .filterNotNull()

    override fun get(state: Int): P? = if (state < 0 || state >= data.size) null else data[state]

    /** Create a mutable copy of this state map using [AtomicArrayStateMap]. */
    fun toAtomic(): AtomicArrayStateMap<P> = AtomicArrayStateMap(AtomicReferenceArray(data))

}