package com.github.sybila.model

import com.github.sybila.solver.Solver

/**
 *
 */
interface TransitionSystem<Param : Any> : Solver<Param> {

    val stateCount: Int

    val states: Iterable<Int>
            get() = (0 until stateCount).asIterable()

    fun State.step(timeFlow: Boolean): Iterable<Pair<State, Param>>

}