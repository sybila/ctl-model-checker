package com.github.sybila.model

/**
 * Generalization of a collection holding a parametrized set of states.
 *
 * We don't use standard [Map] interface because:
 *  - we want to avoid [Map.Entry] types, since the <State, Param> pairs are
 *  frequently passed around without any associated map.
 *  - we don't need access to [Map.values].
 *  - we have a special meaning for null values, which currently aligns with [Map],
 *  but that can change in the future.
 *
 *  Note that negative integers are not allowed as indices!
 */
interface StateMap<out Param : Any> {

    /**
     * Provides access to all non-empty keys managed by this state map.
     */
    val states: Iterable<State>

    /**
     * Provides access to all non-empty entries managed by this state map.
     */
    val entries: Iterable<Pair<State, Param>>

    /**
     * Access elements of state map using the [key].
     */
    operator fun get(key: State): Param?

    /**
     * Check if given [key] is present in this map.
     */
    operator fun contains(key: State): Boolean

    /**
     * Check the map for emptiness.
     */
    fun isEmpty(): Boolean

    /**
     * @see [isEmpty]
     */
    fun isNotEmpty(): Boolean = !isEmpty()

}