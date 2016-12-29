package com.github.sybila.checker.partition

import com.github.sybila.checker.Model
import com.github.sybila.checker.Partition

class IntervalPartition<Params: Any>(
        override val partitionId: Int,
        val intervals: List<IntRange>,
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    override val partitionCount: Int = intervals.size

    override fun Int.owner(): Int = intervals.indexOfFirst { this in it }

    fun myInterval(): IntRange = intervals[partitionId]
}

fun <Params : Any> List<Pair<Model<Params>, IntRange>>.asIntervalPartitions(): List<IntervalPartition<Params>> {
    val intervals = this.map { it.second }
    return this.map { it.first }.mapIndexed { i, model -> IntervalPartition(i, intervals, model) }
}