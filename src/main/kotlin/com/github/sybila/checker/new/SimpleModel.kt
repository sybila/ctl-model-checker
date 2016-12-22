package com.github.sybila.checker.new

import com.github.sybila.huctl.Formula
import java.nio.ByteBuffer
import java.util.*

class EnumeratedSolver(
        override val tt: Set<Int>
) : Solver<Set<Int>> {

    override fun Set<Int>.transferTo(solver: Solver<Set<Int>>): Set<Int> {
        return HashSet(this)    //just make a copy
    }

    override val ff: Set<Int> = setOf()

    override fun Set<Int>.and(other: Set<Int>): Set<Int> = this.intersect(other)

    override fun Set<Int>.or(other: Set<Int>): Set<Int> = this + other

    override fun Set<Int>.not(): Set<Int> = tt - this

    override fun Set<Int>.isEmpty(): Boolean = this.isEmpty()

    override fun Set<Int>.minimize(): Set<Int> = this

    override fun Set<Int>.byteSize(): Int = 4 + (4  * this.size)

    override fun ByteBuffer.putColors(colors: Set<Int>): ByteBuffer {
        this.putInt(colors.size)
        colors.forEach { this.putInt(it) }
        return this
    }

    override fun ByteBuffer.getColors(): Set<Int> = (1..this.int).map { this.int }.toSet()

}

val BOOL_SOLVER = object : Solver<Boolean> {
    override val tt: Boolean = true
    override val ff: Boolean = false

    override fun Boolean.and(other: Boolean): Boolean = this && other

    override fun Boolean.or(other: Boolean): Boolean = this || other

    override fun Boolean.not(): Boolean = !this

    override fun Boolean.isEmpty(): Boolean = !this

    override fun Boolean.minimize(): Boolean = this

    override fun Boolean.byteSize(): Int = 1

    override fun ByteBuffer.putColors(colors: Boolean): ByteBuffer {
        this.put((if (colors) 1 else 0).toByte())
        return this
    }

    override fun ByteBuffer.getColors(): Boolean {
        return if (this.get() == 1.toByte()) true else false
    }

    override fun Boolean.transferTo(solver: Solver<Boolean>): Boolean {
        return this
    }
}

class ExplicitFragment<Colors>(
        private val successorMap: Map<Int, List<Transition<Colors>>>,
        private val validity: Map<Formula.Atom, Map<Int, Colors>>,
        solver: Solver<Colors>
) : Fragment<Colors>, Solver<Colors> by solver {

    private val predecessorMap = successorMap.asSequence().flatMap {
        //direction is not flipped, because we are not going back in time
        it.value.asSequence().map { t -> t.target to Transition(it.key, t.direction, t.bound) }
    }.groupBy({ it.first }, { it.second })


    override val id: Int = 0
    override fun Int.owner(): Int = 0

    override fun step(from: Int, future: Boolean): Iterator<Transition<Colors>> {
        return ((if (future) successorMap[from] else predecessorMap[from]) ?: listOf()).iterator()
    }

    override fun eval(atom: Formula.Atom): StateMap<Colors> = (validity[atom] ?: mapOf()).asStateMap(ff)

}