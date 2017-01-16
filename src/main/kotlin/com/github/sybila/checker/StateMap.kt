package com.github.sybila.checker

/**
 * Read only mapping from states to parameter values.
 *
 * StateMap operations should be THREAD SAFE, however the iterators
 * adhere to the usual standards (since the map is read only,
 * there is no concurrent modification, but the iterator itself
 * is not thread safe).
 *
 * General convention is that
 */
interface StateMap {

    val states: Sequence<Int>
    val entries: Sequence<Pair<Int, Params>>

    /**
     * Return stored parameter value (null represents false)
     */
    operator fun get(state: Int): Params?

    /**
     * Return true if get(state) would return non null value.
     */
    operator fun contains(state: Int): Boolean

}