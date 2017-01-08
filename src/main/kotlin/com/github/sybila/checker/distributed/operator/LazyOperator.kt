package com.github.sybila.checker.distributed.operator

import com.github.sybila.checker.distributed.Operator
import com.github.sybila.checker.distributed.Channel
import com.github.sybila.checker.distributed.StateMap


open class LazyOperator<out Params: Any>(
        partition: Channel<Params>,
        initializer: Channel<Params>.() -> StateMap<Params>
) : Operator<Params> {

    //Initializer is thrown away after value is computed,
    //so any references for inner operators can be GCed
    private val value by lazy(LazyThreadSafetyMode.NONE) { partition.run(initializer) }

    override fun compute(): StateMap<Params> = value
}