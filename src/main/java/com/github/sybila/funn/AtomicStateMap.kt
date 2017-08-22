package com.github.sybila.funn

/**
 * In general assumed not to be thread safe.
 */
interface AtomicStateMap<S, P> : StateMap<S, P> {

    // null signalizes remove
    operator fun set(key: S, value: P)

    fun compareAndSet(key: S, expect: P, value: P): Boolean

}