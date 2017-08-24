package com.github.sybila.model

import com.github.sybila.collection.StateMap
import com.github.sybila.collection.StateSet
import com.github.sybila.huctl.DirFormula
import com.github.sybila.huctl.Formula

/**
 * TransitionSystem provides basic graph-like data structure of the direction labelled transition system.
 *
 * Use [states] to retrieve the complete list of states, [successors] and [predecessors] to discover the
 * successors/predecessors of the given state and finally [transitionBound] and [transitionDirection]
 * to obtain labels of each transition.
 *
 * Transition system is assumed to be thread safe!
 */
interface TransitionSystem<S: Any, P : Any> {

    /**
     * Complete set of states.
     */
    val states: StateSet<S>

    /**
     * All states together with their admissible parameters.
     */
    val universe: StateMap<S, P>

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
     * The same as [transitionBound], but with an optional [time] direction.
     */
    fun transitionBound(start: S, end: S, time: Boolean = true): P?
            = if (time) transitionBound(start, end) else transitionBound(end, start)

    /**
     * The direction proposition associated with the transition from [start] to [end]. If such transition
     * does not exist, return null.
     */
    fun transitionDirection(start: S, end: S): DirFormula?

    /**
     * The same as [transitionDirection], but with an optional [time] direction.
     *
     * Note that while the time direction changes, the direction of transitions does not!
     * This is because, intuitively, we are going back in time, but the direction of the transition
     * remains the same (example: A path that is increasing when traveled using normal time is also increasing
     * when the time is reversed, we just travel in the opposite direction).
     */
    fun transitionDirection(start: S, end: S, time: Boolean = true): DirFormula?
            = if (time) transitionDirection(start, end) else transitionDirection(end, start)

    /**
     * Create a [StateMap] representing the validity of the given proposition.
     *
     * @throws [IllegalArgumentException] when given formula is not a supported proposition.
     */
    fun makeProposition(formula: Formula): StateMap<S, P>

}