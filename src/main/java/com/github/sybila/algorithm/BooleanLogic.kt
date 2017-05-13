package com.github.sybila.algorithm

import com.github.sybila.model.IncreasingStateMap
import com.github.sybila.model.StateMap
import com.github.sybila.solver.SetSolver
import com.github.sybila.solver.Solver
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/*
typealias I<P> = Pair<Int, P>

interface BooleanLogic<P : Any> {

    val solver: Solver<P>

    val size: Int

    fun Mono<StateMap<P>>.complement(): Mono<StateMap<P>> {
        return ParallelCollector<Pair<State, P>, StateMap<P>, IncreasingStateMap<P>>(
                sourceProvider = Flux.from(this).flatMap { Flux.fromIterable(it.entries) },
                openState = { IncreasingStateMap(size, solver, increasing = false) },
                closeState = { it },
                join = { map, (state, params) ->
                    solver.run { params.not()?.let { p ->
                        map.decreaseKey(state, p)
                    } }
                }
        )
    }

    fun Mono<StateMap<P>>.conjunction(other: Mono<StateMap<P>>): Mono<StateMap<P>> {
        return ParallelCollector<Pair<State, P>, StateMap<P>, IncreasingStateMap<P>>(
                sourceProvider = Flux.merge(this, other).flatMap { Flux.fromIterable(it.entries) },
                openState = { IncreasingStateMap(size, solver, increasing = false) },
                closeState = { it },
                join = { map, (state, params) -> map.decreaseKey(state, params) }
        )
    }

    fun Mono<StateMap<P>>.disjunction(other: Mono<StateMap<P>>): Mono<StateMap<P>> {
        return ParallelCollector<Pair<State, P>, StateMap<P>, IncreasingStateMap<P>>(
                sourceProvider = Flux.merge(this, other).flatMap { Flux.fromIterable(it.entries) },
                openState = { IncreasingStateMap(size, solver, increasing = true) },
                closeState = { it },
                join = { map, (state, params) -> map.increaseKey(state, params) }
        )
    }

}

interface FirstOrder<P : Any> {

    val solver: Solver<P>

    val size: Int

    fun Flux<StateMap<P>>.exists(): Mono<StateMap<P>> {
        return ParallelCollector<Pair<State, P>, StateMap<P>, IncreasingStateMap<P>>(
                sourceProvider = Flux.merge(this.map { Flux.fromIterable(it.entries) }, 4),
                openState = { IncreasingStateMap(size, solver, increasing = true) },
                closeState = { it },
                join = { map, (state, params) ->
                    map.increaseKey(state, params)
                }
        )
    }

}

fun main(args: Array<String>) {
    val solver = SetSolver((0..1000).toSet())
    val a = (0..700).toSet()
    val b = (300..1000).toSet()

    val states = 100
    val leftA = Array(size = states) { a } as Array<Any?>
    val rightA = Array(size = states) { b } as Array<Any?>
    val left = IncreasingStateMap<Set<Int>>(leftA, solver, true)
    val right = IncreasingStateMap<Set<Int>>(rightA, solver, true)

    val logic = object : BooleanLogic<Set<Int>> {
        override val solver: Solver<Set<Int>> = solver
        override val size: Int = states
    }
    val start = System.currentTimeMillis()
    logic.run {
        val l: Mono<StateMap<Set<Int>>> = Mono.just(left)
        val r: Mono<StateMap<Set<Int>>> = Mono.just(right)
        l.conjunction(r).block()
        println("Got result after ${System.currentTimeMillis() - start}")
    }
}


class ParallelCollector<I, R, S : Any>(
        private val sourceProvider: Flux<I>,
        private val openState: () -> S,
        private val closeState: (S) -> R,
        private val join: (S, I) -> Unit
) : Mono<R>() {

    override fun subscribe(s: Subscriber<in R>) {
        s.onSubscribe(object : Subscription {

            private var requested = 0L
            private var state: S = openState()

            init {
                sourceProvider.parallel()
                        .runOn(Schedulers.parallel())
                        .subscribe({
                            join(state, it)
                        }, { s.onError(it) }, {
                            s.onNext(closeState(state))
                            s.onComplete()
                        })
            }

            override fun cancel() {
                // not supported
            }

            override fun request(n: Long) {
                if (requested == Long.MAX_VALUE) requested = 0
                if (n == Long.MAX_VALUE) requested = n
                else requested += n
            }

        })
    }

}
/*
class ParallelFixedPointCollector<P, R, S : Any>(
        private val sourceProvider: Flux<Pair<Int, P>>,
        private val openState: () -> S,
        private val closeState: (S) -> R,
        private val iteration: (S, Pair<Int, P>) -> Flux<Int>
) : Mono<R>() {

    override fun subscribe(s: Subscriber<in R>?) {
        sourceProvider.parallel().flatMap { iteration(x, it) }
        val queue = TierStateQueue(size)
        Flux.concat(Flux.fromIterable(queue))
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}*/*/