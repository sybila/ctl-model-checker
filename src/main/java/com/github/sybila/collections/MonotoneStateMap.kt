package com.github.sybila.collections

import com.github.sybila.solver.Solver

/**
 * A mutable variant of the [StateMap] interface.
 *
 * It is designed to be monotone with respect to the added values, meaning
 * that you can only increase the amount of parameters stored in the map,
 * but never decrease it. This gives us a simple way to reason about
 * correctness and fixed points.
 */
interface MonotoneStateMap<State : Any, Param : Any> : StateMap<State, Param> {

    /**
     * Increase the value of a given [key] by [value] using the [solver]
     * to determine parameter set ordering (i.e. effectively union the value
     * with the current one.)
     *
     * @return true if value has increased, false if no change occurred (all
     * parameter valuations in [value] are already in the map)
     */
    fun increaseKey(key: State, value: Param, solver: Solver<Param>): Boolean

}