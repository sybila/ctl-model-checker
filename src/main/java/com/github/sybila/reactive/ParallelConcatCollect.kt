package com.github.sybila.reactive

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The idea behind this operator is very simple: You have a data structure
 * which is thread safe and you want to collect a stream of parallel
 * fluxes into this structure (the stream can recursively depend
 * on the structure itself). Each of these streams is processed
 * in parallel, but overall, they are processed sequentially
 * (so that stream N+1 can depend on the state of the structure
 * when stream N finished).
 *
 * Another option would be that your data structure is not entirely thread
 * safe, but is safe when only distinct values are updated. In that case,
 * you can split your flux into sequence of fluxes of distinct values
 * and execute them sequentially with each stream being processed in parallel.
 */
class ParallelConcatCollect<I, S, R>(
        private val makeState: () -> S,
        private val makeFlux: (S) -> Flux<ParallelFlux<I>>,
        private val collect: (S, I) -> R,
        private val restart: (S, R) -> Unit = { _, _ -> Unit }
) : Mono<S>() {

    override fun subscribe(s: Subscriber<in S>) {
        s.onSubscribe(object : Subscription {

            private val requested = AtomicBoolean(false)
            private val state = makeState()

            override fun cancel() {
                // parallel flux currently cannot be canceled
            }

            override fun request(n: Long) {
                if (n > 0 && requested.compareAndSet(false, true)) {
                    start()
                }
            }

            private fun start() {
                makeFlux(state).concatMap {
                    it.map { collect(state, it) }
                }.subscribe({
                     restart(state, it)
                }, { error ->
                    s.onError(error)
                }, {
                    s.onNext(state)
                    s.onComplete()
                })
            }

        })
    }

}