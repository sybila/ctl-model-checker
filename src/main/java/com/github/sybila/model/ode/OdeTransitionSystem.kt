package com.github.sybila.model.ode

import com.github.sybila.model.State
import com.github.sybila.model.TransitionSystem
import com.github.sybila.ode.generator.NodeEncoder
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.solver.Solver
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.*

abstract class OdeTransitionSystem<Param : Any>(
    val model: OdeModel, val solver: Solver<Param>
) : TransitionSystem<Param> {

    val createSelfLoops = true

    companion object {
        // possible orientations
        private val PositiveIn = 0
        private val PositiveOut = 1
        private val NegativeIn = 2
        private val NegativeOut = 3
    }

    val encoder = NodeEncoder(model)
    val dimensions = model.variables.size

    override final val stateCount: Int = encoder.stateCount

    private val successors: Array<List<Pair<Int, Param>>> = Array(stateCount) { emptyList<Pair<Int, Param>>() }
    private val predecessors: Array<List<Pair<Int, Param>>> = Array(stateCount) { emptyList<Pair<Int, Param>>() }

    init {
        val start = System.currentTimeMillis()
        solver.run {
            val vertices = Array(encoder.vertexCount) {
                Array<Any>(dimensions) { solver.ff }
            }

            Flux.fromIterable(0 until encoder.vertexCount)
                    .parallel().runOn(Schedulers.parallel())
                    .map { vertex ->
                        for (dim in 0 until dimensions) {
                            vertices[vertex][dim] = computeVertexColors(vertex, dim)
                        }
                    }.reduce({ _, _ -> Unit }).block()

            fun getVertexColor(vertex: Int, dim: Int): Param
                    = vertices[vertex][dim] as Param

            val facetColors = Array(stateCount) {
                Array(dimensions) { Array<Any>(4) { solver.ff } }
            }

            fun getFacetColor(state: Int, dim: Int, orientation: Int): Param
                    = facetColors[state][dim][orientation] as Param

            //enumerate all bit masks corresponding to vertices of a state
            val vertexMasks: IntArray = (0 until dimensions).fold(listOf(0)) { a, d ->
                a.map { it.shl(1) }.flatMap { listOf(it, it.or(1)) }
            }.toIntArray()

            Flux.fromIterable(0 until stateCount)
                    .parallel().runOn(Schedulers.parallel())
                    .map { state ->
                        for (dim in 0 until dimensions) {
                            for (orientation in 0..3) {
                                val positiveFacet = if (orientation == PositiveIn || orientation == PositiveOut) 1 else 0
                                val positiveDerivation = orientation == PositiveOut || orientation == NegativeIn
                                facetColors[state][dim][orientation] = vertexMasks
                                        .filter { it.shr(dim).and(1) == positiveFacet }
                                        .fold(ff) { a, mask ->
                                            val vertex = encoder.nodeVertex(state, mask)
                                            val p = getVertexColor(vertex, dim)
                                            if (positiveDerivation) p else p.not()
                                        }
                            }
                        }
                    }.reduce({ _, _ -> Unit }).block()


            for (time in listOf(true, false)) {
                val storage = if (time) successors else predecessors

                Flux.fromIterable(0 until stateCount)
                        .parallel().runOn(Schedulers.parallel())
                        .map { state ->
                            val result = ArrayList<Pair<Int, Param>>()
                            var selfloop = tt

                            for (dim in model.variables.indices) {

                                val positiveOut = getFacetColor(state, dim, if (time) PositiveOut else PositiveIn)
                                val positiveIn = getFacetColor(state, dim, if (time) PositiveIn else PositiveOut)
                                val negativeOut = getFacetColor(state, dim, if (time) NegativeOut else NegativeIn)
                                val negativeIn = getFacetColor(state, dim, if (time) NegativeIn else NegativeOut)

                                encoder.higherNode(state, dim)?.let { higher ->
                                    val colors = if (time) positiveOut else positiveIn
                                    if (colors.isSat()) result.add(higher to colors)

                                    if (createSelfLoops) {
                                        val positiveFlow = negativeIn and positiveOut and (negativeOut or positiveIn).not()
                                        selfloop = selfloop and positiveFlow.not()
                                    }
                                }

                                encoder.lowerNode(state, dim)?.let { lower ->
                                    val colors = if (time) negativeOut else negativeIn
                                    if (colors.isSat()) result.add(lower to colors)

                                    if (createSelfLoops) {
                                        val negativeFlow = negativeOut and positiveIn and (negativeIn or positiveOut).not()
                                        selfloop = selfloop and negativeFlow.not()
                                    }
                                }

                            }

                            if (selfloop.isSat()) {
                                result.add(state to selfloop)
                            }

                            storage[state] = result
                        }.reduce({ _, _ -> Unit }).block()
            }
        }
        println("State space computed in ${System.currentTimeMillis() - start}")
    }

    abstract fun computeVertexColors(vertex: Int, dimension: Int): Param

    override fun State.step(timeFlow: Boolean): Iterable<Pair<State, Param>> {
        return if (timeFlow) successors[this] else predecessors[this]
    }

    private fun facetIndex(from: Int, dimension: Int, orientation: Int)
            = from + (stateCount * dimension) + (stateCount * dimensions * orientation)
}