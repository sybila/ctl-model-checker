package com.github.sybila.funn

/**
 * Assumed to be read only and hence thread safe.
 */
interface StateMap<S, P> {

    val states: Iterable<S>

    operator fun get(key: S): P

}

