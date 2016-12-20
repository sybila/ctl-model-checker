package com.github.sybila.checker.new

import com.github.sybila.huctl.Formula
import java.nio.ByteBuffer

class EnumerativeSolver(
        override val tt: Set<Int>
) : Solver<Set<Int>> {

    override val ff: Set<Int> = setOf()

    override fun Set<Int>.and(other: Set<Int>): Set<Int> {
        val copy = this.toHashSet()
        copy.retainAll(other)
        return copy
    }

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

    override fun ByteBuffer.getColors(): Set<Int> = (1 until this.int).map { this.int }.toSet()

}

class ExplicitKripkeFragment<Colors>(
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