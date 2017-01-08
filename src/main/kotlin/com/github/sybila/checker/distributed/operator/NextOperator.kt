package com.github.sybila.checker.distributed.operator

import com.github.sybila.checker.distributed.Channel
import com.github.sybila.checker.distributed.Operator
import com.github.sybila.checker.eval
import com.github.sybila.checker.distributed.map.mutable.HashStateMap
import com.github.sybila.huctl.DirectionFormula

class ExistsNextOperator<out Params: Any>(
        timeFlow: Boolean, direction: DirectionFormula,
        inner: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val storage = (0 until partitionCount).map { newLocalMutableMap(it) }

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
    val received = mapReduce(storage.prepareTransmission(partitionId))
    received?.forEach { storage[partitionId].setOrUnion(it.first, it.second) }

    storage[partitionId]
})

class AllNextOperator<out Params : Any>(
        timeFlow: Boolean, direction: DirectionFormula,
        inner: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val satisfied = (0 until partitionCount).map { HashStateMap(ff) }
    val candidates = (0 until partitionCount).map { newLocalMutableMap(it) }
    val mySatisfied = satisfied[partitionId]
    val myCandidates = candidates[partitionId]

    //distribute data so that everyone sees their successors that are satisfied in the inner formula
    //and also mark all such states as candidates (candidate essentially means EX phi holds)
    for ((state, value) in inner.compute().entries()) {
        for ((predecessor, dir, bound) in state.predecessors(timeFlow)) {
            if (direction.eval(dir)) {  //if direction is false, predecessor will be false for the entire bound
                val candidate = value and bound
                if (candidate.canSat()) {
                    satisfied[predecessor.owner()].setOrUnion(state, candidate)
                    candidates[predecessor.owner()].setOrUnion(predecessor, candidate)
                }
            }
        }
    }

    mapReduce(satisfied.prepareTransmission(partitionId))?.forEach {
        mySatisfied.setOrUnion(it.first, it.second)
    }
    mapReduce(candidates.prepareTransmission(partitionId))?.forEach {
        myCandidates.setOrUnion(it.first, it.second)
    }

    val result = newLocalMutableMap(partitionId)

    for ((state, value) in candidates[partitionId].entries()) {
        var witness = value
        for ((successor, dir, bound) in state.successors(timeFlow)) {
            if (!witness.canSat()) break    //fail fast

            if (!direction.eval(dir)) {
                witness = witness and bound.not()
            } else {
                //either the transition is not there (bound.not()) or successor is satisfied
                witness = witness and (mySatisfied[successor] or bound.not())
            }
        }
        if (witness.canSat()) result.setOrUnion(state, witness)
    }

    result

})