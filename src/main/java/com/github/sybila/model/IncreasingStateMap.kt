package com.github.sybila.model

import com.github.sybila.solver.Solver

class IncreasingStateMap<Param : Any>(
        size: Int, solver: Solver<Param>
) : ArrayStateMap<Param>(Array(size) { solver.ff }, solver) {

    fun increaseKey(key: Int, value: Param): Boolean {
        if (key < 0 || key >= array.size) {
            throw IllegalArgumentException("Key $key out of bounds [0..${array.lastIndex}]")
        }

        return solver.run { get(key) tryOr value }?.let { union ->
            array[key] = union
            true
        } ?: false
    }

}