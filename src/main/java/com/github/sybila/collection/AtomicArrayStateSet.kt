package com.github.sybila.collection

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicIntegerArray

/**
 * An atomic implementation of [StateSet] using an [AtomicIntegerArray] as the backing
 * data structure.
 *
 * Any positive value in the [data] array is considered as state presence (Although the
 * set itself only uses 1 and 0).
 */
class AtomicArrayStateSet(data: IntArray) : MutableStateSet<Int> {

    private val data = AtomicIntegerArray(data)

    override fun contains(state: Int): Boolean
            = if (state < 0 || state >= data.length()) false else data.get(state) > 0

    override fun atomicAdd(state: Int): Boolean = data.getAndSet(state, 1) == 0

    override fun atomicRemove(state: Int): Boolean = data.getAndSet(state, 0) > 0

    override fun iterator(): Iterator<Int> = (0 until data.length()).asSequence().filter { data[it] > 0 }.iterator()

    override fun lazyAdd(state: Int) {
        data.lazySet(state, 1)
    }

    override fun lazyRemove(state: Int) {
        data.lazySet(state, 0)
    }

    /** Create a read-only copy of this atomic array set */
    fun makeReadOnly(): ArrayStateSet {
        return try {
            val backingArray = data.javaClass.getDeclaredField("array")
            backingArray.isAccessible = true
            ArrayStateSet(backingArray.get(data) as IntArray)
        } catch (e: Exception) {
            ArrayStateSet(IntArray(data.length()) { data[it] })
        }
    }

}