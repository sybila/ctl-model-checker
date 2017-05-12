package com.github.sybila.collections

fun <State : Any, Param : Any> emptyStateMap() = object : StateMap<State, Param> {

    override val states: Iterable<State> = emptyList()
    override val entries: Iterable<Pair<State, Param>> = emptyList()
    override fun get(key: State): Param? = null
    override fun contains(key: State): Boolean = false
    override fun isEmpty(): Boolean = true

    override fun toString(): String = "[]"

}