package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator

class AndOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, {
    val result = partition.newLocalMutableMap(partitionId)

    val l = left.compute()
    val r = right.compute()

    partition.run {
        for ((state, value) in l.entries()) {
            if (state in r) {
                val and = value and r[state]
                if (and.isSat()) {
                    result[state] = and
                }
            }
        }
    }

    result
})

class OrOperator<out Params : Any>(
        left: Operator<Params>, right: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, {
    val result = partition.newLocalMutableMap(partitionId)

    val l = left.compute()
    val r = right.compute()

    partition.run {
        for ((state, value) in l.entries()) {
            if (state in r) {
                result[state] = value or r[state]
            } else {
                result[state] = value
            }
        }
        for ((state, value) in r.entries()) {
            if (state !in l) {
                result[state] = value
            }
        }
    }

    result
})

class ComplementOperator<out Params : Any>(
        full: Operator<Params>, inner: Operator<Params>,
        partition: Channel<Params>
) : LazyOperator<Params>(partition, {
    val result = partition.newLocalMutableMap(partitionId)
    val f = full.compute()
    val i = inner.compute()

    partition.run {
        for ((state, value) in f.entries()) {
            if (state in i) {
                val new = value and i[state].not()
                if (new.isSat()) {
                    result[state] = new
                }
            } else {
                result[state] = value
            }
        }
    }

    result
})