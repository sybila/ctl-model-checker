package com.github.sybila.model.ode

import com.github.sybila.ode.model.OdeModel
import com.github.sybila.solver.grid.Grid2
import com.github.sybila.solver.grid.Grid2Solver
import reactor.core.scheduler.Scheduler
import java.util.*

class Grid2TransitionSystem(
        model: OdeModel, solver: Grid2Solver, scheduler: Scheduler
) : OdeTransitionSystem<Grid2>(model, solver, scheduler) {

    override fun computeVertexColors(vertex: Int, dimension: Int): Grid2 {
        var derivationValue = 0.0
        var denominator = 0.0
        var parameterIndex = -1

        //evaluate equations
        for (summand in model.variables[dimension].equation) {
            var partialSum = summand.constant
            for (v in summand.variableIndices) {
                partialSum *= model.variables[v].thresholds[encoder.vertexCoordinate(vertex, v)]
            }
            if (partialSum != 0.0) {
                for (function in summand.evaluable) {
                    val index = function.varIndex
                    partialSum *= function(model.variables[index].thresholds[encoder.vertexCoordinate(vertex, index)])
                }
            }
            if (summand.hasParam()) {
                parameterIndex = summand.paramIndex
                denominator += partialSum
            } else {
                derivationValue += partialSum
            }
        }

        val bounds: Grid2 = if (parameterIndex == -1 || denominator == 0.0) {
            //there is no parameter in this equation
            if (derivationValue > 0) solver.tt else solver.ff
        } else {
            //if you divide by negative number, you have to flip the condition
            val positive = denominator > 0
            val range = model.parameters[parameterIndex].range
            //min <= split <= max
            val split = Math.min(range.second, Math.max(range.first, -derivationValue / denominator))
            val newLow = if (positive) split else range.first
            val newHigh = if (positive) range.second else split

            if (newLow >= newHigh) solver.ff else {
                val r = model.parameters.flatMap { listOf(it.range.first, it.range.second) }.toDoubleArray()
                r[2*parameterIndex] = newLow
                r[2*parameterIndex+1] = newHigh
                Grid2(
                        thresholdsX = doubleArrayOf(r[0], r[1]),
                        thresholdsY = doubleArrayOf(r[2], r[3]),
                        values = BitSet().apply { set(0) }
                )
            }
        }

        return bounds
    }

}