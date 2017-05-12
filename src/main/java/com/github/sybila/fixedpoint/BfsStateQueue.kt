package com.github.sybila.fixedpoint

import java.util.*

/**
 * A state queue which uses a BFS search strategy to process states
 * while ensuring only one "level" is processed at a time and that level is always
 * the closest to the start.
 */
internal class BfsStateQueue<State : Any>(
        private val distance: MutableMap<State, Int> = HashMap()
) {

    private val recompute = HashSet<State>()

    fun add(states: Iterable<State>, from: State?) {
        recompute.addAll(states)
        if (from == null) {
            states.forEach { distance[it] = 0 }
        } else {
            states.forEach { state ->
                distance[state] = Math.min(
                        distance[state] ?: Int.MAX_VALUE,
                        distance[from]!! + 1
                )
            }
        }
    }

    fun remove(): Iterable<State> {
        val minDistance = recompute.fold(Int.MAX_VALUE) { d, s ->
            Math.min(d, distance[s]!!)
        }
        val closest = recompute.filter { distance[it] == minDistance }
        recompute.removeAll { distance[it] == minDistance }
        return closest
    }

    fun isEmpty(): Boolean = recompute.isEmpty()
    fun isNotEmpty(): Boolean = recompute.isNotEmpty()

}