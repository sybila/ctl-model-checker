package com.github.sybila.funn

/**
 * In general assumed not to be thread safe.
 */
interface MutableStateMap<S, P> : StateMap<S, P> {

    // null signalizes remove
    operator fun set(key: S, value: P?)

}