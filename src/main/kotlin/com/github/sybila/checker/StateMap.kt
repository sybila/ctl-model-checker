package com.github.sybila.checker

/**
 * Read only mapping from states to parameter values.
 *
 * StateMap operations should be THREAD SAFE, however the iterators
 * adhere to the usual standards (since the map is read only,
 * there is no concurrent modification, but the iterator itself
 * is not thread safe).
 */
interface StateMap {

    val states: Sequence<Int>
    val entries: Sequence<Pair<Int, Params>>

    operator fun get(state: Int): Params?

    operator fun contains(state: Int): Boolean

}