package com.github.sybila.algorithm

import com.github.sybila.model.ArrayStateMap
import com.github.sybila.model.decreasingStateMap
import com.github.sybila.model.increasingStateMap
import com.github.sybila.reactive.ParallelConcatCollect
import com.github.sybila.solver.Solver
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

interface HybridLogic<P : Any> {

    val solver: Solver<P>
    val stateCount: Int
    val fork: Int
    val scheduler: Scheduler

    fun exists(states: ResultFlux<P>): Result<P> {
        return ParallelConcatCollect(
                makeState = { solver.increasingStateMap(stateCount) },
                makeFlux = { Flux.merge(states, fork).map { it.second.entriesParallel(scheduler) } },
                collect = { map, (state, params) ->
                    map.increaseKey(state, params)
                }
        ).map { it }
    }

    fun forall(states: ResultFlux<P>): Result<P> {
        return ParallelConcatCollect(
                makeState = { solver.decreasingStateMap(stateCount) },
                makeFlux = { Flux.merge(states, fork).map { it.second.entriesParallel(scheduler) } },
                collect = { map, (state, params) ->
                    map.decreaseKey(state, params)
                }
        ).map { it }
    }

    fun bind(states: ResultFlux<P>, extraSched: Scheduler): Result<P> {
        return ParallelConcatCollect(
                makeState = { solver.increasingStateMap(stateCount) },
                makeFlux = {
                    Flux.just(states.parallel().runOn(extraSched).map { it.block() }
                            .sequential()
                            .map { (state, map) -> state to map[state] }
                            .parallel()
                    )
                    /*Flux.just(Flux.merge(states.parallel().runOn(scheduler), fork).map { (state, map) ->
                        state to map[state]
                    }.parallel())*/
                },
                collect = { map, (state, params) ->
                    map.increaseKey(state, params)
                }
        ).map { it }
    }

    fun at(state: Int, inner: Result<P>): Result<P> {
        return inner.map { map ->
            val value = map[state]
            ArrayStateMap(stateCount, value, solver)
        }
    }

}