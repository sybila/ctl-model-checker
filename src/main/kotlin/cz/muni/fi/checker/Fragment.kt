package cz.muni.fi.checker

import cz.muni.fi.ctl.Atom

/**
 * Represents a Node (empty for now, reserved for future use)
 */
public interface Node { }

/**
 * Represents an immutable set of colors (parameters) of the model.
 * All operations should behave as normal mathematical set.
 *
 * The relation of equivalence does not have to satisfy the set semantics,
 * since these sets are often symbolic and can't be compared easily or at all.
 * (Main algorithm does not require it, but it can be used during testing,
 * so if you want to use the default test suite, you have to implement some sort
 * of equivalence)
 */
public interface Colors<C> {

    operator fun minus(other: C): C

    operator fun plus(other: C): C

    infix fun intersect(other: C): C

    infix fun union(other: C): C = this + other

    infix fun subtract(other: C): C = this - other

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

}

public interface PartitionFunction<N: Node> {

    /**
     * Get ID of partition which owns given node.
     */
    val ownerId: N.() -> Int

    /**
     * My partition ID.
     */
    val myId: Int

}


/**
 * Immutable (but can be lazy) Kripke fragment allowing colored nodes and edges.
 *
 * Note: Several invariants/contracts should hold for each valid Kripke fragment
 * 1. validNodes should always return subset of allNodes (with respect to colors).
 * 2. for every node N and color C from allNodes, successors(N) should be non empty (no finite paths).
 * 3. Border state set is a union of all successors/predecessors minus all nodes (with respect to colors).
 * 4. If N is successor of M for colors C, M is predecessor of N for colors C.
 * 5. The default value in all node sets is always an empty color space.
 * 6. The color set returned by successors/predecessors minus set returned by allNodes does not have to be empty.
 */
public interface KripkeFragment<N: Node, C: Colors<C>> {

    /**
     * Find all successors of given node. (Even in different fragments)
     */
    val successors: N.() -> Nodes<N, C>

    /**
     * Find all predecessors of given node. (Even in different fragments)
     */
    val predecessors: N.() -> Nodes<N, C>

    /**
     * Map of all (non border) nodes of the fragment with colors for which they are valid
     */
    fun allNodes(): Nodes<N, C>

    /**
     * Find all nodes (and respective colors) where given atomic proposition holds in this fragment.
     */
    fun validNodes(a: Atom): Nodes<N, C>

}