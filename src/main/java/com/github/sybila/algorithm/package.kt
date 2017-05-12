package com.github.sybila.algorithm

import com.github.sybila.model.MutableStateMap
import com.github.sybila.model.StateMap
import com.github.sybila.model.TransitionSystem
import com.github.sybila.solver.Solver
import java.io.Closeable
import java.util.concurrent.Executors

typealias State = Int

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
            iteration: FixedPointAlgorithm<Param>.(MutableStateMap<Param>, State) -> Iterable<State>
    ): StateMap<Param> {

        val result = MutableStateMap(stateCount, this)
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


}