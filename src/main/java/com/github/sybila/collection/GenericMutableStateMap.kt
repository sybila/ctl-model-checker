package com.github.sybila.collection

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
        // ConcurrentHashMap does not hold nulls, so we have to do a little magic
        expected == null && new == null -> !data.contains(state)    // if the value is not present, then the "update" worked
        expected == null && new != null -> data.computeIfAbsent(state, { new }) == new
        expected != null && new == null -> data.remove(state, expected)
        else -> data.computeIfPresent(state, { _, p ->
            if (p == expected) new else p
        }) == new
    }

    override fun get(state: S): P? = data[state]

    override fun lazySet(state: S, new: P?) {
        if (new == null) {
            data.remove(state)
        } else {
            data[state] = new
        }
    }
}