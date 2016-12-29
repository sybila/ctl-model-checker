package com.github.sybila.checker.partition

import com.github.sybila.checker.Model
import com.github.sybila.checker.Partition


class UniformPartition<Params : Any>(
        override val partitionId: Int,
        override val partitionCount: Int,
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    //Ceil ensures that partitionCount * statesPerPartition >= stateCount, hence
    //stateId / statesPerPartition will end in [0, partitionCount) range.
    private val statesPerPartition = Math.ceil(stateCount.toDouble() / partitionCount.toDouble()).toInt()

    override fun Int.owner(): Int = this / statesPerPartition

}

fun <Params : Any> List<Model<Params>>.asUniformPartitions(): List<UniformPartition<Params>> {
    return this.mapIndexed { i, model -> UniformPartition(i, this.size, model) }
}