package com.github.sybila.funn.ode

import com.github.sybila.solver.Solver
import com.github.sybila.solver.copy
import com.github.sybila.solver.grid.Grid2
import java.util.*

class Grid2Solver(
        boundsX: Pair<Double, Double>,
        boundsY: Pair<Double, Double>
) : Solver<Grid2> {

    override val TT: Grid2 = Grid2(
            thresholdsX = doubleArrayOf(boundsX.first, boundsX.second),
            thresholdsY = doubleArrayOf(boundsY.first, boundsY.second),
            values = BitSet(1).apply { set(0) }
    )

    override fun Grid2.takeIfNotEmpty(): Grid2? = this.takeIf { it != Grid2.EMPTY }

    override fun Grid2.strictAnd(with: Grid2): Grid2? {
        val (l, r) = this.cut(with) to with.cut(this)
        return l.copy(values = l.values.copy().apply { and(r.values) }).simplify().takeIfNotEmpty()
    }

    override fun Grid2.strictOr(with: Grid2): Grid2? {
        val (l, r) = this.cut(with) to with.cut(this)
        return l.copy(values = l.values.copy().apply { or(r.values) }).simplify().takeIfNotEmpty()
    }

    override fun Grid2.strictComplement(against: Grid2): Grid2? {
        val (l, r) = this.cut(against) to against.cut(this)
        return r.copy(values = r.values.copy().apply { andNot(l.values) }).simplify()
    }

    override fun Grid2.strictTryOr(with: Grid2): Grid2? {
        return this.strictOr(with)?.takeIf { it != this }
    }

}