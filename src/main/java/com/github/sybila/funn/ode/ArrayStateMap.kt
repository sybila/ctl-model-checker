package com.github.sybila.funn.ode

import com.github.sybila.funn.AtomicStateMap
import com.github.sybila.funn.StateMap
import java.util.concurrent.atomic.AtomicReferenceArray

class ArrayStateMap<Param> internal constructor(
        private val zero: Param, array: Array<Any?>
) : AtomicStateMap<Int, Param> {

    private val array: AtomicReferenceArray<Any?> = AtomicReferenceArray(array)

    internal constructor(size: Int, zero: Param, default: Param = zero): this(zero, Array<Any?>(size) { default })

    internal constructor(size: Int, zero: Param, items: (Int) -> Param): this(zero, Array<Any?>(size, items))

    internal constructor(from: ArrayStateMap<Param>) : this(from.zero, Array<Any?>(from.array.length()) { from.array[it] })

    internal constructor(from: StateMap<Int, Param>, zero: Param) : this(zero, Array<Any?>(from.states.max()?.plus(1) ?: 0) { zero }) {
        from.states.forEach {
            this[it] = from[it]
        }
    }

    init {
        if (array.any { it == null }) error("Null in array!")
    }

    override val states: Iterable<Int>
        get() = (0 until array.length())

    /*
    override val entries: Iterable<Pair<Int, Param>>
        get() = states.map { it to get(it) }
    */

    override fun get(key: Int): Param {
        return if (key < 0 || key >= array.length()) zero
        else {
            @Suppress("UNCHECKED_CAST")
            (array[key] as Param)
        }
    }

    override fun set(key: Int, value: Param) {
        if (key < 0 || key >= array.length()) error("Cannot set state $key in map with capacity 0...${array.length()}")
        array[key] = value
    }

    override fun compareAndSet(key: Int, expect: Param, value: Param): Boolean {
        return array.compareAndSet(key, expect, value)
    }

    /*
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

    fun fillFrom(map: Map<Int, Param>) {
        for (s in array.indices) {
            array[s] = map[s] ?: solver.ff
        }
    }*/

}