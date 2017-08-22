package com.github.sybila.collection

/** A generic implementation of an empty [StateMap] */
class EmptyStateMap<S: Any, out P: Any> : StateMap<S, P> {

    override val states: Sequence<S>
        get() = emptySequence()
    override val entries: Sequence<Pair<S, P>>
        get() = emptySequence()

    override fun get(state: S): P? = null

}