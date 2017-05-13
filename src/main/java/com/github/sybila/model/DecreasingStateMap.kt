package com.github.sybila.model

import com.github.sybila.solver.Solver

class DecreasingStateMap<Param : Any>(
        size: Int, solver: Solver<Param>
) : ArrayStateMap<Param>(Array(size) { solver.tt }, solver) {

    fun decreaseKey(key: Int, value: Param): Boolean {
        if (key < 0 || key >= array.size) {
            throw IllegalArgumentException("Key $key out of bounds [0..${array.lastIndex}]")
        }

        return solver.run { get(key) tryAnd value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

}