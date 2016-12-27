package com.github.sybila.checker.new

import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula
import java.awt.Color
import javax.swing.plaf.nimbus.State

/**
 * Represents a partitioned piece of model
 */
interface Fragment<Colors> : PartitionFunction {

    fun step(from: Int, future: Boolean): Iterator<Transition<Colors>>

    fun eval(atom: Formula.Atom): StateMap<Colors>

}

data class Transition<out Colors>(
        val target: Int,
        val direction: DirectionFormula.Atom,
        val bound: Colors
)

interface PartitionFunction {
    val id: Int
    fun Int.owner(): Int
}

/**
 * WARNING: States that are iterable in the map can be an over-approximation of it's actual content.
 * Hence the get operation can return an empty color set even if the state was return by the iterator.
 */
interface StateMap<Colors> : Iterable<Int> {
    operator fun get(state: Int): Colors
    operator fun contains(state: Int): Boolean
}


/**
 * Both state maps must be managed by the same solver.
 * Very slow, but very precise equality operation - use only for tests.
 */
fun <Colors> deepEquals(left: StateMap<Colors>, right: StateMap<Colors>, solver: Solver<Colors>): Boolean {
    return solver.run {
        (left + right).toSet().all {
            val l = left[it]
            val r = right[it]
            !(l andNot r) && !(r andNot l)
        }
    }
}

/**
 * Each partition has it's own solver, and the last one is for the original set.
 */
fun <Colors> deepEquals(
        full: Pair<StateMap<Colors>, Solver<Colors>>,
        partitions: List<Pair<StateMap<Colors>, Solver<Colors>>>
): Boolean {
    val data = full.first
    val solver = full.second
    return solver.run {
        (data + partitions.flatMap { it.first }).toSet().all {
            val l = data[it]
            val r = partitions.fold(ff) { a, b -> a or b.second.run { b.first[it].transferTo(solver) }}
            !(l andNot r) && !(r andNot l)
        }
    }
}

fun <Colors> Map<Int, Colors>.asStateMap(default: Colors): StateMap<Colors> = MapStateSet(default, this)

private data class MapStateSet<Colors>(private val default: Colors, private val map: Map<Int, Colors>) : StateMap<Colors> {
    override fun iterator(): Iterator<Int> = map.keys.iterator()
    override fun get(state: Int): Colors = map[state] ?: default
    override fun contains(state: Int): Boolean = state in map
}

fun <Colors> Pair<Int, Colors>.asStateMap(default: Colors): StateMap<Colors> = SingletonStateSet(default, this.first, this.second)

fun <Colors> Colors.asConstantStateMap(range: IntRange): StateMap<Colors> = ConstantStateSet(this, range)

private data class SingletonStateSet<Colors>(
        private val default: Colors,
        private val state: Int,
        private val value: Colors
) : StateMap<Colors> {

    override fun iterator(): Iterator<Int> = object : Iterator<Int> {

        private var item: Int? = state

        override fun hasNext(): Boolean = item != null

        override fun next(): Int = item!!.apply { item = null }

    }

    override fun get(state: Int): Colors = if (state == this.state) value else default

    override fun contains(state: Int): Boolean = state == this.state

}

private data class ConstantStateSet<Colors>(
        private val value: Colors,
        private val stateRange: IntRange
) : StateMap<Colors> {
    override fun iterator(): Iterator<Int> = stateRange.iterator()

    override fun get(state: Int): Colors = value

    override fun contains(state: Int): Boolean = state in stateRange
}