package com.github.sybila.collection

class SingletonStateMap<S: Any, out P: Any>(
        private val state: S,
        private val value: P
) : StateMap<S, P> {

    override val states: Sequence<S>
        get() = sequenceOf(state)
    override val entries: Sequence<Pair<S, P>>
        get() = sequenceOf(state to value)

    override fun get(state: S): P? = value.takeIf { state == this.state }

}