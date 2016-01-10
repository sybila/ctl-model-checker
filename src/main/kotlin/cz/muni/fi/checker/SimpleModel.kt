package cz.muni.fi.checker

import cz.muni.fi.ctl.Atom
import java.util.*

/**
 * Simple node represented only by it's ID
 */
public data class IDNode(
        public val id: Int
) : Node

/**
 * A node represented by it's coordinates in sum rectangular space.
 * Note: Don't use this in production. Usually it's much cheaper to encode
 * the coordinates into one number.
 */
public data class CoordinateNode(
        public val coordinates: IntArray
) : Node

/**
 * Simple color set represented by a set of IDs
 */
public data class IDColors(private val set: Set<Int> = HashSet()) : Colors<IDColors> {

    constructor(vararg items: Int): this(items.toSet())

    override fun minus(other: IDColors): IDColors = IDColors(set - other.set)

    override fun plus(other: IDColors): IDColors = IDColors(set + other.set)

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun intersect(other: IDColors): IDColors = IDColors(set.intersect(other.set))

}

/**
 * Primitive alternative to IDColors that stores color sets as intervals.
 */
public data class IntervalColors(
        private val intervals: Set<Interval> = emptySet()
) : Colors<IntervalColors> {

    constructor(vararg intervals: Interval): this(intervals.toSet())

    override fun minus(other: IntervalColors): IntervalColors
            = IntervalColors(normalize(intervals
                    .flatMap { fst ->  other.intervals.map { snd -> fst - snd } }
                    .filterNotNull().toSet()
            ))

    override fun plus(other: IntervalColors): IntervalColors
            = IntervalColors(normalize(intervals + other.intervals))

    override fun intersect(other: IntervalColors): IntervalColors
            = IntervalColors(normalize(intervals
                    .flatMap { fst -> other.intervals.map { snd -> fst intersect snd } }
                    .filter { !it.isEmpty() }.toSet()
            ))

    override fun isEmpty(): Boolean = intervals.isEmpty()

    private fun normalize(items: Set<Interval>): Set<Interval> {
        val nonRedundant = HashSet<Interval>()

        for (range in items) {
            if (items.all { it == range || !(it encloses range) }) {
                nonRedundant.add(range)
            }
        }

        val independent = HashSet<Interval>()

        while (nonRedundant.isNotEmpty()) {
            val candidate = nonRedundant.first()
            nonRedundant.remove(candidate)

            //println("candidate: $candidate")

            val merge = nonRedundant.asSequence()   //should be lazy
                    .map { Pair(it merge candidate, it) }
                    .filter { it.first != null }.firstOrNull()

            //println("Remaining: $nonRedundant")
            //println("merge: $merge")

            if (merge == null) {    //can't merge with anything!
                independent.add(candidate)
            } else {                //merge again!
                //remove comes first, since if fst == snd, the set would remain empty
                nonRedundant.remove(merge.second)
                nonRedundant.add(merge.first!!)
            }

        }

        return independent
    }
}

/**
 * Partition function defined by explicit enumeration.
 *
 * You can either provide a direct or inverse mapping (or a combination of both)
 */
public class ExplicitPartitionFunction<N: Node>(
        private val id: Int,
        directMapping: Map<N, Int> = mapOf(),
        inverseMapping: Map<Int, List<N>> = mapOf()
): PartitionFunction<N> {

    private val mapping =
            directMapping + inverseMapping.flatMap { entry ->
                entry.value.map { Pair(it, entry.key) }
            }.toMap()

    init {
        //check if we preserved size of input - i.e. the input is a valid function
        if (mapping.size != directMapping.size + inverseMapping.map { it.value.size }.sum()) {
            throw IllegalArgumentException("Provided mapping is not a function! $directMapping  $inverseMapping")
        }
    }

    override val ownerId: N.() -> Int = { mapping[this]!! }
    override val myId: Int = id
}

/**
 * Partition function defined by an actual function from nodes to ids.
 */
public class FunctionalPartitionFunction<N: Node>(
        private val id: Int,
        private val function: (N) -> Int
) : PartitionFunction<N> {
    override val ownerId: N.() -> Int = {function(this)}
    override val myId: Int = id
}

/**
 * "No partition"
 */
public class UniformPartitionFunction<N: Node>(private val id: Int = 0) : PartitionFunction<N> {
    override val ownerId: N.() -> Int = { id }
    override val myId: Int = id
}

/**
 * Utility data class
 */
public data class Edge<N: Node, C: Colors<C>>(
        public val start: N,
        public val end: N,
        public val colors: C
)

/**
 * Explicitly defined kripke fragment with some invariant verification.
 * Don't use for big models! (verification will kill you)
 */
public class ExplicitKripkeFragment(
        nodes: Map<IDNode, IDColors>,
        edges: Set<Edge<IDNode, IDColors>>,
        validity: Map<Atom, Map<IDNode, IDColors>>
) : KripkeFragment<IDNode, IDColors> {

    private val emptyNodeSet = MapNodes(IDColors(), mapOf<IDNode, IDColors>())

    private val successorMap = edges
            .groupBy { it.start }
            .mapValues { entry ->
                entry.value.toMap({ it.end }, { it.colors }).toNodes(IDColors())
            }.withDefault { emptyNodeSet }

    private val predecessorMap = edges
            .groupBy { it.end }
            .mapValues { entry ->
                entry.value.toMap({ it.start }, { it.colors }).toNodes(IDColors())
            }.withDefault { emptyNodeSet }

    private val nodes = nodes.toNodes(IDColors())

    private val validity = validity
            .mapValues { it.value.toNodes(IDColors()) }
            .withDefault { emptyNodeSet }

    override val successors: IDNode.() -> Nodes<IDNode, IDColors> = { successorMap.getOrImplicitDefault(this) }

    override val predecessors: IDNode.() -> Nodes<IDNode, IDColors> = { predecessorMap.getOrImplicitDefault(this) }

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

    override fun validNodes(a: Atom): Nodes<IDNode, IDColors> = validity.getOrImplicitDefault(a)

}

public val emptyIDNodes = emptyMap<IDNode, IDColors>().toNodes(IDColors())
public fun Iterable<Pair<IDNode, IDColors>>.toIDNodes(): Nodes<IDNode, IDColors>
        = MapNodes(IDColors(), this.toMap())
public fun Map<IDNode, IDColors>.toIDNodes(): Nodes<IDNode, IDColors>
        = MapNodes(IDColors(), this)


