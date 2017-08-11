package com.github.sybila

import com.github.sybila.algorithm.TierStateQueue
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.PathQuantifier
import com.github.sybila.model.TransitionSystem
import io.reactivex.processors.BehaviorProcessor
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
/*
    val fastDelays = listOf(1,2,4,8,4,2,1).map { it * 100L }
    val slowDelays = (1..2).map { 1000L }

    val duration = measureTimeMillis {
        val smallFlows: Flowable<Flowable<Runnable>> = Flowable.fromArray(*Array(80) { i ->
            val subj = BehaviorProcessor.create<Runnable>()
            subj.onNext(subj.nextRun(fastDelays))
            subj
        })

        val p = BehaviorProcessor.create<Runnable>()
        p.onNext(p.nextRun(slowDelays))

        val flows: Flowable<Flowable<Runnable>> = Flowable.just(p as Flowable<Runnable>).concatWith(smallFlows)

        val par = 16

        val e = Executors.newFixedThreadPool(par)

        //flows.parallel(par).runOn(Schedulers.computation()).flatMap { it }.sequential()
                Flowable.merge(flows, par)
                .subscribe(object : FlowableSubscriber<Runnable> {

            private lateinit var sub: Subscription

            override fun onError(t: Throwable) {
                throw t
            }

            override fun onComplete() {
                println("Done")
                synchronized(e) {
                    (e as java.lang.Object).notify()
                }
            }

            override fun onNext(t: Runnable) {
                //println("On next")
                e.execute {
                    t.run()
                    sub.request(1)
                }
            }

            override fun onSubscribe(s: Subscription) {
                sub = s
                s.request(par.toLong())
            }

        })

        synchronized(e) {
            (e as java.lang.Object).wait()
        }

        e.shutdown()
    }

    println("Duration: $duration")*/

    val par = 4
    val context = newFixedThreadPoolContext(par, "work")

    val duration = measureTimeMillis {
        runBlocking {
            val l1 = async(context) {
                val items = (1..10_000).toList()
                val chunkSize = AtomicInteger(1)
                val chunks = produce<IntRange>(context) {
                    var start = 0
                    while (start != items.size) {
                        val size = chunkSize.get()
                        println("Make chunk $size")
                        val end = Math.min(items.size, start + size) - 1
                        send(start..end)
                        start = end + 1
                    }
                    close()
                }

                (1..par).map {
                    async(context) {
                        chunks.consumeEach { range ->
                            println("Thread: ${Thread.currentThread()}")
                            val elapsed = measureTimeMillis {
                                Thread.sleep((range.last - range.first + 1).toLong())
                            }
                            val size = (range.last - range.first) + 1
                            if (elapsed < 500) {
                                System.out.println("Update $size to ${size * 2} because elapsed $elapsed")
                                chunkSize.compareAndSet(size, size * 2)
                            }
                            if (elapsed > 1000) {
                                System.out.println("Update $size to ${size / 2} because elapsed $elapsed")
                                chunkSize.compareAndSet(size, size / 2)
                            }
                        }
                    }
                }.forEach { it.await() }
            }
            val l2 = async(context) {
                val items = (1..10_000).toList()
                val chunkSize = AtomicInteger(1)
                val chunks = produce<IntRange>(context) {
                    var start = 0
                    while (start != items.size) {
                        val size = chunkSize.get()
                        println("Make chunk $size")
                        val end = Math.min(items.size, start + size) - 1
                        send(start..end)
                        start = end + 1
                    }
                    close()
                }

                (1..par).map {
                    async(context) {
                        chunks.consumeEach { range ->
                            println("Thread: ${Thread.currentThread()}")
                            val elapsed = measureTimeMillis {
                                Thread.sleep((range.last - range.first + 1).toLong())
                            }
                            val size = (range.last - range.first) + 1
                            if (elapsed < 500) {
                                System.out.println("Update $size to ${size * 2} because elapsed $elapsed")
                                chunkSize.compareAndSet(size, size * 2)
                            }
                            if (elapsed > 1000) {
                                System.out.println("Update $size to ${size / 2} because elapsed $elapsed")
                                chunkSize.compareAndSet(size, size / 2)
                            }
                        }
                    }
                }.forEach { it.await() }
            }
            l1.await()
            l2.await()
        }
    }

    println("Duration: $duration")

}

private fun BehaviorProcessor<Runnable>.nextRun(list: List<Long>): Runnable = Runnable {
    println("Sleep for ${list.first()} on ${Thread.currentThread()} on ${this.hashCode()}")
    Thread.sleep(list.first())
    val rest = list.drop(1)
    if (rest.isEmpty()) this.onComplete()
    else this.onNext(this.nextRun(rest))
}

/*

Processing architecture:

DAG of dependent operators.

Each operator holds a result map together with a

 */

class ModelChecker<State : Any, out Param : Any>(
        val parallelism: Int = 1, val name: String = "MC",
        val system: TransitionSystem<Param>
) {

    private val executor = newFixedThreadPoolContext(parallelism, name)

    private fun Formula.Until.toJob(): Deferred<Unit> = lazyAsync {
        val until = this@toJob
        if (until.quantifier != PathQuantifier.E) TODO("Not implemented")

        val reach = until.reach.toJob()
        val path = until.path.toJob()

        reach.start(); path.start()
        reach.await(); path.await()

        val queue = TierStateQueue(system.stateCount)

        val chunkSize = AtomicInteger(10)
        val tier = queue.remove().toList()
        val chunks = produce<IntRange>(executor) {
            var start = 0
            while (start != tier.size) {
                val size = chunkSize.get()
                val end = Math.min(tier.size, start + size) - 1
                send(start..end)
                start = end + 1
            }
            close()
        }
        repeat(parallelism) {
            chunks.channel.consumeEach { range ->
                for (i in range) {

                }
                // update chunkSize
            }
        }

        Unit

    }

    private fun Formula.toJob(): Deferred<Unit> {
        return when (this) {
            is Formula.Until -> this.toJob()
            else -> TODO("Not implemented")
        }
    }

    private fun <T> lazyAsync(block: suspend CoroutineScope.() -> T) = async(executor, CoroutineStart.LAZY, block)


}