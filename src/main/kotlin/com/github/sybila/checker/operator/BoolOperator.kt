package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator

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