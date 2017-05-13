package com.github.sybila.reactive

import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux
import java.util.concurrent.atomic.AtomicIntegerArray

/*

This thing here requires a thread safe S and

class ParallelCollect<I, S>(
        private val sourceFlux: ParallelFlux<I>,
        private val makeState: () -> S,
        private val collect: (S, I) -> Unit
) : Mono<S>() {

    override fun subscribe(s: Subscriber<in S>) {
        s.onSubscribe(object : Subscription {

            // this is a Mono, so we don't have to keep number of requests
            private val requested = AtomicBoolean(false)
            private val state = makeState()

            override fun cancel() {
                // sadly, there is no way to stop parallel flux now :/
            }

            override fun request(n: Long) {
                if (n > 0 && requested.compareAndSet(false, true)) {
                    start()
                }
            }

            private fun start() {
                sourceFlux.subscribe({ item ->
                    collect(state, item)
                }, { error ->
                    s.onError(error)
                }, {
                    // we get here only by setting requested to true, so no need to check that
                    s.onNext(state)
                    s.onComplete()
                })
            }

        })
    }

}
*/