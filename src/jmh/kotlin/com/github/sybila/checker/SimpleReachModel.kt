package com.github.sybila.checker

import com.github.sybila.checker.solver.BoolSolver
import com.github.sybila.huctl.*
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.*


/**
 * Simplified version of the SimpleReachModel used for testing.
 *
 * This version does not have parameters, hence should minimize time spent in solver.
 */

@State(Scope.Benchmark)
open class SimpleReachModel(
        private val dimensions: Int = 3,
        private val dimensionSize: Int = 10
) : Model<Boolean>, Solver<Boolean> by BoolSolver() {

    @Setup(Level.Trial)
    fun cacheTransitions() {
        for (state in 0 until stateCount) {
            state.successors(true)
            state.predecessors(true)
        }
    }

    /**
     * Use these propositions in your model queries, nothing else is supported!
     */
    enum class Prop : () -> Formula.Atom.Float {
        UPPER_CORNER, LOWER_CORNER, CENTER, BORDER, UPPER_HALF;

        override fun invoke(): Formula.Atom.Float {
            return when (this) {
                UPPER_CORNER -> "upper".asVariable() gt 0.0.asConstant()
                LOWER_CORNER -> "lower".asVariable() gt 0.0.asConstant()
                CENTER -> "center".asVariable() gt 0.0.asConstant()
                BORDER -> "border".asVariable() gt 0.0.asConstant()
                UPPER_HALF -> "upper_half".asVariable() gt 0.0.asConstant()
            }
        }
    }

    init {
        assert(dimensionSize > 0)
        assert(dimensions > 0)
        val size = Math.pow(dimensionSize.toDouble(), dimensions.toDouble())
        if (size.toLong() > size.toInt()) throw IllegalArgumentException("Model too big: $size")
    }

    override val stateCount = pow(dimensionSize, dimensions)

    /**
     * Helper function to extract a coordinate from node id
     */
    fun extractCoordinate(node: Int, i: Int): Int = (node / pow(dimensionSize, i)) % dimensionSize

    /**
     * Encode node coordinates into an index
     */
    fun toStateIndex(coordinates: List<Int>): Int = coordinates.mapIndexed { i, e ->
        e * pow(dimensionSize, i)
    }.sum()



    private fun step(from: Int, successors: Boolean, timeFlow: Boolean): List<Transition<Boolean>> {
        val dim = (0 until dimensions).asSequence()
        val step = if (successors == timeFlow) {
            dim .filter { extractCoordinate(from, it) + 1 < dimensionSize }
                    .map { it to from + pow(dimensionSize, it) }
        } else {
            dim .filter { extractCoordinate(from, it) - 1 > -1 }
                    .map { it to from - pow(dimensionSize, it) }
        }
        val transitions = step.map {
            val state = it.second
            val dimName = it.first.toString()
            Transition(state, if (timeFlow) dimName.increaseProp() else dimName.decreaseProp(), true)
        }.toList()
        val loop = if (transitions.isEmpty()) listOf(Transition(from, DirectionFormula.Atom.Loop, true)) else emptyList()
        return (transitions + loop)
    }

    private val successors = HashMap<Int, List<Transition<Boolean>>>()
    private val predecessors = HashMap<Int, List<Transition<Boolean>>>()

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Boolean>> {
        if (!timeFlow) throw IllegalArgumentException("Only positive time allowed in this model")
        return (successors.computeIfAbsent(this) {
            step(this, true, timeFlow)
        }).iterator()
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Boolean>> {
        if (!timeFlow) throw IllegalArgumentException("Only positive time allowed in this model")
        return (predecessors.computeIfAbsent(this) {
            step(this, false, timeFlow)
        }).iterator()
    }

    override fun Formula.Atom.Float.eval(): StateMap<Boolean> {
        return when (this) {
            Prop.CENTER() -> toStateIndex((1..dimensions).map { dimensionSize / 2 }).asStateMap(tt)
            Prop.UPPER_CORNER() -> toStateIndex((1..dimensions).map { dimensionSize - 1 }).asStateMap(tt)
            Prop.LOWER_CORNER() -> toStateIndex((1..dimensions).map { 0 }).asStateMap(tt)
            Prop.BORDER() -> (0 until stateCount).asSequence().filter { state ->
                (0 until dimensions).any { val c = extractCoordinate(state, it); c == 0 || c == dimensionSize - 1 }
            }.associateBy({it}, { tt }).asStateMap()
            Prop.UPPER_HALF() -> (0 until stateCount).asSequence().filter { state ->
                (0 until dimensions).all { extractCoordinate(state, it) >= dimensionSize/2 }
            }.associateBy({it}, { tt }).asStateMap()
            else -> throw IllegalStateException("Unexpected atom $this")
        }
    }

    override fun Formula.Atom.Transition.eval(): StateMap<Boolean> { throw UnsupportedOperationException("not implemented") }

    private fun pow (a: Int, b: Int): Int {
        if ( b == 0)        return 1
        if ( b == 1)        return a
        if ( b % 2 == 0)    return pow (a * a, b / 2)       //even a=(a^2)^b/2
        else                return a * pow (a * a, b / 2)   //odd  a=a*(a^2)^b/2
    }
}