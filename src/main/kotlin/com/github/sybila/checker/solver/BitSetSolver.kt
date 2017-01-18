package com.github.sybila.checker.solver

import com.github.sybila.checker.Solver
import com.github.sybila.checker.byTheWay
import java.nio.ByteBuffer
import java.util.*

class BitSetSolver(
        private val nBits: Int
) : Solver<BitSet> {

    override fun BitSet.prettyPrint(): String = this.toString()

    override val tt: BitSet = BitSet().apply { this.set(0, nBits) }
    override val ff: BitSet = BitSet()

    override fun BitSet.and(other: BitSet): BitSet
            = (this.clone() as BitSet).apply { this.and(other) }

    override fun BitSet.or(other: BitSet): BitSet
            = (this.clone() as BitSet).apply { this.or(other) }

    override fun BitSet.not(): BitSet
            = (tt.clone() as BitSet).apply { this.andNot(this@not) }

    override fun BitSet.isSat(): Boolean = !this.isEmpty.byTheWay { SolverStats.solverCall() }

    override fun BitSet.minimize() {}

    override fun BitSet.byteSize(): Int =  4 + this.toLongArray().size

    override fun ByteBuffer.putColors(colors: BitSet): ByteBuffer = this.apply {
        val array = colors.toLongArray()
        this.putInt(array.size)
        array.forEach { this.putLong(it) }
    }

    override fun ByteBuffer.getColors(): BitSet {
        val array = LongArray(this.int) { this.long }
        return BitSet.valueOf(array)
    }

    override fun BitSet.transferTo(solver: Solver<BitSet>): BitSet = this.clone() as BitSet

    override fun BitSet.canSat(): Boolean = !this.isEmpty
    override fun BitSet.canNotSat(): Boolean = this.isEmpty

    override fun BitSet.andNot(other: BitSet): Boolean = (this.clone() as BitSet).run {
        this.andNot(other)
        !this.isEmpty
    }.byTheWay { SolverStats.solverCall() }

    override fun BitSet.equals(other: BitSet): Boolean = (this == other).byTheWay { SolverStats.solverCall() }
}