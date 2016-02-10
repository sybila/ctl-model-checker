package cz.muni.fi.checker

import cz.muni.fi.ctl.Atom
import java.util.*

/**
 * Simple node represented only by it's ID
 */
data class IDNode(
        val id: Int
) : Node

/**
 * A node represented by it's coordinates in rectangular space.
 * Note: Don't use this in production. Usually it's cheaper to encode
 * the coordinates into one number.
 */
data class CoordinateNode(
        val coordinates: IntArray
) : Node

/**
 * Simple color set represented by a set of IDs
 */
data class IDColors(private val set: Set<Int> = HashSet()) : Colors<IDColors> {

    constructor(vararg items: Int): this(items.toSet())

    override fun minus(other: IDColors): IDColors = IDColors(set - other.set)

    override fun plus(other: IDColors): IDColors = IDColors(set + other.set)

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun intersect(other: IDColors): IDColors = IDColors(set.intersect(other.set))

}

/**
 * Utility edge data class
 */
data class Edge<N: Node, C: Colors<C>>(
        val start: N,
        val end: N,
        val colors: C
)

/**
 * Explicitly defined kripke fragment with some invariant verification.
 * Don't use for big models! (verification will kill you)
 */
class ExplicitKripkeFragment(
        nodes: Map<IDNode, IDColors>,
        edges: Set<Edge<IDNode, IDColors>>,
        validity: Map<Atom, Map<IDNode, IDColors>>
) : KripkeFragment<IDNode, IDColors> {

    private val emptyNodeSet = MapNodes(IDColors(), mapOf<IDNode, IDColors>())

    private val successorMap = edges
            .groupBy { it.start }
            .mapValues { entry ->
                entry.value.associateBy({ it.end }, { it.colors }).toNodes(IDColors())
            }

    private val predecessorMap = edges
            .groupBy { it.end }
            .mapValues { entry ->
                entry.value.associateBy({ it.start }, { it.colors }).toNodes(IDColors())
            }

    private val nodes = nodes.toNodes(IDColors())

    private val validity = validity
            .mapValues { it.value.toNodes(IDColors()) }

    override val successors: IDNode.() -> Nodes<IDNode, IDColors> = { successorMap.getOrElse(this, { emptyNodeSet }) }

    override val predecessors: IDNode.() -> Nodes<IDNode, IDColors> = { predecessorMap.getOrElse(this, { emptyNodeSet }) }

    init {
        for (valid in validity.values) {  //Invariant 1.
            for ((node, colors) in valid) {
                if (this.nodes[node] intersect colors != colors) {
                    throw IllegalArgumentException("Suspicious atom color in $node for $colors")
                }
            }
        }
        for (node in this.nodes.validKeys) { //Invariant 2.
            if (node.successors().isEmpty()) {
                throw IllegalArgumentException("Missing successors for $node")
            }
        }
        for ((from, to, colors) in edges) {
            if (from !in allNodes() && to !in allNodes()) {
                throw IllegalArgumentException("Invalid edge $from $to $colors")
            }
        }
    }

    override fun allNodes(): Nodes<IDNode, IDColors> = nodes

    override fun validNodes(a: Atom): Nodes<IDNode, IDColors> = validity.getOrElse(a, { emptyNodeSet })

}

val emptyIDNodes = emptyMap<IDNode, IDColors>().toNodes(IDColors())
fun Iterable<Pair<IDNode, IDColors>>.toIDNodes(): Nodes<IDNode, IDColors>
        = MapNodes(IDColors(), this.toMap())
fun Map<IDNode, IDColors>.toIDNodes(): Nodes<IDNode, IDColors>
        = MapNodes(IDColors(), this)


