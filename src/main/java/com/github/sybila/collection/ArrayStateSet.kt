package com.github.sybila.collection

/**
 * This is (a bit memory inefficient) set implementation for Int states.
 *
 * It is preferred because it allows simple conversion to and from mutable set based
 * on atomic arrays [AtomicArrayStateSet].
 *
 * @param [data] A positive value at index `i` signifies presence in the set.
 */
class ArrayStateSet(private val data: IntArray) : StateSet<Int> {

    override fun contains(state: Int): Boolean
            = if (state < 0 || state >= data.size) false else data[state] > 0

    override fun iterator(): Iterator<Int>
            = data.indices.asSequence().filter { data[it] > 0 }.iterator()

    /**
     * Create a mutable copy of this set using the [AtomicArrayStateSet].
     */
    fun makeMutable(): AtomicArrayStateSet = AtomicArrayStateSet(data) // copy is performed by the atomic array

}