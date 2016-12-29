package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator
import com.github.sybila.checker.StateMap
import com.github.sybila.checker.eval
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.huctl.DirectionFormula
import java.util.*

private fun <Params : Any> List<StateMap<Params>>.prepareTransmission(partitionId: Int): Array<List<Pair<Int, Params>>?>
        = this  .mapIndexed { i, map -> if (i == partitionId) null else map.entries().asSequence().toList() }
                .map { if (it?.isEmpty() ?: true) null else it }.toTypedArray()

private fun <Params : Any> List<StateMap<Params>>.prepareFilteredTransmission(
        partitionId: Int, include: Set<Int>
): Array<List<Pair<Int, Params>>?>
        = this  .mapIndexed { i, map ->
                    if (i == partitionId) null else map.entries().asSequence().filter { it.first in include }.toList()
                }
                .map { if (it?.isEmpty() ?: true) null else it }.toTypedArray()

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

//EU works based on value notification - what is sent is valid and should be saved.
class ExistsUntilOperator<out Params : Any>(
        timeFlow: Boolean, direction: DirectionFormula,
        pathOp: Operator<Params>?, reach: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val path = pathOp?.compute()

    val storage = (0 until partitionCount).map { newLocalMutableMap(it) }
    val result = storage[partitionId]

    val recompute = HashSet<Int>()
    val send = HashSet<Int>()

    //load local data
    for ((state, value) in reach.compute().entries()) {
        result.setOrUnion(state, value)
        recompute.add(state)
    }

    var received: List<Pair<Int, Params>>? = null

    do {
        received?.forEach {
            val (state, value) = it
            val withPath = if (path != null) value and path[state] else value
            if (withPath.canSat() && result.setOrUnion(state, withPath)) {
                recompute.add(it.first)
            }
        }

        do {
            val iteration = recompute.toList()
            recompute.clear()
            for (state in iteration) {
                val value = result[state]
                for ((predecessor, dir, bound) in state.predecessors(timeFlow)) {
                    if (direction.eval(dir)) {
                        val owner = predecessor.owner()
                        if (owner == partitionId) {   //also consider path
                            val witness = if (path != null) {
                                value and bound and path[predecessor]
                            } else value and bound
                            if (witness.canSat() && result.setOrUnion(predecessor, witness)) {
                                recompute.add(predecessor)
                            }
                        } else {    //path will be handled by receiver
                            val witness = value and bound
                            if (witness.canSat() && storage[owner].setOrUnion(predecessor, witness)) {
                                send.add(predecessor)
                            }
                        }
                    }
                }
            }
        } while (recompute.isNotEmpty())
        //all local computation is done - exchange info with other workers!

        received = mapReduce(storage.prepareFilteredTransmission(partitionId, send))
        send.clear()
    } while (received != null)

    result

})

//AU works based on dependency notification - when something increases, it is propagated to dependencies
class AllUntilOperator<out Params : Any>(
        timeFlow: Boolean, direction: DirectionFormula,
        pathOp: Operator<Params>?, reach: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val path = pathOp?.compute()

    val satisfied = (0 until partitionCount).map { HashStateMap(ff) }
    val result = newLocalMutableMap(partitionId)

    val candidates = HashSet<Int>()
    val send = HashSet<Int>()

    fun Int.notifyCandidates(witness: Params) {
        for ((predecessor, dir, bound) in this.predecessors(timeFlow)) {
            if (direction.eval(dir)) { //if direction is false, the whole bound will be false in predecessor
                val owner = predecessor.owner()
                if (owner == partitionId) {
                    candidates.add(predecessor)
                } else {
                    val candidate = witness and bound //predecessor only cares about what he sees after transition
                    if (candidate.canSat() && satisfied[owner].setOrUnion(this, candidate)) {
                        send.add(this)
                    }
                }
            }
        }
    }

    for ((state, value) in reach.compute().entries()) {
        result.setOrUnion(state, value)
        satisfied[partitionId].setOrUnion(state, value)
        state.notifyCandidates(value)
    }

    var received: List<Pair<Int, Params>>? = null

    do {
        received?.forEach {
            val (state, value) = it
            if (satisfied[partitionId].setOrUnion(state, value)) {
                state.predecessors(timeFlow).forEach {
                    if (it.target in this) candidates.add(it.target)
                }
            }
        }

        do {
            val iteration = candidates.toList()
            candidates.clear()
            for (state in iteration) {
                var witness = if (path != null) path[state] else tt
                for ((successor, dir, bound) in state.successors(timeFlow)) {
                    if (!witness.canSat()) break

                    if (!direction.eval(dir)) {
                        witness = witness and bound.not()
                    } else {
                        witness = witness and (satisfied[partitionId][successor] or bound.not())
                    }
                }
                if (witness.canSat()) {
                    if (result.setOrUnion(state, witness)) {
                        satisfied[partitionId][state] = result[state]
                        state.notifyCandidates(witness)
                    }
                }
            }
        } while (candidates.isNotEmpty())
        //all local computation is done - exchange info with other workers!

        //println("$partitionId: $send ${satisfied.map { it.sizeHint.toString() + ": " + it.prettyPrint() }}")
        received = mapReduce(satisfied.prepareFilteredTransmission(partitionId, send))
        send.clear()
    } while (received != null)

    result

})