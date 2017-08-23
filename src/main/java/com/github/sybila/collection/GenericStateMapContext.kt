package com.github.sybila.collection

/**
 * [StateMapContext] which uses [GenericStateMap] and [GenericMutableStateMap] as default implementations
 * of state maps.
 */
class GenericStateMapContext<S: Any, P: Any>(universe: Map<S, P>) : StateMapContext<S, P> {

    override val emptyMap: StateMap<S, P> = EmptyStateMap()
    override val fullMap: StateMap<S, P> = GenericStateMap(universe)

    override fun StateMap<S, P>.toMutable(): MutableStateMap<S, P> {
        return if (this is GenericStateMap) this.mutate()
        else {
            val source = this
            GenericMutableStateMap<S, P>(emptyMap()).apply {
                source.entries.forEach { (s, p) -> lazySet(s, p) }
            }
        }
    }

    override fun MutableStateMap<S, P>.toReadOnly(): StateMap<S, P> = GenericStateMap(this.entries.toMap())

}