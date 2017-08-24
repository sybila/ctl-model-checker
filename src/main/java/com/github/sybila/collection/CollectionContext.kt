package com.github.sybila.collection

/**
 * Usually, one would like to choose a domain-specific [StateMap] and [StateSet] implementations depending on
 * the type of system. To this end, one can supply a context which will be used to create new collections.
 *
 * Both state and parameter set types are common for the whole context, because otherwise it would be impossible
 * to derive the desired types at runtime.
 */
interface CollectionContext<S: Any, P: Any> {

    /**
     * Make a new mutable empty state map. (For read-only map, use [EmptyStateMap])
     */
    fun makeEmptyMap(): MutableStateMap<S, P>

    /**
     * Make a new mutable empty state set.
     */
    fun makeEmptySet(): MutableStateSet<S>

    /**
     * Create a mutable copy of this state map.
     */
    fun StateMap<S, P>.toMutable(): MutableStateMap<S, P>

    /**
     * Create a mutable copy of this state set.
     */
    fun StateSet<S>.toMutable(): MutableStateSet<S>

    /**
     * Create a read-only copy of this state map.
     */
    fun MutableStateMap<S, P>.toReadOnly(): StateMap<S, P>

    /**
     * Create a read-only copy of this state set.
     */
    fun MutableStateSet<S>.toReadOnly(): StateSet<S>

}