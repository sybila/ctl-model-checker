package com.github.sybila.checker.operator

import com.github.sybila.checker.Channel
import com.github.sybila.checker.Operator
import com.github.sybila.checker.map.mutable.HashStateMap

class ForAllOperator<out Params : Any>(
        full: Operator<Params>,
        inner: MutableList<Operator<Params>?>, bound: Operator<Params>, channel: Channel<Params>
) : LazyOperator<Params>(channel, {

    //forall x in B: A <=> forall x: ((at x: B) => A) <=> forall x: (!(at x: B) || A)

    val b = HashStateMap(ff, bound.compute().entries().asSequence().toMap())
    val boundTransmission = (0 until partitionCount).map { b }
    mapReduce(boundTransmission.prepareTransmission(partitionId))?.forEach {
        b[it.first] = it.second
    }
    //now everyone has a full bound

    val result = newLocalMutableMap(partitionId)
    full.compute().entries().forEach { result[it.first] = it.second }
    for (state in 0 until stateCount) {
        if (state in b) {
            val i = inner[state]!!.compute()
            println("I: $state -> ${i.prettyPrint()}")
            result.states().forEach {
                result[it] = result[it] and (i[it] or b[state].not())
            }
        }
        inner[state] = null
    }

    result
})

class ExistsOperator<out Params : Any>(
        inner: MutableList<Operator<Params>?>, bound: Operator<Params>, channel: Channel<Params>
) : LazyOperator<Params>(channel, {

    //exists x in B: A <=> exists x: ((at x: B) && A)

    val result = newLocalMutableMap(partitionId)

    val b = HashStateMap(ff, bound.compute().entries().asSequence().toMap())
    val boundTransmission = (0 until partitionCount).map { b }
    mapReduce(boundTransmission.prepareTransmission(partitionId))?.forEach {
        b[it.first] = it.second
    }
    //now everyone has a full bound
    for (state in 0 until stateCount) {
        if (state in b) {
            val i = inner[state]!!.compute()
            i.entries().forEach {
                result[it.first] = result[it.first] or (it.second and b[state])
            }
        }
        inner[state] = null
    }

    result
})

class BindOperator<out Params : Any>(
        inner: MutableList<Operator<Params>?>, channel: Channel<Params>
) : LazyOperator<Params>(channel, {

    val result = newLocalMutableMap(partitionId)

    for (state in 0 until stateCount) {
        val i = inner[state]!!.compute()

        if (state in this) {
            result[state] = i[state]
        }
        inner[state] = null
    }

    result
})

class AtOperator<out Params : Any>(
    state: Int,
    inner: Operator<Params>, channel: Channel<Params>
) : LazyOperator<Params>(channel, {
    val i = inner.compute()
    val transmission: Array<List<Pair<Int, Params>>?> = if (state in this) {
        (0 until partitionCount).map { listOf(state to i[state]) }.toTypedArray()
    } else kotlin.arrayOfNulls<List<Pair<Int, Params>>>(partitionCount)

    val result = mapReduce(transmission)?.first() ?: (state to i[state])

    (0 until stateCount).asStateMap(result.second)
})