package com.github.sybila.checker.shared

import com.github.sybila.checker.Transition
import com.github.sybila.huctl.Formula


/**
 * A transition system tied to a channel.
 */
interface TransitionSystem : Solver {

    /**
     * Total number of states.
     */
    val stateCount: Int

    /**
     * Successor/Predecessors of given state.
     *
     * timeFlow == true -> Normal time flow.
     * timeFlow == false -> Reversed transition system.
     *
     * Note: predecessors/successors have inverted directions, hence we can't just
     * decide based timeFlow.
     *
     * @Contract state \in (0 until stateCount)
     */
    fun Int.successors(timeFlow: Boolean): Iterator<Transition<Params>>
    fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Params>>

    /**
     * Proposition evaluation.
     */
    fun Formula.Atom.Float.eval(): StateMap
    fun Formula.Atom.Transition.eval(): StateMap

}