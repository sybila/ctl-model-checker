package com.github.sybila.checker

import com.github.daemontus.jafra.Terminator
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.FutureTask
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This is a utility file for all the goodies you won't find anywhere else.
 * Remember, with great power comes great responsibility.
 *
 * If something in here exceeds ~100 lines or is connected strictly to something else,
 * refactor it away!
 */

/**
 * Logger extensions for painless lazy logging
 */
fun Logger.lInfo(action: () -> String) {
    if (this.isLoggable(Level.INFO)) {
        this.info(action())
    }
}

fun Logger.lFine(action: () -> String) {
    if (this.isLoggable(Level.FINE)) {
        this.fine(action())
    }
}

fun Logger.lFinest(action: () -> String) {
    if (this.isLoggable(Level.FINEST)) {
        this.finest(action())
    }
}


/**
 * Utility conversions
 */
fun List<Communicator>.toTokenMessengers(): List<CommunicatorTokenMessenger> = this.map { CommunicatorTokenMessenger(it) }
fun List<CommunicatorTokenMessenger>.toFactories(): List<Terminator.Factory> = this.map { Terminator.Factory(it) }

/**
 * Haskell style repeat
 */
fun <I, T: Iterable<I>> T.flatRepeat(n: Int): List<I> = (1..n).flatMap { this }


/**
 * Haskell style Maybe
 * Note: We need this because queues don't accept null values.
 */
sealed class Maybe<T: Any> {

    class Just<T: Any>(val value: T): Maybe<T>() {
        override fun equals(other: Any?): Boolean = value.equals(other)
        override fun hashCode(): Int = value.hashCode()
    }

    class Nothing<T: Any>(): Maybe<T>() {
        override fun equals(other: Any?): Boolean = other is Nothing<*>
        override fun hashCode(): Int = 23
    }
}

/**
 * Helper method to reify a generic class at compile time (Since you can't write K<A,B>::class)
 */
inline fun <reified T: Any> genericClass(): Class<T> = T::class.java

/**
 * Return random subset of this set - mainly for testing, etc.
 */
fun <T> Set<T>.randomSubset(): Set<T> {
    val remove = (Math.random() * size).toInt()
    val result = ArrayList<T>(this)
    for (i in 0..remove) {
        val victim = (Math.random() * result.size)
        result.removeAt(victim.toInt())
    }
    return result.toSet()
}


class GuardedThread(
        val thread: Thread
) {

    constructor(task: () -> Unit) : this(Thread(task))

    var ex: Throwable? = null

    init {
        thread.setUncaughtExceptionHandler { thread, throwable ->
            ex = throwable
            //sometimes, thread won't join because of this exception. Print it so that we at least know something is wrong.
            System.err.println("Uncaught exception in $thread")
            ex?.printStackTrace()
        }
    }

    fun join() {
        thread.join()
        if (ex != null) throw ex!!
    }

}

fun guardedThread(task: () -> Unit) = GuardedThread(task).apply { this.thread.start() }

/**
 * Create a thread listening to all items in a blocking queue until Nothing is received.
 * (Classic poison pill principle)
 */
fun <T: Any> BlockingQueue<Maybe<T>>.threadUntilPoisoned(onItem: (T) -> Unit): GuardedThread {
    val result = GuardedThread {
        var job = this.take()
        while (job is Maybe.Just) {
            onItem(job.value)
            job = this.take()
        }   //got Nothing
    }
    result.thread.start()
    return result
}

/**
 * And poison it afterwards.
 */
fun <T: Any> BlockingQueue<Maybe<T>>.poison() = this.put(Maybe.Nothing())


/**
 * Initializes a single model checker with in memory messaging and then properly closes it.
 */
fun <N: Node, C: Colors<C>> withSingleModelChecker(
        model: KripkeFragment<N, C>,
        task: (ModelChecker<N, C>) -> Unit) {

    withModelCheckers(
            listOf(model),
            listOf(UniformPartitionFunction()),
            task
    )

}

/**
 * Initialize multiple model checkers connected using shared memory and properly closes them after the task is complete.
 */
fun <N: Node, C: Colors<C>, R> withModelCheckers(
        models: List<KripkeFragment<N, C>>,
        partitions: List<PartitionFunction<N>> = (0 until models.size).map { UniformPartitionFunction<N>(it) },
        task: (ModelChecker<N, C>) -> R): List<R> {

    val comm = createSharedMemoryCommunicators(models.size)
    val tokens = comm.toTokenMessengers()
    val terminators = tokens.toFactories()
    val queues = createSingleThreadJobQueues<N, C>(
            models.size, partitions, comm, terminators)

    val result = queues.zip(models).map {
        ModelChecker(it.second, it.first)
    }.map {
        FutureTask {
            task(it)
        }
    }.map { guardedThread { it.run() }; it }.map { it.get() }

    tokens.map { it.close() }
    comm.map { it.close() }
    return result
}