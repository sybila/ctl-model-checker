package com.github.sybila.model

/**
 * TransitionSystem provides basic graph-like data structure using the [step] method.
 *
 * The states of a transition system are integers and each transition system
 * has to declare how many states (not all have to be reachable) are present, i.e. the [stateCount].
 *
 * TransitionSystem is always thread safe.
 */
interface TransitionSystem<out Param : Any> {

    /**
     * Number of states in this system. `s` is a valid state if `0 <= s < stateCount`
     */
    val stateCount: Int

    /**
     * The immediate successors (`[timeFlow] = true`) or predecessors (`[timeFlow] = false`)
     * of this state together with the parametric transition bound.
     *
     * Note that only non empty transitions are actually returned, since Param is non null.
     */
    fun State.step(timeFlow: Boolean): Iterable<Pair<State, Param>>

}