package com.github.sybila.model

import com.github.sybila.solver.Solver

abstract class ArrayStateMap<Param : Any> internal constructor(
        size: Int, default: Param, protected val solver: Solver<Param>
) : StateMap<Int, Param> {

    protected val array: Array<Any> = Array(size) { default }

    override val states: Iterable<Int>
        get() = array.indices.filter { it in this }
    override val entries: Iterable<Pair<Int, Param>>
        get() = states.map { it to get(it) }

    override fun get(key: Int): Param {
        return if (key < 0 || key >= array.size) solver.ff
        else {
            @Suppress("UNCHECKED_CAST")
            array[key] as Param
        }
    }

    override fun contains(key: Int): Boolean
        = key >= 0 && key < array.size && solver.run { get(key).isSat() }

    override fun isEmpty(): Boolean
        = solver.run { array.indices.all { get(it).isNotSat() } }

}