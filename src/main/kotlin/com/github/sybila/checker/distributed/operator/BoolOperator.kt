package com.github.sybila.checker.distributed.operator

import com.github.sybila.checker.distributed.Channel
import com.github.sybila.checker.distributed.Operator

class AndOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, { left.compute() lazyAnd right.compute() })

class OrOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, { left.compute() lazyOr right.compute() })

class ComplementOperator<out Params : Any>(
        full: Operator<Params>, inner: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, { inner.compute() complementAgainst full.compute() })