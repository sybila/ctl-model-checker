package com.github.sybila.checker.map.mutable

import com.github.sybila.checker.MutableStateMap

/**
 * A map implementation with backing array of objects.
 *
 * Suitable for maps that require as little overhead as possible or are expected to have a high load factor on
 * a predictable interval.
 */
class ContinuousStateMap<Params : Any>(
        private val from: Int,
        private val to: Int,
        private val default: Params
) : MutableStateMap<Params> {

    private val data = arrayOfNulls<Any?>(to - from)

    private var size = 0

    override fun states(): Iterator<Int> = data.indices.asSequence().filter { data[it] != null }.iterator()

    override fun entries(): Iterator<Pair<Int, Params>>
            = data.indices.asSequence().filter {
                data[it] != null
            }.map {
                @Suppress("UNCHECKED_CAST") //only params objects are inserted and nulls are filtered out
                it.toState() to (data[it] as Params)
            }.iterator()

    override fun get(state: Int): Params {
        @Suppress("UNCHECKED_CAST") //only params objects are inserted
        val value = data[state.toKey()] as Params?
        return value ?: default
    }

    override fun contains(state: Int): Boolean = data[state.toKey()] != null

    override val sizeHint: Int = size

    override fun set(state: Int, value: Params) {
        val key = state.toKey()
        if (data[key] == null) size += 1
        data[state.toKey()] = value
    }

    private fun Int.toKey(): Int {
        val key = this - from
        if (key < 0 || key >= to) throw IndexOutOfBoundsException("Map holds values [$from, $to), but index $this was given.")
        return key
    }

    private fun Int.toState(): Int = this + from

}