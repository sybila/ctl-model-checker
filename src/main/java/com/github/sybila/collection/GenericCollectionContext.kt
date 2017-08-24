package com.github.sybila.collection

/**
 * [CollectionContext] which uses [GenericStateMap], [GenericMutableStateMap], [GenericStateSet] and
 * [GenericMutableStateSet] as default implementations of the collections.
 */
class GenericCollectionContext<S: Any, P: Any>(universe: Map<S, P>) : CollectionContext<S, P> {

    override fun makeEmptyMap(): MutableStateMap<S, P> = GenericMutableStateMap(emptyMap())

    override fun makeEmptySet(): MutableStateSet<S> = GenericMutableStateSet(emptySet())

    override fun StateMap<S, P>.toMutable(): MutableStateMap<S, P>
            = (this as? GenericStateMap)?.copyMutable() ?: GenericMutableStateMap(this.entries.toMap())

    override fun StateSet<S>.toMutable(): MutableStateSet<S>
            = (this as? GenericStateSet)?.copyMutable() ?: GenericMutableStateSet(this.toSet())

    override fun MutableStateMap<S, P>.toReadOnly(): StateMap<S, P>
            = (this as? GenericMutableStateMap)?.copyReadOnly() ?: GenericStateMap(this.entries.toMap())

    override fun MutableStateSet<S>.toReadOnly(): StateSet<S>
            = (this as? GenericMutableStateSet)?.copyReadOnly() ?: GenericStateSet(this.toSet())

}