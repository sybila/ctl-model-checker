package com.github.sybila.checker.distributed.partition

import com.github.sybila.checker.distributed.Model
import com.github.sybila.checker.distributed.MutableStateMap
import com.github.sybila.checker.distributed.Partition
import com.github.sybila.checker.distributed.map.mutable.ContinuousStateMap
import com.github.sybila.checker.distributed.map.mutable.HashStateMap

class IntervalPartition<Params: Any>(
        override val partitionId: Int,
        val intervals: List<IntRange>,
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    override val partitionCount: Int = intervals.size

    override fun Int.owner(): Int = intervals.indexOfFirst { this in it }

    fun myInterval(): IntRange = intervals[partitionId]

    override fun newLocalMutableMap(partition: Int): MutableStateMap<Params> {
        return if (partition == partitionId) {
            val range = intervals[partition]
            ContinuousStateMap(range.first, range.last + 1, ff)
        } else HashStateMap(ff)
    }
}

fun <Params : Any> List<Pair<Model<Params>, IntRange>>.asIntervalPartitions(): List<IntervalPartition<Params>> {
    val intervals = this.map { it.second }
    return this.map { it.first }.mapIndexed { i, model -> IntervalPartition(i, intervals, model) }
}