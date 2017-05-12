package com.github.sybila.model

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

}