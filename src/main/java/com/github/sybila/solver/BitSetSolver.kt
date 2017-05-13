package com.github.sybila.solver

import java.util.*

/**
 * Solver for enumerated parameter sets represented as a [BitSet].
 */
class BitSetSolver(
        override val tt: BitSet
) : Solver<BitSet> {

    override val ff: BitSet = BitSet()

    override fun BitSet.and(other: BitSet): BitSet = when {
        this.isEmpty || other.isEmpty -> ff
        else -> this.copy().apply { and(other) }
    }

    override fun BitSet.or(other: BitSet): BitSet = when {
        this.isEmpty -> other
        other.isEmpty -> this
        else -> this.copy().apply { or(other) }
    }

    override fun BitSet.not(): BitSet = when {
        this.isEmpty -> tt
        this == tt -> ff
        else -> tt.copy().apply { andNot(this@not) }
    }

    override fun BitSet.equal(other: BitSet): Boolean = this == other

    override fun BitSet.isSat(): Boolean = !this.isEmpty

    private fun BitSet.copy() = this.clone() as BitSet

}