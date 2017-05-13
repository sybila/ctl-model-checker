package com.github.sybila.model

import com.github.sybila.solver.Solver

/**
 * State map with a backing array. It is not strictly thread safe, but
 * it is safe under parallel read and sequential write, just as a standard array.
 *
 * That is, you can safely write from different threads as long as your
 * writes don't write to the same indices. You can also safely read from any index,
 * concurrently, but you are not guaranteed to observe the latest value if it was
 * written by another thread.
 *
 * Each map has an associated solver.
 */
class MutableStateMap<Param : Any> internal constructor(private val array: Array<Any?>,
                                                        private val solver: Solver<Param>,
                                                        private val increasing: Boolean = true
) : StateMap<Param> {

    constructor(size: Int, solver: Solver<Param>, increasing: Boolean = true)
            : this(if (increasing) arrayOfNulls(size) else Array<Any?>(size) { solver.universe }, solver)
/*
    fun merge(other: MutableStateMap<Param>): MutableStateMap<Param> {
        if (this.array.size != other.array.size) error("Sizes don't match")
        return MutableStateMap(Array(this.array.size) {

        }, solver)
    }
*/
    //private val array = arrayOfNulls<Any?>(size)

    override val states: Iterable<Int>
        get() = array.indices.filter { array[it] != null }

    override val entries: Iterable<Pair<Int, Param>>
        get() = array.indices.map { state -> get(state)?.let { state to it } }.filterNotNull()

    override fun get(key: Int): Param? {
        if (key < 0) throw IllegalArgumentException("Accessing negative key: $key")
        return if (key >= array.size) null
        else {
            @Suppress("UNCHECKED_CAST")
            array[key] as Param?
        }
    }

    override fun contains(key: Int): Boolean {
        if (key < 0) throw IllegalArgumentException("Checking negative key: $key")
        return key < array.size && array[key] != null
    }

    override fun isEmpty(): Boolean
            = array.indices.all { array[it] == null }

    /**
     * Union current state of [key] with the given [value] using the associated solver.
     *
     * If the state changed (increased), return true. Otherwise return false.
     */
    fun increaseKey(key: Int, value: Param): Boolean {
        if (key < 0) throw IllegalArgumentException("Increasing negative key: $key")
        if (key >= array.size) throw IllegalArgumentException("Increasing key $key, array size: ${array.size}")
        val current = get(key)
        return solver.run { current tryOr value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

    fun decreaseKey(key: Int, value: Param): Boolean {
        if (key < 0) throw IllegalArgumentException("Increasing negative key: $key")
        if (key >= array.size) throw IllegalArgumentException("Increasing key $key, array size: ${array.size}")
        val current = get(key)
        return solver.run {
            val new = (current and value)
            if (new equal current) false else {
                array[key] = new
                true
            }
        }
    }

}