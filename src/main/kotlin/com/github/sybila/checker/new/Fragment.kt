package com.github.sybila.checker.new

import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula

/**
 * Represents a partitioned piece of model
 */
interface Fragment<Colors> : PartitionFunction {

    fun step(from: Int, future: Boolean): Iterator<Transition<Colors>>

    fun eval(atom: Formula.Atom): StateMap<Colors>

}

data class Transition<out Colors>(
        val target: Int,
        val direction: DirectionFormula.Atom.Proposition,
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
 * Very slow, but very precise equality operation - use only for tests.
 */
fun <Colors> StateMap<Colors>.deepEquals(right: StateMap<Colors>, solver: Solver<Colors>): Boolean {
    val left = this
    return solver.run {
        (left + right).all {
            val l = left[it]
            val r = right[it]
            !(l andNot r) && !(r andNot l)
        }
    }
}

fun <Colors> Map<Int, Colors>.asStateMap(default: Colors): StateMap<Colors> = MapStateSet(default, this)

private data class MapStateSet<Colors>(val default: Colors, val map: Map<Int, Colors>) : StateMap<Colors> {
    override fun iterator(): Iterator<Int> = map.keys.iterator()
    override fun get(state: Int): Colors = map[state] ?: default
    override fun contains(state: Int): Boolean = state in map
}