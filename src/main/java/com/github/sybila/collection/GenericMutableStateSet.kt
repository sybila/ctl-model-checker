package com.github.sybila.collection

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A generic implementation of [MutableStateSet] based on [ConcurrentHashMap].
 *
 * Sadly, lazy operations aren't actually lazy, because [ConcurrentHashMap] does not support that (fair enough).
 */
class GenericMutableStateSet<S: Any>(data: Set<S>) : MutableStateSet<S> {

    private val data = ConcurrentHashMap<S, Unit>()

    override fun contains(state: S): Boolean = data.containsKey(state)

    override fun iterator(): Iterator<S> = data.keys().iterator()

    override fun atomicAdd(state: S): Boolean = data.putIfAbsent(state, Unit) == null

    override fun atomicRemove(state: S): Boolean = data.remove(state, Unit)

    override fun lazyAdd(state: S) {
        data.put(state, Unit)
    }

    override fun lazyRemove(state: S) {
        data.remove(state)
    }

    /** Make a read only copy of this set */
    fun copyReadOnly(): GenericStateSet<S> = GenericStateSet(data.keys().asSequence().toSet())

}