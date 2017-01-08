package com.github.sybila.checker.distributed

/**
 * A collection of states with assigned parameters.
 *
 * Note that the contains operation is also just an approximation:
 * Even if the contains check returns true, the returned params can be empty.
 * However, you should try to avoid this when possible.
 *
 * @Contract All param values belong to the same solver.
 */
interface StateMap<out Params : Any> {

    /**
     * Increasing iterator over all states in the map.
     */
    fun states(): Iterator<Int>

    /**
     * Increasing (in states) iterator over all entries in the map.
     */
    fun entries(): Iterator<Pair<Int, Params>>

    /**
     * Return the stored value, or empty set if value is not present.
     */
    operator fun get(state: Int): Params

    /**
     * Return true if a non-empty value is present in this set.
     */
    operator fun contains(state: Int): Boolean

    /**
     * The size of the map doesn't have to be known at all times.
     *
     * This value should however provide an over-approximation of the map size.
     * (In the worst case, stateCount is a valid upper bound)
     */
    val sizeHint: Int

}

/**
 * A mutable variant of [StateMap].
 *
 * It is simpler than [MutableMap] because it doesn't support removing.
 */
interface MutableStateMap<Params : Any> : StateMap<Params> {

    /**
     * Store this value in the map.
     */
    operator fun set(state: Int, value: Params): Unit

}