package com.github.sybila.checker.solver

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer
import java.util.*

class IntSetSolver(
        override val tt: Set<Int>
) : Solver<Set<Int>> {

    override fun Set<Int>.prettyPrint(): String = this.toString()

    override val ff: Set<Int> = setOf()

    override fun Set<Int>.and(other: Set<Int>): Set<Int> = this.intersect(other)

    override fun Set<Int>.or(other: Set<Int>): Set<Int> = this + other

    override fun Set<Int>.not(): Set<Int> = tt - this

    override fun Set<Int>.isSat(): Boolean = this.isNotEmpty()

    override fun Set<Int>.minimize() {}

    override fun Set<Int>.byteSize(): Int = 4 * this.size

    override fun ByteBuffer.putColors(colors: Set<Int>): ByteBuffer = this.apply {
        this.putInt(colors.size)
        colors.forEach { this.putInt(it) }
    }

    override fun ByteBuffer.getColors(): Set<Int> = (1..this.int).map { this.int }.toSet()

    override fun Set<Int>.transferTo(solver: Solver<Set<Int>>): Set<Int> = HashSet(this)

    override fun Set<Int>.canSat(): Boolean = this.isNotEmpty()
    override fun Set<Int>.canNotSat(): Boolean = this.isEmpty()

    override fun Set<Int>.andNot(other: Set<Int>): Boolean = this.any { it !in other }
    override fun Set<Int>.equals(other: Set<Int>): Boolean = this == other

}