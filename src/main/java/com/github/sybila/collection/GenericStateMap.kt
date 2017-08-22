package com.github.sybila.collection

/**
 * Simple [StateMap] implementation based on read-only map.
 *
 * Probably won't any benchmarks, but very useful for testing and small/sparse maps.
 */
class GenericStateMap<S: Any, out P: Any>(private val data: Map<S, P>) : StateMap<S, P> {

    constructor(vararg items: Pair<S, P>) : this(mapOf(*items))

    override val states: Sequence<S>
        get() = data.keys.asSequence()
    override val entries: Sequence<Pair<S, P>>
        get() = data.entries.asSequence().map { it.key to it.value }

    override fun get(state: S): P? = data[state]

}