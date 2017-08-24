package com.github.sybila.collection

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A simple implementation of [MutableStateMap] using a [ConcurrentHashMap].
 *
 * Probably too slow/memory hungry for general usage, but good for testing and simple problems.
 */
class GenericMutableStateMap<S: Any, P: Any>(data: Map<S, P>) : MutableStateMap<S, P> {

    private val data = ConcurrentHashMap(data)

    override val states: Sequence<S>
        get() = data.keys().asSequence()
    override val entries: Sequence<Pair<S, P>>
        get() = data.entries.asSequence().map { it.key to it.value }

    override fun compareAndSet(state: S, expected: P?, new: P?): Boolean = when {
        expected == null -> data.putIfAbsent(state, new) == null
        new == null -> data.remove(state, expected)
        else -> data.replace(state, expected, new)
    }

    override fun get(state: S): P? = data[state]

    override fun lazySet(state: S, new: P?) {
        if (new == null) {
            data.remove(state)
        } else {
            data[state] = new
        }
    }

    /** Create a read only copy of this state map */
    fun copyReadOnly(): GenericStateMap<S, P> = GenericStateMap(HashMap(data))

}