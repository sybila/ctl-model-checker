package com.github.sybila.checker

/**
 * Collection of various simple PartitionFunction implementations.
 */

/**
 * Partition function defined by explicit enumeration.
 *
 * You can either provide a direct or inverse mapping (or a combination of both)
 */
class ExplicitPartitionFunction<N: Node>(
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
class FunctionalPartitionFunction<N: Node>(
        private val id: Int,
        private val function: (N) -> Int
) : PartitionFunction<N> {
    override val ownerId: N.() -> Int = {function(this)}
    override val myId: Int = id
}

/**
 * "No partition"
 */
class UniformPartitionFunction<N: Node>(private val id: Int = 0) : PartitionFunction<N> {
    override val ownerId: N.() -> Int = { id }
    override val myId: Int = id
}
