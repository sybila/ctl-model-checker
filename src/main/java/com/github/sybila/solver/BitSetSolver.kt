package com.github.sybila.solver

import java.util.*

/**
 * Solver for enumerated parameter sets represented as a [BitSet].
 */
class BitSetSolver(
        override val universe: BitSet
) : Solver<BitSet> {

    override fun BitSet?.and(other: BitSet?): BitSet? = when {
        this == null || other == null -> null
        else -> this.copy().apply { and(other) }.takeIf { !it.isEmpty }
    }

    override fun BitSet?.or(other: BitSet?): BitSet? = when {
        this == null -> other
        other == null -> this
        else -> this.copy().apply { or(other) }
    }

    override fun BitSet?.not(): BitSet? = when {
        this == null -> universe
        this == universe -> null
        else -> universe.copy().apply { andNot(this@not) }.takeIf { !it.isEmpty }
    }

    override fun BitSet?.equal(other: BitSet?): Boolean = this == other

    private fun BitSet.copy() = this.clone() as BitSet

}