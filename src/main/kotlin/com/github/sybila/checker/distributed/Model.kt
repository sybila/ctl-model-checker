package com.github.sybila.checker.distributed

import com.github.sybila.checker.Transition
import com.github.sybila.huctl.Formula

/**
 * A transition system tied to a channel.
 */
interface Model<Params : Any> : Solver<Params> {

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
    fun Formula.Atom.Float.eval(): StateMap<Params>
    fun Formula.Atom.Transition.eval(): StateMap<Params>

}