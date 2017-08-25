package com.github.sybila.algorithm

import com.github.sybila.collection.Counter
import com.github.sybila.collection.StateMap
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async

interface Components<S: Any, P: Any> : Reachability<S, P>, BooleanLogic<S, P> {

    fun makeTerminalComponents(): Deferred<Counter<P>> = makeDeferred {
        val counter = Counter(solver)
        branch(universe, counter).join()
        counter
    }

    private fun selectPivots(universe: StateMap<S, P>): StateMap<S, P> {
        solver.run {
            val pivots = makeEmptyMap()
            var uncovered = universe.entries.map { it.second }
                    .fold<P?, P?>(null) { uncovered, p -> uncovered or p }
            for ((s, p) in universe.entries) {
                (p and uncovered)?.takeIfNotEmpty()?.let { pivot ->
                    pivots.lazySet(s, pivot)
                    uncovered = (pivot complement uncovered)?.takeIfNotEmpty()
                }
                if (uncovered == null) break
            }
            return pivots
        }
    }

    private fun branch(universe: StateMap<S, P>, counter: Counter<P>): Job = async(executor) {
        fun StateMap<S, P>.allParams() = solver.run {
            entries.map { it.second }.fold<P?, P?>(null) { a, b -> a or b }
        }
        val pivots = selectPivots(universe)
        println("Pivots: ${pivots.states.toList()}")

        val F = FWD(makeDeferred { pivots })
        val B = BWD(makeDeferred { pivots })

        val F_minus_B = makeComplement(B, F).await()

        val j1 = F_minus_B.takeIf { it.entries.any() }?.let { next ->
            branch(next, counter)
        }

        val BB = BWD(F)
        val V_minus_BB = makeComplement(BB, makeDeferred { universe }).await()

        val j2 = solver.run {
            V_minus_BB.allParams()?.takeIfNotEmpty()?.let { newComponents ->
                //println("Increment! ${newComponents}")
                counter.increment(newComponents)
                branch(V_minus_BB, counter)
            }
        }

        j1?.join()
        j2?.join()
    }

    private fun FWD(from: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = makeReachability(from, false)
    private fun BWD(from: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = makeReachability(from, true)

}