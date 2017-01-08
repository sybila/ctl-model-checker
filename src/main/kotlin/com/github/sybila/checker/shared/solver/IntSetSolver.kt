package com.github.sybila.checker.shared.solver

import com.github.sybila.checker.shared.*

data class IntSet(
     val set: Set<Int>
) : Params

fun intSetOf(vararg values: Int) = IntSet(values.toSet())

fun Set<Int>.toParams(): Params = IntSet(this)

class IntSetSolver(
        val universe: Set<Int>
) : Solver {

    override fun Params.extendWith(other: Params): Params? {
        solverCalled()
        val current = this.toSet()
        val new = other.toSet()
        val union = current + new
        return if (union != current) IntSet(union) else null
    }

    override fun Params.isSat(): Params? {
        solverCalled()
        //println("Check sat $this")
        val set = this.toSet()
        //println("Result: $set")
        return if (set.isNotEmpty()) IntSet(set) else null
    }

    private fun Params.toSet(): Set<Int> = when (this) {
        is TT -> universe
        is FF -> emptySet()
        is IntSet -> set
        is And -> args.fold(universe) { a, i -> a.intersect(i.toSet()) }
        is Or -> args.fold(emptySet()) { a, i -> a + i.toSet() }
        is Not -> universe - inner.toSet()
        else -> throw UnsupportedParameterValue(this)
    }

}