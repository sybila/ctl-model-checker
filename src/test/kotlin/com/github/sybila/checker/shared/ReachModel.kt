package com.github.sybila.checker.shared

import com.github.sybila.checker.Transition
import com.github.sybila.checker.decreaseProp
import com.github.sybila.checker.increaseProp
import com.github.sybila.checker.pow
import com.github.sybila.checker.shared.solver.BitSetParams
import com.github.sybila.checker.shared.solver.BitSetSolver
import com.github.sybila.checker.shared.solver.asParams
import com.github.sybila.huctl.*
import java.util.*

/**
 * Representation of n-dimensional hypercube of size s where all transitions lead
 * from one lower corner (0,0..) to upper corner (s-1,s-1,...), while each transition "adds" one color.
 * So border of upper corner can "go through" with almost all colors, while
 * lower corner transitions have only one color (zero)
 * Total number of colors is (size - 1) * dimensions + 1
 * Color zero goes through the whole model, last color does not have any transitions.
 *
 * Note: All transition are increasing.
 *
 * WARNING: This implementation is hilariously inefficient. Really just use for testing.
 *
 * See: <a href="https://photos.google.com/share/AF1QipMGw9XEJiI9rMSw-u-JuOowwhKEuKuLWkWw-hAL8ZE84-QkBqkkX4d8fj2GEmkFpw?key=WnB0Vm94RDkwSGk0eU16enl4ZXAtUFNvLXM0SUN3">image</a>
 */

class ReachModel(
        private val dimensions: Int,
        private val dimensionSize: Int
) : TransitionSystem, Solver by BitSetSolver((dimensionSize - 1) * dimensions + 2) {

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
        if (size.toLong() > size.toInt()) throw IllegalArgumentException("TransitionSystem too big: $size")
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


    private val colorCache = HashMap<Int, BitSetParams>()

    /**
     * Returns the set of colors that can reach upper corner from given state. Very useful ;)
     */
    fun stateColors(state: Int): BitSetParams {
        return colorCache.computeIfAbsent(state) {
            val set = BitSet()

            set.set(0)
            for (dim in 0 until dimensions) {
                for (p in 1..extractCoordinate(state, dim)) {
                    set.set(p + (dimensionSize - 1) * dim)
                }
            }

            set.asParams()
        }
    }

    private fun step(from: Int, successors: Boolean, timeFlow: Boolean): Iterator<Transition<Params>> {
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
            Transition(state, if (timeFlow) dimName.increaseProp() else dimName.decreaseProp(), stateColors(state))
        }
        val loop = Transition(from, DirectionFormula.Atom.Loop, Not(stateColors(from)))
        return (transitions + sequenceOf(loop)).iterator()
    }

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Params>> = step(this, true, timeFlow)

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Params>> = step(this, false, timeFlow)

    override fun Formula.Atom.Float.eval(): StateMap {
        return when (this) {
            Prop.CENTER() -> toStateIndex((1..dimensions).map { dimensionSize / 2 }).asStateMap(TT)
            Prop.UPPER_CORNER() -> toStateIndex((1..dimensions).map { dimensionSize - 1 }).asStateMap(TT)
            Prop.LOWER_CORNER() -> toStateIndex((1..dimensions).map { 0 }).asStateMap(TT)
            Prop.BORDER() -> (0 until stateCount).asSequence().filter { state ->
                (0 until dimensions).any { val c = extractCoordinate(state, it); c == 0 || c == dimensionSize - 1 }
            }.associateBy({it}, { TT }).asStateMap()
            Prop.UPPER_HALF() -> (0 until stateCount).asSequence().filter { state ->
                (0 until dimensions).all { extractCoordinate(state, it) >= dimensionSize/2 }
            }.associateBy({it}, { TT }).asStateMap()
            else -> throw IllegalStateException("Unexpected atom $this")
        }
    }

    override fun Formula.Atom.Transition.eval(): StateMap { throw UnsupportedOperationException("not implemented") }

}