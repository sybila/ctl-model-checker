package com.github.sybila.checker.solver

import com.github.daemontus.Option
import com.github.daemontus.asSome
import com.github.daemontus.none
import com.github.sybila.checker.*

data class IntSetParams(
     val set: Set<Int>
) : Params

fun Set<Int>.asParams(): Params = IntSetParams(this)

class IntSetSolver(
        val universe: Set<Int>
) : Solver {


    override fun Params?.extendWith(other: Params?): Option<Params> = when {
        this === TT || other === null -> Option.None()
        this === null -> other.asSome()
        else -> {
            val current = this.toSet()
            val new = other.toSet() + current
            new .assuming { new != current }
                ?.asParams()?.asSome() ?: none()
        }
    }.byTheWay { SolverStats.solverCall() }

    override fun Params.isSat(): Params?
            = this.byTheWay { SolverStats.solverCall() }
            .toSet().assuming { it.isNotEmpty() }?.asParams()


    private fun Params.toSet(): Set<Int> = when (this) {
        is TT -> universe
        is IntSetParams -> set
        is And -> args.fold(universe) { a, i -> a.intersect(i.toSet()) }
        is Or -> args.fold(emptySet()) { a, i -> a + i.toSet() }
        is Not -> universe - inner.toSet()
        else -> throw UnsupportedParameterType(this)
    }

}