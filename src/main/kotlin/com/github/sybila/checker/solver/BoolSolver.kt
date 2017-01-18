package com.github.sybila.checker.solver

import com.github.sybila.checker.Solver
import com.github.sybila.checker.byTheWay
import java.nio.ByteBuffer

class BoolSolver : Solver<Boolean> {

    override fun Boolean.prettyPrint(): String = this.toString()

    override val tt: Boolean = true
    override val ff: Boolean = false

    override fun Boolean.and(other: Boolean): Boolean = this && other

    override fun Boolean.or(other: Boolean): Boolean = this || other

    override fun Boolean.not(): Boolean = !this

    override fun Boolean.isSat(): Boolean = this.byTheWay { SolverStats.solverCall() }

    override fun Boolean.minimize() {}

    override fun Boolean.byteSize(): Int = 1

    override fun ByteBuffer.putColors(colors: Boolean): ByteBuffer = this.apply {
        this.put((if (colors) 1 else 0).toByte())
    }

    override fun ByteBuffer.getColors(): Boolean = this.get() == 1.toByte()

    override fun Boolean.transferTo(solver: Solver<Boolean>): Boolean = this

    override fun Boolean.canSat(): Boolean = this
    override fun Boolean.canNotSat(): Boolean = !this

    override fun Boolean.andNot(other: Boolean): Boolean = (this && !other).byTheWay { SolverStats.solverCall() }
    override fun Boolean.equals(other: Boolean): Boolean = (this == other).byTheWay { SolverStats.solverCall() }
}