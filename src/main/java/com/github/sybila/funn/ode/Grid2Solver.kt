package com.github.sybila.funn.ode

import com.github.sybila.funn.Solver
import com.github.sybila.solver.copy
import com.github.sybila.solver.grid.Grid2
import java.util.*

class Grid2Solver(
        boundsX: Pair<Double, Double>,
        boundsY: Pair<Double, Double>
) : Solver<Grid2> {

    override val ONE: Grid2 = Grid2(
            thresholdsX = doubleArrayOf(boundsX.first, boundsX.second),
            thresholdsY = doubleArrayOf(boundsY.first, boundsY.second),
            values = BitSet(1).apply { set(0) }
    )

    override val ZERO: Grid2 = Grid2.EMPTY

    override fun Grid2.plus(other: Grid2): Grid2 {
        return when {
            this === ZERO -> other
            other === ZERO -> this
            else -> {
                val (l, r) = this.cut(other) to other.cut(this)
                l.copy(values = l.values.copy().apply { or(r.values) }).simplify()
            }
        }
    }

    override fun Grid2.times(other: Grid2): Grid2 {
        return when {
            this === ZERO || other === ZERO -> ZERO
            else -> {
                val (l, r) = this.cut(other) to other.cut(this)
                l.copy(values = l.values.copy().apply { and(r.values) }).simplify()
            }
        }
    }

    override fun Grid2.minus(other: Grid2): Grid2 {
        val (l, r) = this.cut(other) to other.cut(this)
        return l.copy(values = l.values.copy().apply { andNot(r.values) }).simplify()
    }

    override fun Grid2.equal(other: Grid2): Boolean = this == other

}