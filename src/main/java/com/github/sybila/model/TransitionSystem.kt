package com.github.sybila.model

import com.github.sybila.funn.StateMap
import com.github.sybila.huctl.DirFormula
import com.github.sybila.huctl.Formula

/**
 * TransitionSystem provides basic graph-like data structure of the direction labelled transition system.
 *
 * Use [states] to retrieve the complete list of states, [successors] and [predecessors] to discover the
 * successors/predecessors of the given state and finally [transitionBound] and [transitionDirection]
 * to obtain labels of each transition.
 *
 *
 */
interface TransitionSystem<S: Any, P : Any> {

    /**
     * Complete list of states.
     */
    val states: Iterable<S>

    /**
     * Collection of successor states with respect to the [time] direction.
     */
    fun S.successors(time: Boolean = true): Iterable<S>

    /**
     * Collection of predecessor states with respect to the [time] direction.
     */
    fun S.predecessors(time: Boolean = true): Iterable<S> = this.successors(!time)

    /**
     * The parameter set associated with the transition from [start] to [end]. If such transition
     * does not exist, return null.
     */
    fun transitionBound(start: S, end: S): P?

    /**
     * The direction proposition associated with the transition from [start] to [end]. If such transition
     * does not exist, return null.
     */
    fun transitionDirection(start: S, end: S): DirFormula?

    /**
     * Create a [StateMap] representing the validity of the given proposition.
     *
     * @throws [IllegalArgumentException] when given formula is not a supported proposition.
     */
    fun makeProposition(formula: Formula): StateMap<S, P>

}