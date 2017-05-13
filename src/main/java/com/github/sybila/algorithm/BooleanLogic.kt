package com.github.sybila.algorithm

import com.github.sybila.model.decreasingStateMap
import com.github.sybila.model.increasingStateMap
import com.github.sybila.reactive.ParallelConcatCollect
import com.github.sybila.solver.Solver
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler

interface BooleanLogic<P : Any> {

    val solver: Solver<P>
    val stateCount: Int
    val scheduler: Scheduler

    fun Result<P>.complement(against: Result<P>? = null): Result<P> {
        return ParallelConcatCollect(
                makeState = {
                    against?.block()?.toDecreasing() ?: solver.decreasingStateMap(stateCount)
                },
                makeFlux = { Flux.just(this.entriesParallel(scheduler)) },
                collect = { map, (state, params) ->
                    // result = universe and current.not()
                    solver.run { map.decreaseKey(state, params.not()) }
                }
        ).map { it }
    }

    fun Result<P>.disjunction(other: Result<P>): Result<P> {
        return ParallelConcatCollect(
                makeState = { solver.increasingStateMap(stateCount) },
                makeFlux = { Flux.just(this.entriesParallel(scheduler), other.entriesParallel(scheduler)) },
                collect = { map, (state, params) ->
                   solver.run { map.increaseKey(state, params) }
                }
        ).map { it }
    }

    fun Result<P>.conjunction(other: Result<P>): Result<P> {
        return ParallelConcatCollect(
                makeState = { solver.decreasingStateMap(stateCount) },
                makeFlux = { Flux.just(this.entriesParallel(scheduler), other.entriesParallel(scheduler)) },
                collect = { map, (state, params) ->
                    solver.run { map.decreaseKey(state, params) }
                }
        ).map { it }
    }

}

