package com.github.sybila.checker

import com.github.sybila.checker.new.*
import com.github.sybila.huctl.*


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
        private val dimensionSize: Int,
        private val partitionFunction: PartitionFunction = object : PartitionFunction {
            override val id: Int = 0
            override fun Int.owner(): Int = 0
        }
) : Fragment<Set<Int>>, PartitionFunction by partitionFunction {

    init {
        assert(dimensionSize > 0)
        assert(dimensions > 0)
        val size = Math.pow(dimensionSize.toDouble(), dimensions.toDouble())
        if (size.toLong() > size.toInt()) throw IllegalArgumentException("Model too big: $size")
    }

    val stateCount = pow(dimensionSize, dimensions)

    val states = Array(stateCount) { it }

    val parameters = (0..((dimensionSize - 1) * dimensions + 1)).toSet()

    /**
     * Use these propositions in your model queries, nothing else is supported!
     */
    enum class Prop : () -> Formula.Atom {
        UPPER_CORNER, LOWER_CORNER, CENTER, BORDER, UPPER_HALF;

        override fun invoke(): Formula.Atom {
            return when (this) {
                //TODO: Change to references
                UPPER_CORNER -> "upper".positiveIn()
                LOWER_CORNER -> "lower".positiveIn()
                CENTER -> "center".positiveIn()
                BORDER -> "border".positiveIn()
                UPPER_HALF -> "upper_half".positiveIn()
            }
        }
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
    fun stateColors(state: Int): Set<Int> {
        return setOf(0) + (0 until dimensions).flatMap { dim ->
            (1..extractCoordinate(state, dim)).map { it + (dimensionSize - 1) * dim }
        }.toSet()
    }

    override fun step(from: Int, future: Boolean): Iterator<Transition<Set<Int>>> {
        val r = (if (future) {
            (0 until dimensions)
                    .filter { extractCoordinate(from, it) + 1 < dimensionSize } //don't escape from model
                    .map { from + pow(dimensionSize, it) }
        } else {
            (0 until dimensions)
                    .filter { extractCoordinate(from, it) - 1 > -1 }            //don't escape from model
                    .map { from - pow(dimensionSize, it) }                   //create new id
        }.map { Transition(it, it.toString().increaseProp(), stateColors(it)) } +
                listOf(Transition(from, DirectionFormula.Atom.Proposition("loop", Facet.POSITIVE), parameters - stateColors(from))))
        //TODO loops + listOf(Transition(from, LOOP, parameters - stateColors(from)))
        return r.iterator()
    }

    override fun eval(atom: com.github.sybila.huctl.Formula.Atom): StateMap<Set<Int>> {
        return (when (atom) {
            True -> states.asIterable()
            False -> listOf<Int>()
            Prop.CENTER() -> listOf(states[toStateIndex((1..dimensions).map { dimensionSize / 2 })])
            Prop.BORDER() -> states.filter { state ->
                (0 until dimensions).any { val c = extractCoordinate(state, it); c == 0 || c == dimensionSize - 1 }
            }
            Prop.UPPER_CORNER() -> listOf(states[toStateIndex((1..dimensions).map { dimensionSize - 1 })])
            Prop.LOWER_CORNER() -> listOf(states[toStateIndex((1..dimensions).map { 0 })])
            Prop.UPPER_HALF() -> states.filter { state ->
                (0 until dimensions).all { extractCoordinate(state, it) >= dimensionSize/2 }
            }
            else -> throw IllegalArgumentException("Unknown proposition $atom")
        }).filter { it.owner() == id }.associateBy({it}, {parameters}).asStateMap(setOf())
    }
}