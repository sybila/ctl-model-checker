package com.github.sybila.algorithm

import com.github.sybila.algorithm.components.PivotSelector
import com.github.sybila.collection.Counter
import com.github.sybila.collection.StateMap
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import java.util.concurrent.atomic.AtomicInteger

val c = AtomicInteger(0)

interface Components<S: Any, P: Any> : Reachability<S, P>, BooleanLogic<S, P>, PivotSelector<S, P> {

    fun makeTerminalComponents(): Deferred<Counter<P>> = makeDeferred {
        val counter = Counter(solver)
        branch(universe, counter).join()
        counter
    }

    private fun branch(universe: StateMap<S, P>, counter: Counter<P>): Job = async(executor) {

        fun StateMap<S, P>.allParams() = solver.run {
            entries.map { it.second }.fold<P?, P?>(null) { a, b -> a or b }
        }

        val pivots = universe.findPivots()
        println("Iteration: ${c.incrementAndGet()} pivots: ${pivots.states.count()}")

        val F = FWD(makeDeferred { pivots }, makeDeferred { universe })
        val B = BWD(makeDeferred { pivots }, F)

        val BB = BWD(F, makeDeferred { universe })
        val V_minus_BB = makeComplement(BB, makeDeferred { universe }).await()

        val F_minus_B = makeComplement(B, F).await()

        val j2 = solver.run {
            V_minus_BB.allParams()?.takeIfNotEmpty()?.let { newComponents ->
                //println("Increment! ${newComponents}")
                counter.increment(newComponents)
                branch(V_minus_BB, counter)
            }
        }

        val j1 = F_minus_B.takeIf { it.entries.any() }?.let { next ->
            branch(next, counter)
        }

        j1?.join()
        j2?.join()
    }

    fun FWD(from: Deferred<StateMap<S, P>>, through: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = makeReachability(from, through, false)
    fun BWD(from: Deferred<StateMap<S, P>>, through: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = makeReachability(from, through,true)

}