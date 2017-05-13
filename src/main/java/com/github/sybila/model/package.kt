package com.github.sybila.model

import com.github.sybila.solver.Solver

/**
 * The states of a model are currently fixed to integers.
 *
 * We might lift this restriction eventually, but right now it reduces implementation
 * complexity significantly, since they can be directly used as indices.
 */
typealias State = Int

fun <Param : Any> Solver<Param>.increasingStateMap(size: Int): IncreasingStateMap<Int, Param>
        = ArrayStateMap(size, this.ff, this)
fun <Param : Any> Solver<Param>.decreasingStateMap(size: Int): DecreasingStateMap<Int, Param>
        = ArrayStateMap(size, this.tt, this)