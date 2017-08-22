package com.github.sybila.funn.ode

import com.github.sybila.funn.AtomicStateMap
import com.github.sybila.funn.Solver
import com.github.sybila.funn.StateMap
import com.github.sybila.funn.TransitionSystem
import com.github.sybila.huctl.*
import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.solver.grid.Grid2
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.*

class ODETransitionSystem(
        val model: OdeModel,
        val solver: Solver<Grid2> = Grid2Solver(model.parameters[0].range, model.parameters[1].range),
        scheduler: Scheduler = Schedulers.parallel(),
        private val createSelfLoops:Boolean = true
) : TransitionSystem<Int, Grid2> {

    companion object {
        // possible orientations
        private val PositiveIn = 0
        private val PositiveOut = 1
        private val NegativeIn = 2
        private val NegativeOut = 3
    }

    private val encoder = NodeEncoder(model)
    private val dimensions = model.variables.size

    val stateCount: Int = encoder.stateCount

    private val successors: Array<List<Pair<Int, Grid2>>> = Array(stateCount) { emptyList<Pair<Int, Grid2>>() }
    private val predecessors: Array<List<Pair<Int, Grid2>>> = Array(stateCount) { emptyList<Pair<Int, Grid2>>() }

    init {
        val start = System.currentTimeMillis()
        solver.run {
            val vertices = Array(encoder.vertexCount) {
                Array<Any>(dimensions) { solver.ZERO }
            }

            Flux.fromIterable(0 until encoder.vertexCount)
                    .parallel().runOn(scheduler)
                    .map { vertex ->
                        for (dim in 0 until dimensions) {
                            vertices[vertex][dim] = computeVertexColors(vertex, dim)
                        }
                    }.reduce({ _, _ -> Unit }).block()

            fun getVertexColor(vertex: Int, dim: Int): Grid2
                    = vertices[vertex][dim] as Grid2

            val facetColors = Array(stateCount) {
                Array(dimensions) { Array<Any>(4) { solver.ZERO } }
            }

            fun getFacetColor(state: Int, dim: Int, orientation: Int): Grid2
                    = facetColors[state][dim][orientation] as Grid2

            //enumerate all bit masks corresponding to vertices of a state
            val vertexMasks: IntArray = (0 until dimensions).fold(listOf(0)) { a, _ ->
                a.map { it.shl(1) }.flatMap { listOf(it, it.or(1)) }
            }.toIntArray()

            Flux.fromIterable(0 until stateCount)
                    .parallel().runOn(scheduler)
                    .map { state ->
                        for (dim in 0 until dimensions) {
                            for (orientation in 0..3) {
                                val positiveFacet = if (orientation == PositiveIn || orientation == PositiveOut) 1 else 0
                                val positiveDerivation = orientation == PositiveOut || orientation == NegativeIn
                                facetColors[state][dim][orientation] = vertexMasks
                                        .filter { it.shr(dim).and(1) == positiveFacet }
                                        .fold(solver.ZERO) { _, mask ->
                                            val vertex = encoder.nodeVertex(state, mask)
                                            val p = getVertexColor(vertex, dim)
                                            if (positiveDerivation) p else p.complement()
                                        }
                            }
                        }
                    }.reduce({ _, _ -> Unit }).block()


            for (time in listOf(true, false)) {
                val storage = if (time) successors else predecessors

                Flux.fromIterable(0 until stateCount)
                        .parallel().runOn(scheduler)
                        .map { state ->
                            val result = ArrayList<Pair<Int, Grid2>>()
                            var selfloop = solver.ONE

                            for (dim in model.variables.indices) {

                                val positiveOut = getFacetColor(state, dim, if (time) PositiveOut else PositiveIn)
                                val positiveIn = getFacetColor(state, dim, if (time) PositiveIn else PositiveOut)
                                val negativeOut = getFacetColor(state, dim, if (time) NegativeOut else NegativeIn)
                                val negativeIn = getFacetColor(state, dim, if (time) NegativeIn else NegativeOut)

                                encoder.higherNode(state, dim)?.let { higher ->
                                    val colors = if (time) positiveOut else positiveIn
                                    if (colors.isNotZero()) result.add(higher to colors)

                                    if (createSelfLoops) {
                                        val positiveFlow = negativeIn * positiveOut * (negativeOut + positiveIn).complement()
                                        selfloop *= positiveFlow.complement()
                                    }
                                }

                                encoder.lowerNode(state, dim)?.let { lower ->
                                    val colors = if (time) negativeOut else negativeIn
                                    if (colors.isNotZero()) result.add(lower to colors)

                                    if (createSelfLoops) {
                                        val negativeFlow = negativeOut * positiveIn * (negativeIn + positiveOut).complement()
                                        selfloop *= negativeFlow.complement()
                                    }
                                }

                            }

                            if (selfloop.isNotZero()) {
                                result.add(state to selfloop)
                            }

                            storage[state] = result
                        }.reduce({ _, _ -> Unit }).block()
            }
        }
        println("State space computed in ${System.currentTimeMillis() - start}")
    }

    private fun computeVertexColors(vertex: Int, dimension: Int): Grid2 {
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

        return if (parameterIndex == -1 || denominator == 0.0) {
            //there is no parameter in this equation
            if (derivationValue > 0) solver.ONE else solver.ZERO
        } else {
            //if you divide by negative number, you have to flip the condition
            val positive = denominator > 0
            val range = model.parameters[parameterIndex].range
            //min <= split <= max
            val split = Math.min(range.second, Math.max(range.first, -derivationValue / denominator))
            val newLow = if (positive) split else range.first
            val newHigh = if (positive) range.second else split

            if (newLow >= newHigh) solver.ZERO else {
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
    }

    override val emptyMap: StateMap<Int, Grid2> = ArrayStateMap(stateCount, zero = solver.ZERO)
    override val fullMap: StateMap<Int, Grid2> = ArrayStateMap(stateCount, zero = solver.ZERO, default = solver.ONE)

    override fun nextStep(from: Int, timeFlow: Boolean): Iterable<Pair<Int, Grid2>> {
        return if (timeFlow) successors[from] else predecessors[from]
    }

    override fun mutate(stateMap: StateMap<Int, Grid2>): AtomicStateMap<Int, Grid2> {
        return if (stateMap is ArrayStateMap<Grid2>) ArrayStateMap(stateMap)
        else ArrayStateMap(stateMap, solver.ZERO)
    }

    override fun makeProposition(proposition: Formula): StateMap<Int, Grid2> {
        if (proposition !is Formula.Numeric) error("Unknown proposition $proposition")
        val left = proposition.left
        val right = proposition.right
        val dimension: Int
        val threshold: Int
        val gt: Boolean
        when {
            left is Expression.Variable && right is Expression.Constant -> {
                dimension = model.variables.indexOfFirst { it.name == left.name }
                if (dimension < 0) throw IllegalArgumentException("Unknown variable ${left.name}")
                threshold = model.variables[dimension].thresholds.indexOfFirst { it == right.value }
                if (threshold < 0) throw IllegalArgumentException("Unknown threshold ${right.value}")

                gt = when (proposition.cmp) {
                    CompareOp.EQ, CompareOp.NEQ -> throw IllegalArgumentException("${proposition.cmp} comparison not supported.")
                    CompareOp.GT, CompareOp.GE -> true
                    CompareOp.LT, CompareOp.LE -> false
                }
            }
            left is Expression.Constant && right is Expression.Variable -> {
                dimension = model.variables.indexOfFirst { it.name == right.name }
                if (dimension < 0) throw IllegalArgumentException("Unknown variable ${right.name}")
                threshold = model.variables[dimension].thresholds.indexOfFirst { it == left.value }
                if (threshold < 0) throw IllegalArgumentException("Unknown threshold ${left.value}")

                gt = when (proposition.cmp) {
                    CompareOp.EQ, CompareOp.NEQ -> throw IllegalArgumentException("${proposition.cmp} comparison not supported.")
                    CompareOp.GT, CompareOp.GE -> false
                    CompareOp.LT, CompareOp.LE -> true
                }
            }
            else -> throw IllegalAccessException("Proposition is too complex: ${this}")
        }
        return ArrayStateMap(stateCount, solver.ZERO) { state ->
            val index = encoder.coordinate(state, dimension)
            if (gt == index > threshold) solver.ONE else solver.ZERO
        }
    }

    override fun <T> genericMap(zero: T): AtomicStateMap<Int, T> = ArrayStateMap(stateCount, zero)

    /*override fun Int.step(timeFlow: Boolean): Iterable<Pair<State, Grid2>> {
        return if (timeFlow) successors[this] else predecessors[this]
    }*/

    private fun facetIndex(from: Int, dimension: Int, orientation: Int)
            = from + (stateCount * dimension) + (stateCount * dimensions * orientation)
}