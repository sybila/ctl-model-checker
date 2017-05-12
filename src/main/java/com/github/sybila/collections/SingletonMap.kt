package com.github.sybila.collections

import com.github.sybila.checker.assuming

/**
 * A [StateMap] which stores exactly one ([State] -> [Param]) pair.
 */
class SingletonMap<State : Any, out Param : Any>(
        private val state: State, private val value: Param
) : StateMap<State, Param> {

    private val entry = state to value

    override val states: Iterable<State> get() = listOf(state)

    override val entries: Iterable<Pair<State, Param>> get() = listOf(entry)

    override fun get(key: State): Param? = value.assuming { key == state }

    override fun contains(key: State): Boolean = key == state

    override fun isEmpty(): Boolean = false

    override fun toString(): String = "[($state -> $value)]"

}