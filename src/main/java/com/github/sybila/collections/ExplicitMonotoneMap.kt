package com.github.sybila.collections

import com.github.sybila.solver.Solver

class ExplicitMonotoneMap<State : Any, Param : Any>(
        private val delegate: MutableMap<State, Param>
) : MonotoneStateMap<State, Param> {

    override fun increaseKey(key: State, value: Param, solver: Solver<Param>): Boolean {
        val current = delegate[key]
        val union = solver.run { current or value }
        return if (union == null || solver.run { current equal union }) {
            false
        } else {
            delegate[key] = union
            true
        }
    }

    override val states: Iterable<State>
        get() = delegate.keys
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