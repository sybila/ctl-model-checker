package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator
import com.github.sybila.checker.eval
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.huctl.DirectionFormula
import java.util.*

//EU works based on value notification - what is sent is valid and should be saved.
class ExistsUntilOperator<out Params : Any>(
        timeFlow: Boolean, direction: DirectionFormula, weak: Boolean = false,
        pathOp: Operator<Params>?, reach: Operator<Params>, partition: Channel<Params>
) : LazyOperator<Params>(partition, {

    val path = pathOp?.compute()

    val storage = (0 until partitionCount).map { newLocalMutableMap(it) }
    val result = storage[partitionId]

    val recompute = HashSet<Int>()
    val send = HashSet<Int>()

    //load local data
    if (!weak) {
        for ((state, value) in reach.compute().entries()) {
            result.setOrUnion(state, value)
            recompute.add(state)
        }
    } else {
        val r = reach.compute()
        (0 until stateCount).filter { it in this }.forEach { state ->
            val existsWrongDirection = state.successors(timeFlow).asSequence().fold(ff) { a, t ->
                if (!direction.eval(t.direction)) a or t.bound else a
            }
            val value = r[state] or existsWrongDirection
            if (value.canSat() && result.setOrUnion(state, value)) {
                recompute.add(state)
            }
        }
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
        timeFlow: Boolean, direction: DirectionFormula, weak: Boolean = false,
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

    if (!weak) {
        for ((state, value) in reach.compute().entries()) {
            result.setOrUnion(state, value)
            satisfied[partitionId][state] = result[state]
            state.notifyCandidates(value)
        }
    } else {
        val r = reach.compute()
        (0 until stateCount).filter { it in this }.forEach { state ->
            val existsValidDirection = state.successors(timeFlow).asSequence().fold(ff) { a, t ->
                if (direction.eval(t.direction)) a or t.bound else a
            }
            val value = r[state] or existsValidDirection.not()  //proposition or deadlock
            if (value.canSat() && result.setOrUnion(state, value)) {
                satisfied[partitionId][state] = result[state]
                state.notifyCandidates(value)
            }
        }
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

        received = mapReduce(satisfied.prepareFilteredTransmission(partitionId, send))
        send.clear()
    } while (received != null)

    result

})