package com.github.sybila.checker

import com.github.sybila.checker.map.PartitionStateMap
import com.github.sybila.checker.map.mutable.HashStateMap

/**
 * A portion of a state space.
 *
 * Partitions of the same model can't overlap.
 * Each model partition has it's own solver (unless the solver is thread safe).
 *
 * @Contract The predecessor/successor methods return states from every partition.
 * @Contract The proposition eval returns only states that belong to this partition.
 *
 */
interface Partition<Params : Any> : Model<Params> {

    /**
     * Total number of partitions.
     */
    val partitionCount: Int

    /**
     * ID of this partition.
     *
     * @Contract partitionId in (0 until partitionCount)
     */
    val partitionId: Int

    /**
     * ID of the state partition id.
     *
     * @Contract partitionId in (0 until partitionCount)
     * @Contract state in (0 until stateCount)
     */
    fun Int.owner(): Int

    /**
     * @Contract state in (0 until stateCount)
     */
    operator fun contains(state: Int): Boolean = state.owner() == partitionId

    /**
     * Create a new mutable map that will be used to store states only from given partition.
     *
     * Read operations on such map are still permitted for every state (just return false),
     * but write operations can fail if the state is not part of the partition.
     *
     * (This way the best map implementation for a specific partition can be chosen)
     */
    fun newLocalMutableMap(partition: Int): MutableStateMap<Params> = HashStateMap(ff)

    fun StateMap<Params>.restrictToPartition(): StateMap<Params> = PartitionStateMap(this, this@Partition)

}