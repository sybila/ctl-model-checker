package com.github.sybila.checker.distributed.partition

import com.github.sybila.checker.distributed.Model
import com.github.sybila.checker.distributed.Partition

class BlockPartition<Params : Any>(
        override val partitionId: Int,
        override val partitionCount: Int,
        private val blockSize: Int,
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    override fun Int.owner(): Int = (this / blockSize) % partitionCount

}

fun <Params : Any> List<Model<Params>>.asBlockPartitions(blockCount: Int): List<BlockPartition<Params>> {
    return this.mapIndexed { i, model -> BlockPartition(i, this.size, model.stateCount / blockCount, model) }
}