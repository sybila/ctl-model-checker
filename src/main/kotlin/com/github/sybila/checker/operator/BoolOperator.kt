package com.github.sybila.checker.operator

import com.github.sybila.checker.Operator
import com.github.sybila.checker.Partition
import com.github.sybila.checker.StateMap

open class LazyOperator<out Params: Any>(
        initializer: () -> StateMap<Params>
) : Operator<Params> {

    //Initializer is thrown away after value is computed,
    //so any references for inner operators can be GCed
    private val value by lazy(LazyThreadSafetyMode.NONE, initializer)

    override fun compute(): StateMap<Params> = value
}

class AndOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Partition<Params>
) : LazyOperator<Params>({
    partition.run { left.compute() lazyAnd right.compute() }
})

class OrOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Partition<Params>
) : LazyOperator<Params>({
    partition.run { left.compute() lazyOr right.compute() }
})

class ComplementOperator<out Params : Any>(
        full: Operator<Params>, inner: Operator<Params>,
        partition: Partition<Params>
) : LazyOperator<Params>({
    partition.run { inner.compute() complementAgainst full.compute() }
})