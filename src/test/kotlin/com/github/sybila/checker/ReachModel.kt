package com.github.sybila.checker
/*
import com.github.sybila.ctl.*


/**
 * Representation of n-dimensional hypercube of size s where all transitions lead
 * from one lower corner (0,0..) to upper corner (s-1,s-1,...), while each transition "adds" one color.
 * So border of upper corner can "go through" with almost all colors, while
 * lower corner transitions have only one color (zero)
 * So total number of colors is (size - 1) * dimensions + 1
 * Color zero goes through the whole model, last color does not have any transitions.
 *
 * WARNING: This implementation is hilariously inefficient. Really just use for testing.
 *
 * See: <a href="https://photos.google.com/share/AF1QipMGw9XEJiI9rMSw-u-JuOowwhKEuKuLWkWw-hAL8ZE84-QkBqkkX4d8fj2GEmkFpw?key=WnB0Vm94RDkwSGk0eU16enl4ZXAtUFNvLXM0SUN3">image</a>
 */
class ReachModel(
        private val dimensions: Int,
        private val dimensionSize: Int
) : KripkeFragment<IDNode, IDColors> {

    /**
     * Use these propositions in your model queries, nothing else is supported!
     */
    enum class Prop : Atom {
        UPPER_CORNER, LOWER_CORNER, CENTER, BORDER, UPPER_HALF;
        override val operator: Op = Op.ATOM
        override val subFormulas: List<Formula> = emptyList()
    }

    init {
        assert(dimensionSize > 0)
        assert(dimensions > 0)
        val size = Math.pow(dimensionSize.toDouble(), dimensions.toDouble())
        if (size.toLong() > size.toInt()) throw IllegalArgumentException("Model too big: $size")
    }

    val stateCount = pow(dimensionSize, dimensions)

    val states = Array(stateCount) { index -> IDNode(index) }

    val parameters = IDColors((0..((dimensionSize - 1) * dimensions + 1)).toSet())

    /**
     * Helper function to extract a coordinate from node id
     */
    fun extractCoordinate(node: IDNode, i: Int): Int = (node.id / pow(dimensionSize, i)) % dimensionSize

    /**
     * Encode node coordinates into an index
     */
    fun toStateIndex(coordinates: List<Int>): Int = coordinates.mapIndexed { i, e ->
        e * pow(dimensionSize, i)
    }.sum()

    /**
     * Returns the set of colors that can reach upper corner from given state. Very useful ;)
     */
    fun stateColors(state: IDNode): IDColors {
        return IDColors(0) + IDColors((0 until dimensions).flatMap { dim ->
            (1..extractCoordinate(state, dim)).map { it + (dimensionSize - 1) * dim }
        }.toSet())
    }

    override val successors: IDNode.() -> Nodes<IDNode, IDColors> = {
        ((0 until dimensions)
            .filter { extractCoordinate(this, it) + 1 < dimensionSize }
            .map { this.id + pow(dimensionSize, it) }
            .associate { id -> Pair(states[id], stateColors(this)) }
             + Pair(this, parameters - stateColors(this))).toIDNodes()
    }

    override val predecessors: IDNode.() -> Nodes<IDNode, IDColors> = {
        ((0 until dimensions)
                .filter { extractCoordinate(this, it) - 1 >= 0 }
                .map { this.id - pow(dimensionSize, it) }
                .associate { id -> Pair(states[id], stateColors(states[id])) }
                 + Pair(this, parameters - stateColors(this))).toIDNodes()
    }

    override fun allNodes(): Nodes<IDNode, IDColors> {
        return states.associate { Pair(it, parameters) }.toIDNodes()
    }

    override fun validNodes(a: Atom): Nodes<IDNode, IDColors> {
        val r = when (a) {
            True -> allNodes()
            False -> emptyIDNodes
            Prop.CENTER -> nodesOf(Pair(states[toStateIndex((1..dimensions).map { dimensionSize / 2 })], parameters))
            Prop.BORDER -> states.filter { state ->
                (0 until dimensions).any { val c = extractCoordinate(state, it); c == 0 || c == dimensionSize - 1 }
            }.associate { Pair(it, parameters) }.toIDNodes()
            Prop.UPPER_CORNER -> nodesOf(Pair(states[toStateIndex((1..dimensions).map { dimensionSize - 1 })], parameters))
            Prop.LOWER_CORNER -> nodesOf(Pair(states[toStateIndex((1..dimensions).map { 0 })], parameters))
            Prop.UPPER_HALF -> states.filter { state -> (0 until dimensions).all { extractCoordinate(state, it) >= dimensionSize/2 } }
                    .associate { Pair(it, parameters) }.toIDNodes()
            else -> throw IllegalArgumentException("Unsupported atom: $a")
        }
        return r
    }

}

/**
 * Wrapper around ReachModel that hides nodes that are not defined by partition function.
 * Inefficient, but it works fine and is basically a one liner! :)
 */
class ReachModelPartition(
        private val model: ReachModel,
        private val partition: PartitionFunction<IDNode>
) :
        KripkeFragment<IDNode, IDColors> by model,
        PartitionFunction<IDNode> by partition
{

    override fun allNodes(): Nodes<IDNode, IDColors> {
        return model.allNodes().entries.filter { partition.myId == it.key.ownerId() }.associate { it.toPair() }.toIDNodes()
    }

    override fun validNodes(a: Atom): Nodes<IDNode, IDColors> {
        return model.validNodes(a).entries.filter { partition.myId == it.key.ownerId() }.associate { it.toPair() }.toIDNodes()
    }

}*/