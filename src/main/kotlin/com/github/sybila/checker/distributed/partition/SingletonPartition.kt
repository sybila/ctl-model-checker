package com.github.sybila.checker.distributed.partition

import com.github.sybila.checker.distributed.Model
import com.github.sybila.checker.distributed.Partition

class SingletonPartition<Params : Any>(
        model: Model<Params>
) : Partition<Params>, Model<Params> by model {

    override val partitionCount: Int = 1
    override val partitionId: Int = 0

    override fun Int.owner(): Int = 0

}

fun <Params : Any> Model<Params>.asSingletonPartition(): SingletonPartition<Params> = SingletonPartition(this)