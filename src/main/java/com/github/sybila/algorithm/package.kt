package com.github.sybila.algorithm

import com.github.sybila.model.StateMap
import org.intellij.lang.annotations.Flow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Scheduler


typealias Result<P> = Mono<StateMap<Int, P>>
typealias ResultFlux<P> = Flux<Mono<Pair<Int, StateMap<Int, P>>>>

typealias State = Int

fun <S : Any, P : Any> Mono<StateMap<S, P>>.entriesParallel(scheduler: Scheduler): ParallelFlux<Pair<S, P>>
        = this.flatMapMany { Flux.fromIterable(it.entries) }.parallel().runOn(scheduler)

fun <S : Any, P : Any> StateMap<S, P>.entriesParallel(scheduler: Scheduler): ParallelFlux<Pair<S, P>>
        = Flux.fromIterable(this.entries).parallel().runOn(scheduler)

/*
class FixedPointAlgorithm<Param : Any>(parallelism: Int,
                                           transitionSystem: TransitionSystem<Param>,
                                           solver: Solver<Param>
) : Closeable, TransitionSystem<Param> by transitionSystem, Solver<Param> by solver {

    private val executor = Executors.newFixedThreadPool(parallelism)

    override fun close() {
        executor.shutdown()
    }

    fun fixedPoint(
            initial: StateMap<Param>,
            iteration: FixedPointAlgorithm<Param>.(IncreasingStateMap<Param>, State) -> Iterable<State>
    ): StateMap<Param> {

        val result = IncreasingStateMap(stateCount, this, increasing = true)
        val queue = TierStateQueue(stateCount)

        // this could be parallel, but it is rather trivial, so we probably don't need it
        initial.entries.forEach { (state, value) ->
            result.increaseKey(state, value)
            queue.add(state, null)
        }

        while (queue.isNotEmpty()) {
            queue.remove().map { state ->
                state to executor.submit<Iterable<State>> { iteration(result, state) }
            }.forEach { (source, future) ->
                future.get().forEach { queue.add(it, source) }
            }
        }

        return result
    }


}*/