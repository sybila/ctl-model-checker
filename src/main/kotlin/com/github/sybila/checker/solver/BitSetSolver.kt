package com.github.sybila.checker.solver

import com.github.daemontus.Option
import com.github.daemontus.asSome
import com.github.daemontus.none
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

    override fun Params?.extendWith(other: Params?): Option<Params> = when {
        this === TT || other === null -> Option.None()
        this === null -> other.asSome()
        else -> {
            val current = this.toBitSet()
            val new = other.toBitSet().clone() as BitSet
            new.or(current)
            new .assuming { new != current }
                ?.asParams()?.asSome() ?: none()
        }
    }.byTheWay { SolverStats.solverCall() }

    override fun Params.isSat(): Params?
            = this.byTheWay { SolverStats.solverCall() }
            .toBitSet().assuming { !it.isEmpty }?.asParams()

    fun Params.toBitSet(): BitSet = when (this) {
        is TT -> universe
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
        else -> throw UnsupportedParameterType(this)
    }

}