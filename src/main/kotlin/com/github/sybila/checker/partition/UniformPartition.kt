package com.github.sybila.checker.partition

import com.github.sybila.checker.Model
import com.github.sybila.checker.MutableStateMap
import com.github.sybila.checker.Partition
import com.github.sybila.checker.map.mutable.ContinuousStateMap
import com.github.sybila.checker.map.mutable.HashStateMap


class UniformPartition<Params : Any>(
        override val partitionId: Int,
        override val partitionCount: Int,
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    //Ceil ensures that partitionCount * statesPerPartition >= stateCount, hence
    //stateId / statesPerPartition will end in [0, partitionCount) range.
    private val statesPerPartition = Math.ceil(stateCount.toDouble() / partitionCount.toDouble()).toInt()

    override fun Int.owner(): Int = this / statesPerPartition

    override fun newLocalMutableMap(partition: Int): MutableStateMap<Params> {
        return if (partition == partitionId) {
            ContinuousStateMap(partition * statesPerPartition, (partition + 1) * statesPerPartition, ff)
        } else HashStateMap(ff)
    }
}

fun <Params : Any> List<Model<Params>>.asUniformPartitions(): List<UniformPartition<Params>> {
    return this.mapIndexed { i, model -> UniformPartition(i, this.size, model) }
}