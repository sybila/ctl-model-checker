package com.github.sybila.collections

import com.github.sybila.solver.Solver

/**
 * A fast implementation that uses a backing array in models which
 * can use integers as keys.
 */
class ArrayMap<Param : Any>(size: Int) : MonotoneStateMap<Int, Param> {

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

    override fun increaseKey(key: Int, value: Param, solver: Solver<Param>): Boolean {
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