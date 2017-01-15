package com.github.sybila.checker.solver

import com.github.sybila.checker.*
import java.util.*

data class BitSetParams(
        val bitSet: BitSet
) : Params

fun BitSet.asParams() = BitSetParams(this)

class BitSetSolver(
        val size: Int
) : Solver {

    val universe = BitSet().apply { set(0, size) }

    override fun Params.extendWith(other: Params): Params? {
        solverCalled()
        val current = this.toBitSet()
        val new = other.toBitSet().clone() as BitSet
        new.or(current)
        return if (new != current) {
            BitSetParams(new)
        } else null
    }

    override fun Params.isSat(): Params? {
        solverCalled()
        //println("Solve $this")
        val set = this.toBitSet()
        return if (set.isEmpty) null else BitSetParams(set)
    }

    fun Params.toBitSet(): BitSet = when (this) {
        is TT -> universe
        is FF -> BitSet()
        is BitSetParams -> this.bitSet
        is And -> {
            val result = universe.clone() as BitSet
            args.forEach { result.and(it.toBitSet()) }
            result
        }
        is Or -> {
            val result = BitSet()
            args.forEach { result.or(it.toBitSet()) }
            result
        }
        is Not -> {
            val result = universe.clone() as BitSet
            result.andNot(inner.toBitSet())
            result
        }
        else -> throw UnsupportedParameterValue(this)
    }

}