package com.github.sybila.algorithm

import com.github.sybila.model.IncreasingStateMap
import com.github.sybila.model.TransitionSystem
import com.github.sybila.model.increasingStateMap
import com.github.sybila.reactive.ParallelConcatCollect
import com.github.sybila.solver.Solver
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler

interface TemporalLogic<P : Any> : TransitionSystem<P> {

    //override val stateCount: Int
    val solver: Solver<P>
    val scheduler: Scheduler

    fun existsFinally(finally: Result<P>, time: Boolean = true): Result<P> {
        solver.run {
            return ParallelConcatCollect<Int, Pair<TierStateQueue, IncreasingStateMap<Int, P>>, Pair<Int, Iterable<Int>>>(
                    makeState = { TierStateQueue(stateCount) to solver.increasingStateMap(stateCount) },
                    makeFlux = { (queue, map) ->
                        finally.block().entries.forEach { (s, p) ->
                            if (map.increaseKey(s, p)) {
                                s.step(!time).forEach { (pred, _) ->
                                    queue.add(pred, null)
                                }
                            }
                        }
                        Flux.fromIterable(queue).map {
                            Flux.fromIterable(it).parallel().runOn(scheduler)
                        }
                    },
                    collect = { (_, map), state ->
                        val witness = state.step(time).fold(ff) { witness, (succ, bound) ->
                            witness or (map[succ] and bound)
                        }
                        state to if (map.increaseKey(state, witness)) state.step(!time).map { it.first }
                        else listOf()
                    },
                    restart = { (queue, _), (from, states) ->
                        states.forEach { queue.add(it, from) }
                    }
            ).map { it.second }
        }
    }

}