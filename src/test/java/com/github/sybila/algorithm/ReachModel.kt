package com.github.sybila.algorithm

import com.github.sybila.model.State
import com.github.sybila.model.TransitionSystem
import com.github.sybila.model.toStateMap
import com.github.sybila.solver.BitSetSolver
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
) : TransitionSystem<BitSet> {

    val solver = BitSetSolver(BitSet().apply {
        set(0, (dimensionSize - 1) * dimensions + 2)
    })

    private val colorCache = HashMap<Int, BitSet>()

    init {
        assert(dimensionSize > 0)
        assert(dimensions > 0)
        val size = Math.pow(dimensionSize.toDouble(), dimensions.toDouble())
        if (size.toLong() > size.toInt()) throw IllegalArgumentException("Model too big: $size")
    }

    override val stateCount = pow(dimensionSize, dimensions)

    init {
        // pre-compute state colors, otherwise we won't be thread safe
        for (s in 0 until stateCount) { stateColors(s) }
    }

    val center = run {
        val center = toStateIndex((1..dimensions).map { dimensionSize / 2 })
        mapOf(center to solver.tt).toStateMap(solver, stateCount)
    }.asMono()

    val upperCorner = run {
        val corner = toStateIndex((1..dimensions).map { dimensionSize - 1 })
        mapOf(corner to solver.tt).toStateMap(solver, stateCount)
    }.asMono()

    val lowerCorner = run {
        val corner = toStateIndex((1..dimensions).map { 0 })
        mapOf(corner to solver.tt).toStateMap(solver, stateCount)
    }.asMono()

    val border = (0 until stateCount).filter { state ->
        (0 until dimensions).any { val c = extractCoordinate(state, it); c == 0 || c == dimensionSize - 1 }
    }.associateBy({it}, { solver.tt }).toStateMap(solver, stateCount).asMono()

    val upperHalf = (0 until stateCount).filter { state ->
        (0 until dimensions).all { extractCoordinate(state, it) >= dimensionSize/2 }
    }.associateBy({it}, { solver.tt }).toStateMap(solver, stateCount).asMono()

    override fun State.step(timeFlow: Boolean): Iterable<Pair<State, BitSet>> {
        val from = this
        val dim = (0 until dimensions).asSequence()
        val states =
                if (timeFlow) dim
                    .filter { extractCoordinate(from, it) + 1 < dimensionSize }
                    .map { from + pow(dimensionSize, it) }
                else dim
                    .filter { extractCoordinate(from, it) - 1 > -1 }
                    .map { from - pow(dimensionSize, it) }

        return states.map {
            it to stateColors(from)
        }.toList()
    }

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

    /**
     * Returns the set of colors that can reach upper corner from given state. Very useful ;)
     */
    fun stateColors(state: Int): BitSet {
        return colorCache.computeIfAbsent(state) {
            val set = BitSet()

            set.set(0)
            for (dim in 0 until dimensions) {
                for (p in 1..extractCoordinate(state, dim)) {
                    set.set(p + (dimensionSize - 1) * dim)
                }
            }

            set
        }
    }

    private fun pow (a: Int, b: Int): Int {
        if ( b == 0)        return 1
        if ( b == 1)        return a
        if ( b % 2 == 0)    return pow (a * a, b / 2)       //even a=(a^2)^b/2
        else                return a * pow (a * a, b / 2)   //odd  a=a*(a^2)^b/2
    }
}