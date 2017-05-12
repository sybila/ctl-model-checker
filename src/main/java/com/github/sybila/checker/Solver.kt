package com.github.sybila.checker

import com.github.sybila.checker.map.*
import java.nio.ByteBuffer
import java.util.*

/**
 * All color related actions are part of the solver.
 *
 * This provides a way to add decoupled caching and
 * other advanced options without complicating the
 * colors interface implementation.
 */
interface SolverOld<Params : Any> {

    val tt: Params
    val ff: Params

    /**
     * Standard logical operations.
     *
     * [and]: { x \in P | x \in this && x \in other }
     * [or]: { x \in P | x \in this || x \in other }
     * [not]: { x \in P | x \not\in this }
     *
     * Operations don't modify (at least visibly) the original set.
     *
     * @Complexity: constant
     */

    infix fun Params?.and(other: Params?): Params?
    infix fun Params?.or(other: Params?): Params?
    fun Params.not(): Params

    infix fun Params?.equal(other: Params?): Boolean

    /**
     * Return a constant-time over- and under-approximation of the
     * [isSat] value.
     *
     * Invariants:
     * [canSat] || [isNotSat]
     * [canNotSat] || [isSat]
     *
     * @Complexity: constant
     */
    fun Params.canSat(): Boolean = true
    fun Params.canNotSat(): Boolean = true

    /**
     * Full emptiness check.
     *
     * @Complexity: exponential
     */
    fun Params.isSat(): Boolean
    fun Params.isNotSat(): Boolean = !this.isSat()

    /**
     * Semantic comparison operators:
     *
     * A andNot B = exists x \in A: x \not\in B
     * A equals B = x \in A <-> \in B
     *
     * (sometimes can be implemented faster)
     *
     * @Complexity: exponential
     */
    infix fun Params.andNot(other: Params): Boolean = (this and other.not()).isSat()
    infix fun Params.equals(other: Params): Boolean = !((this or other) andNot (this and other))

    /**
     * Try to reduce the size of the params representation (in place).
     *
     * This is useful for concise printing and network transfers.
     *
     * @Complexity: exponential
     */
    fun Params.minimize()

    /**
     * Return required number of bytes to serialize this object.
     *
     * @Complexity: constant
     */
    fun Params.byteSize(): Int

    /**
     * Write a params object into a byte buffer.
     */
    fun ByteBuffer.putColors(colors: Params): ByteBuffer

    /**
     * Read a params object from buffer.
     */
    fun ByteBuffer.getColors(): Params

    /**
     * Create a copy of this color set with transferred ownership.
     * (Has to be synchronized on both solvers)
     */
    fun Params.transferTo(solver: Solver<Params>): Params

    /**
     * Return true if value in state map has changed.
     */
    fun MutableStateMap<Params>.setOrUnion(state: Int, value: Params): Boolean {
        val current = this[state]
        return if (value andNot current) {
            this[state] = value or current
            true
        } else false
    }

    /**
     * Create a human readable version of this params object.
     */
    fun Params.prettyPrint(): String

    fun StateMap<Params>.prettyPrint(): String {
        return this.entries().asSequence().map { it.first to it.second.prettyPrint() }.joinToString()
    }

    // Functions for testing

    fun StateMap<Params>.deepEquals(other: StateMap<Params>): Boolean {
        return OrStateMap(this, other, this@Solver).states().asSequence().all {
            this[it] equals other[it]
        }
    }

    fun StateMap<Params>.assertDeepEquals(other: StateMap<Params>) {
        if (!this.deepEquals(other)) {
            throw IllegalStateException("Expected ${this.prettyPrint()}, but got ${other.prettyPrint()}")
        }
    }

    fun StateMap<Params>.deepEquals(partitions: List<Pair<Solver<Params>, StateMap<Params>>>): Boolean {
        val keys = (this.states().asSequence() + partitions.flatMap { it.second.states().asSequence().asIterable() }).toSet()
        return keys.all { key ->
            val expected = this[key]
            val actual = partitions.asSequence().filter { it.second.contains(key) }.fold(ff) { a, i ->
                val (solver, states) = i
                a or solver.run { states[key].transferTo(this@Solver) }
            }
            expected equals actual
        }
    }

    fun StateMap<Params>.assertDeepEquals(partitions: List<Pair<Solver<Params>, StateMap<Params>>>) {
        if (!this.deepEquals(partitions)) {
            throw IllegalStateException("Expected ${this.prettyPrint()}, but got ${partitions.map { it.first.run { it.second.prettyPrint() } }}")
        }
    }

    // Utility functions

    fun emptyStateMap() = EmptyStateMap(ff)
    fun Int.asStateMap(value: Params): StateMap<Params> = SingletonStateMap(this, value, ff)
    fun IntRange.asStateMap(value: Params): StateMap<Params> = RangeStateMap(this, value, ff)
    fun BitSet.asStateMap(value: Params): StateMap<Params> = ConstantStateMap(this, value, ff)
    fun Map<Int, Params>.asStateMap(): StateMap<Params> = this.asMutableStateMap()
    fun Map<Int, Params>.asMutableStateMap(): MutableStateMap<Params> = HashStateMap(ff, this)

    infix fun StateMap<Params>.lazyAnd(other: StateMap<Params>): StateMap<Params>
            = AndStateMap(this, other, this@Solver)
    infix fun StateMap<Params>.lazyOr(other: StateMap<Params>): StateMap<Params>
            = OrStateMap(this, other, this@Solver)
    infix fun StateMap<Params>.complementAgainst(full: StateMap<Params>): StateMap<Params>
            = ComplementStateMap(full, this, this@Solver)

}