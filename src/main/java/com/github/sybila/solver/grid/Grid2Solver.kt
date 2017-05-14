package com.github.sybila.solver.grid

import com.github.sybila.solver.Solver
import com.github.sybila.solver.copy
import java.util.*

class Grid2Solver(
        boundsX: Pair<Double, Double>,
        boundsY: Pair<Double, Double>
) : Solver<Grid2> {

    override val tt: Grid2 = Grid2(
            thresholdsX = doubleArrayOf(boundsX.first, boundsX.second),
            thresholdsY = doubleArrayOf(boundsY.first, boundsY.second),
            values = BitSet(1).apply { set(0) }
    )
    override val ff: Grid2 = Grid2.EMPTY

    override fun Grid2.and(other: Grid2): Grid2 {
        val (l, r) = this.cut(other) to other.cut(this)
        return l.copy(values = l.values.copy().apply { and(r.values) }).simplify()
    }

    override fun Grid2.or(other: Grid2): Grid2 {
        val (l, r) = this.cut(other) to other.cut(this)
        return l.copy(values = l.values.copy().apply { or(r.values) }).simplify()
    }

    override fun Grid2.not(): Grid2 {
        val v = this.cut(tt)
        return v.copy(values = v.values.copy().apply {
            flip(0, (v.thresholdsX.size - 1) * (v.thresholdsY.size - 1))
        }).simplify()
    }

    override fun Grid2.equal(other: Grid2): Boolean = this == other

    override fun Grid2.isSat(): Boolean = !this.values.isEmpty

}