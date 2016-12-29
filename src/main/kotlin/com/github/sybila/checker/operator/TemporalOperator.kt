package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator
import com.github.sybila.checker.eval
import com.github.sybila.huctl.DirectionFormula
import java.util.*

class ExistsNextOperator<out Params: Any>(
        timeFlow: Boolean, direction: DirectionFormula,
        inner: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val storage = (0 until partitionCount).map { newMutableMap(it) }


    //distribute data from inner formula
    for ((state, value) in inner.compute().entries()) {
        for ((predecessor, dir, bound) in state.predecessors(timeFlow)) {
            if (direction.eval(dir)) {
                val witness = value and bound
                if (witness.canSat()) {
                    storage[predecessor.owner()].setOrUnion(predecessor, witness)
                }
            }
        }
    }

    //gather data from everyone else
    val transmission: Array<List<Pair<Int, Params>>?> = storage.mapIndexed { i, map ->
        if (i == partitionId) null else map.entries().asSequence().toList()
    }.map { if(it?.isEmpty() ?: true) null else it }.toTypedArray()
    val received = mapReduce(transmission)
    received?.forEach { storage[partitionId].setOrUnion(it.first, it.second) }

    storage[partitionId]
})