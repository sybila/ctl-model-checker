package com.github.sybila.collection

/**
 * Interface representing a parametrised state set using a state->parameter set mapping.
 *
 * It is expected to be immutable by contract in the same way [List] or [Set] is.
 * Due to this expected immutability, it is also assumed to be thread safe.
 *
 * Note that the StateMap can contain parameter sets which are empty, however
 * we discourage this behaviour and recommend removing these states from the map
 * completely.
 */
interface StateMap<S: Any, out P: Any> {

    /**
     * Sequence of all states stored in this set.
     */
    val states: Sequence<S>

    /**
     * Sequence of all state->parameter set pairs stored in this set.
     */
    val entries: Sequence<Pair<S, P>>

    /**
     * Retrieve parameter set for given state. Return null if the state is not present in the set.
     *
     * Note that the return value can be still an empty parameter set.
     */
    operator fun get(state: S): P?

}