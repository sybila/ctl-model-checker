package com.github.sybila.algorithm

import com.github.sybila.model.MutableStateMap
import com.github.sybila.model.StateMap
import com.github.sybila.solver.Solver
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean


typealias I<P> = Pair<Int, P>

interface BooleanLogic<P : Any> {

    val solver: Solver<P>

    val executor: ExecutorService

    val size: Int
/*
    fun Flux<I<P>>.conjunction(other: Flux<I<P>>) = this.mergeWith(other).parallel()
            .reduce()

    fun Mono<StateMap<P>>.conjunction(other: Mono<StateMap<P>>) = this
            .flatMapMany { Flux.fromIterable(it.entries) }
            .mergeWith(other.flatMapMany { Flux.fromIterable(it.entries) })
            .withLatestFrom<StateMap<P>, Pair<Entry<P>, StateMap<P>>>(other, BiFunction { t, u -> t to u })
            .parallel()
            .map { (entry, B) ->
                val (state, a) = entry
                state to solver.run { B[state]?.let { b -> a and b } }
            }
            .runOn(Schedulers.parallel())

*/



    fun <Param : Any> computeAnd(left: StateMap<Param>,
                                 right: StateMap<Param>,
                                 solver: Solver<Param>
    ): StateMap<Param> = solver.run {
        val (A, B) = if (left.states.count() < right.states.count()) left to right else right to left

        // if both maps are empty, just return an empty map
        val size = A.states.max() ?: return MutableStateMap(0, solver)

        val result = MutableStateMap(size, solver)
        A.entries.map { (state, a) ->
            B[state]?.let { b ->
                executor.submit {
                    (a and b)?.let { c -> result.increaseKey(state, c) }
                }
            }
        }.forEach { it?.get() }

        result
    }

    fun <Param : Any> computeOr(left: StateMap<Param>,
                                right: StateMap<Param>,
                                solver: Solver<Param>
    ) : StateMap<Param> = solver.run {
        val (A, B) = left to right
        val size = Math.max(A.states.max() ?: 0, B.states.max() ?: 0)
        val result = MutableStateMap(size, solver)

        if (size == 0) return result

        /*
            Following can happen: state is in both A and B (1), state is in A (2), state is in B (3)
         */

        // handle (1) concurrently and (2) sequentially
        A.entries.map { (state, a) ->
            if (state !in B) {
                result.increaseKey(state, a); null
            } else {
                B[state]?.let { b ->
                    executor.submit {
                        (a or b)?.let { c -> result.increaseKey(state, c) }
                    }
                }
            }
        }.forEach { it?.get() }

        // handle (3) sequentially
        B.entries.forEach { (state, b) ->
            if (state !in left) result.increaseKey(state, b)
        }

        result
    }
/*
    fun <Param : Any> computeComplement(map: StateMap<Param>,
                                        against: StateMap<Param>,
                                        solver: Solver<Param>
    ) : StateMap<Param> = solver.run {

    }*/

}

fun main(args: Array<String>) {

    Flux.fromIterable((0..100).toList())
            .parallel()
            .runOn(Schedulers.parallel())
            .subscribe({ println("Executed ${Thread.currentThread()}") })

}

class ParallelCollect<T, S : Any>(
        private val source: ParallelFlux<T>,
        private val makeState: () -> S,
        private val mergeState: (S, T) -> T?
) : Flux<T>() {

    private val subscribed = AtomicBoolean(false)
    private lateinit var state: S

    override fun subscribe(s: Subscriber<in T>) {

        source.subscribe()

        s.onSubscribe(object : Subscription {
            override fun cancel() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun request(n: Long) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })


    }


    private fun ensureSubscribed() {
        if (subscribed.compareAndSet(false, true)) {
            state = makeState()
            source.subscribe({

            }, {

            }, {

            })
        }
    }

}

class Foo : ParallelFlux<Unit>() {

    override fun subscribe(subscribers: Array<out Subscriber<in Unit>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun parallelism(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}