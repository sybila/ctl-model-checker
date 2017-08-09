package com.github.sybila

import io.reactivex.Flowable
import io.reactivex.FlowableSubscriber
import io.reactivex.processors.BehaviorProcessor
import org.reactivestreams.Subscription
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {

    val fastDelays = listOf(1,2,4,8,4,2,1).map { it * 100L }
    val slowDelays = (1..2).map { 1000L }

    val duration = measureTimeMillis {
        val smallFlows: Flowable<Flowable<Runnable>> = Flowable.fromArray(*Array(80) { i ->
            val subj = BehaviorProcessor.create<Runnable>()
            subj.onNext(subj.nextRun(fastDelays))
            subj
            /*val queue = LinkedBlockingQueue<Optional<Runnable>>()
            queue.add(Optional.of(queue.nextRun(fastDelays)))
            Flowable.generate<Runnable> { emitter ->
                val item = queue.take()
                //println("item take fast $queue ${Thread.currentThread()}")
                if (item.isPresent) emitter.onNext(item.get())
                else emitter.onComplete()
            }*/
        })

        //val queue = LinkedBlockingQueue<Optional<Runnable>>()
        //queue.add(Optional.of(queue.nextRun(slowDelays)))
        val p = BehaviorProcessor.create<Runnable>()
        p.onNext(p.nextRun(slowDelays))
        /*val bigFlow: Flowable<Runnable> = Flowable.generate<Runnable> { emitter ->
            val item = queue.poll()
            if (item != null) {
                //println("item take slow $queue ${Thread.currentThread()}")
                if (item.isPresent) emitter.onNext(item.get())
                else emitter.onComplete()
            }
        }*/

        val flows: Flowable<Flowable<Runnable>> = Flowable.just(p as Flowable<Runnable>).concatWith(smallFlows)

        val par = 16
        //val s1 = Schedulers.from(Executors.newFixedThreadPool(par))
        //val s2 = Schedulers.from(Executors.newFixedThreadPool(par))

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

    println("Duration: $duration")
}

private fun BehaviorProcessor<Runnable>.nextRun(list: List<Long>): Runnable = Runnable {
    println("Sleep for ${list.first()} on ${Thread.currentThread()} on ${this.hashCode()}")
    Thread.sleep(list.first())
    val rest = list.drop(1)
    if (rest.isEmpty()) this.onComplete()
    else this.onNext(this.nextRun(rest))
}