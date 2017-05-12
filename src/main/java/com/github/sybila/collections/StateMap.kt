package com.github.sybila.collections

/**
 * State map is a simplified map interface for representing (symbolic)
 * state -> parameter mappings.
 *
 * Note that standard parameter semantics apply, i.e. a null parameter represents
 * an empty parameter set and any non-null parameter can be expected to be non-empty.
 *
 * Furthermore, the state map should be effectively immutable.
 *
 * Finally, note that a state map does not provide a direct access to a [Solver]
 * and therefore should not expect any specific solver instance.
 *
 * Ideally, all operations should be implemented with constant time complexity.
 *
 * @param State Type of the state objects
 * @param Param Type of the parameter objects
 */
interface StateMap<State : Any, out Param : Any> {

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