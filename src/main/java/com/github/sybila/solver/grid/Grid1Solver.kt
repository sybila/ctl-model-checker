package com.github.sybila.solver.grid

import com.github.sybila.solver.Solver
import com.github.sybila.solver.copy
import java.util.*

class Grid1Solver(bound: Pair<Double, Double>) : Solver<Grid1> {

    override val tt: Grid1 = Grid1(doubleArrayOf(bound.first, bound.second), BitSet(1).apply { set(0) })
    override val ff: Grid1 = Grid1.EMPTY

    override fun Grid1.and(other: Grid1): Grid1 {
        val (l, r) = this.cut(other) to other.cut(this)
        return l.copy(values = l.values.copy().apply { and(r.values) }).simplify()
    }

    override fun Grid1.or(other: Grid1): Grid1 {
        val (l, r) = this.cut(other) to other.cut(this)
        return l.copy(values = l.values.copy().apply { or(r.values) }).simplify()
    }

    override fun Grid1.not(): Grid1 {
        val v = this.cut(tt)
        return v.copy(values = v.values.copy().apply { flip(0, v.thresholds.size - 1) }).simplify()
    }

    // This relies on the fact that simplification is deterministic and hence a grid with specific
    // values is uniquely defined.
    override fun Grid1.equal(other: Grid1): Boolean = this == other

    override fun Grid1.isSat(): Boolean = !this.values.isEmpty

}