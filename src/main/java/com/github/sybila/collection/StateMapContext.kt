package com.github.sybila.collection

/**
 * Usually, one would like to choose an appropriate [StateMap] implementations depending on
 * the current circumstances (mutable/immutable, big/small, etc.). To this end, one can supply
 * a context which will be used to create new state maps.
 */
interface StateMapContext<S: Any, P: Any> {

    /** Empty immutable state map. */
    val emptyMap: StateMap<S, P>

    /** State map containing all states together with full parameter sets. */
    val fullMap: StateMap<S, P>

    /**
     * Create a mutable copy of this state map.
     *
     * For example when I want to modify some shared intermediate result.
     */
    fun StateMap<S, P>.toMutable(): MutableStateMap<S, P>

    /**
     * Create an immutable copy of this state map.
     *
     * For example when I want to pass some intermediate results, but keep working on the current copy.
     */
    fun MutableStateMap<S, P>.toImmutable(): StateMap<S, P>

    /**
     * Construct a state map from a (lazy) sequence.
     */
    fun Sequence<Pair<S, P>>.toStateMap(): StateMap<S, P>

}