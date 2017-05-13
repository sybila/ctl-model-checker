package com.github.sybila.model

import com.github.sybila.solver.Solver

class ArrayStateMap<Param : Any> internal constructor(
        size: Int, default: Param, protected val solver: Solver<Param>
) : StateMap<Int, Param>, IncreasingStateMap<Int, Param>, DecreasingStateMap<Int, Param> {

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

    override fun toIncreasing(): IncreasingStateMap<Int, Param> {
        val map = ArrayStateMap(array.size, solver.ff, solver)
        System.arraycopy(this.array, 0, map.array, 0, this.array.size)
        return map
    }

    override fun toDecreasing(): DecreasingStateMap<Int, Param> {
        val map = ArrayStateMap(array.size, solver.tt, solver)
        System.arraycopy(this.array, 0, map.array, 0, this.array.size)
        return map
    }

    override fun increaseKey(key: Int, value: Param): Boolean {
        if (key < 0 || key >= array.size) {
            throw IllegalArgumentException("Key $key out of bounds [0..${array.lastIndex}]")
        }

        return solver.run { get(key) tryOr value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

    override fun decreaseKey(key: Int, value: Param): Boolean {
        if (key < 0 || key >= array.size) {
            throw IllegalArgumentException("Key $key out of bounds [0..${array.lastIndex}]")
        }

        return solver.run { get(key) tryAnd value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

}