package com.github.sybila.collections

/**
 * A [StateMap] implementation which uses a standard [Map] to store
 * its values.
 *
 * Note that the map is not copied but used as is.
 */
class ExplicitMap<State : Any, out Param : Any>(
        private val delegate: Map<State, Param>
) : StateMap<State, Param> {

    override val states: Iterable<State> get() = delegate.keys
    override val entries: Iterable<Pair<State, Param>>
        get() = delegate.entries.map { it.key to it.value }

    override fun get(key: State): Param? = delegate[key]

    override fun contains(key: State): Boolean = key in delegate

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun toString(): String = delegate.entries.joinToString(
            prefix = "[", postfix = "]", separator = ",",
            transform = { "(${it.key} -> ${it.value})" }
    )

}