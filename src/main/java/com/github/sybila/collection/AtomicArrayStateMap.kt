package com.github.sybila.collection

import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * A simple [MutableStateMap] implementation backed by an [AtomicReferenceArray].
 */
class AtomicArrayStateMap<P: Any>(private val data: AtomicReferenceArray<P?>) : MutableStateMap<Int, P> {

    /** Construct a state map full of nulls */
    constructor(capacity: Int) : this(AtomicReferenceArray(capacity))

    override val states: Sequence<Int>
        get() = (0 until data.length()).asSequence().filter { data[it] != null }

    override val entries: Sequence<Pair<Int, P>>
        get() = (0 until data.length()).asSequence()
                .map { i -> data[i]?.let { i to it } }
                .filterNotNull()


    override fun compareAndSet(state: Int, expected: P?, new: P?): Boolean {
        return data.compareAndSet(boundsCheck(state), expected, new)
    }

    override fun lazySet(state: Int, new: P?) {
        data.lazySet(boundsCheck(state), new)
    }

    override fun get(state: Int): P? = if (state < 0 || state >= data.length()) null else data[state]

    private fun boundsCheck(state: Int): Int {
        if (state < 0 || state >= data.length()) {
            IndexOutOfBoundsException("Trying to save state $state in state map with capacity [0, ${data.length()})")
        }
        return state
    }

}