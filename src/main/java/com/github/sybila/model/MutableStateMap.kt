package com.github.sybila.model

import com.github.sybila.solver.Solver

/**
 * State map with a backing array. It is not strictly thread safe, but
 * it is safe under parallel read and sequential write.
 *
 * That is, you can safely write from different threads as long as your
 * writes don't access the same indices. You can also safely read from any index,
 * concurrently, but you are not guaranteed to observe the latest value if it was
 * written by another thread.
 */
internal class MutableStateMap<Param : Any>(size: Int) : StateMap<Param> {

    private val array = arrayOfNulls<Any?>(size)

    override val states: Iterable<Int>
        get() = array.indices.filter { array[it] != null }

    override val entries: Iterable<Pair<Int, Param>>
        get() = array.indices.filter { array[it] != null }.map { it to get(it)!! }

    override fun get(key: Int): Param? {
        return if (key < 0 || key >= array.size) null
        else {
            @Suppress("UNCHECKED_CAST")
            array[key] as Param?
        }
    }

    override fun contains(key: Int): Boolean
            = key >= 0 && key < array.size && array[key] != null

    override fun isEmpty(): Boolean
            = array.indices.all { array[it] == null }

    fun increaseKey(key: Int, value: Param, solver: Solver<Param>): Boolean {
        val current = get(key)
        return solver.run { current tryOr value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

}