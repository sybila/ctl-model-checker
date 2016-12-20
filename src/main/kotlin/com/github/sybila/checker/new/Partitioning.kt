package com.github.sybila.checker.new


/**
 * Partition function defined by explicit enumeration.
 *
 * You can either provide a direct or inverse mapping (or a combination of both)
 */
class ExplicitPartitionFunction(
        override val id: Int,
        directMapping: Map<Int, Int> = mapOf(),
        inverseMapping: Map<Int, List<Int>> = mapOf()
): PartitionFunction {

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

    override fun Int.owner(): Int = mapping[this]!!

}

/**
 * Partition function defined by an actual function from nodes to ids.
 */
class FunctionalPartitionFunction(
        override val id: Int,
        private val function: (Int) -> Int
) : PartitionFunction {
    override fun Int.owner(): Int = function(this)
}

/**
 * "No partition"
 */
class UniformPartitionFunction(override val id: Int = 0) : PartitionFunction {
    override fun Int.owner(): Int = id
}