package cz.muni.fi.checker

import com.github.daemontus.jafra.Terminator
import java.util.*
import java.util.concurrent.BlockingQueue

/**
 * This is a utility file for all the goodies you won't find anywhere else.
 * Remember, with great power comes great responsibility.
 *
 * If something in here exceeds ~100 lines or is connected strictly to something else,
 * refactor it away!
 */


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
        override fun equals(other: Any?): Boolean = other is Maybe.Nothing<*>
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
            System.err.println("Uncaught exception in $thread: $throwable")
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