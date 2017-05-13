package com.github.sybila.model

/**
 * Generalization of a collection holding a parametrized set of states.
 *
 * We don't use standard [Map] interface because:
 *  - we want to avoid [Map.Entry] types, since the <State, Param> pairs are
 *  frequently passed around without any association with a map.
 *  - we don't need access the [Map.values].
 */
interface StateMap<State : Any, Param : Any> {

    /**
     * Provides access to all keys managed by this state map.
     */
    val states: Iterable<State>

    /**
     * Provides access to all entries managed by this state map.
     *
     * Note that the iterator may choose to omit unsatisfiable parameter sets,
     * but is not required to do so.
     */
    val entries: Iterable<Pair<State, Param>>

    /**
     * Access elements of state map using the [key].
     *
     * If the [key] is not present in the map, return empty parameter set provided by
     * a corresponding solver.
     */
    operator fun get(key: State): Param

    /**
     * Check if given [key] is present in this map.
     *
     * Note that this does not guarantee that the key has a satisfiable parameter set associated with it.
     * Only that it was previously set.
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

    fun toIncreasing(): IncreasingStateMap<State, Param>
    fun toDecreasing(): DecreasingStateMap<State, Param>
}