package com.github.sybila.model

import com.github.sybila.solver.Solver

/**
 * State map with a backing array. It is not strictly thread safe, but
 * it is publish safe under parallel read and sequential write.
 *
 * That is, you can read from any cell, but as long as you consistently write
 * to a cell from a single thread and you have some other synchronisation point
 * somewhere else, you are good to go.
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
        val union = solver.run { current or value }
        return if (union == null || solver.run { current equal union }) {
            false
        } else {
            array[key] = union
            true
        }
    }

}