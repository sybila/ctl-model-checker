package com.github.sybila.solver

import com.github.sybila.solver.Truth.*

/**
 * Simple solver for binary parameter sets.
 *
 * It uses
 */
class UnitSolver : Solver<Truth> {

    override val tt: Truth = True
    override val ff: Truth = False

    override fun Truth.and(other: Truth): Truth = when {
        this === False -> this
        other === False -> other
        else -> this
    }

    override fun Truth.or(other: Truth): Truth = when {
        this === True -> this
        other === True -> other
        else -> this
    }

    override fun Truth.not(): Truth = if (this === True) False else True

    override fun Truth.equal(other: Truth): Boolean = this === other

    override fun Truth.isSat(): Boolean = this === True

}