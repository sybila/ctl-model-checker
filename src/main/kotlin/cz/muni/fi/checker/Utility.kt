package cz.muni.fi.checker

import java.util.*
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread

/**
 * This is a utility file for all the goodies you won't find anywhere else.
 * Remember, with great power comes great responsibility.
 *
 * If something in here exceeds ~100 lines or is connected strictly to something else,
 * refactor it away!
 */

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

/**
 * Cartesian product of two iterable objects.
 */
infix fun <T, U> Iterable<T>.times(other: Iterable<U>): Iterable<Pair<T, U>> {
    return this.flatMap { a -> other.map { b -> Pair(a, b) } }
}

/**
 * "Flat" cartesian product.
 */
infix fun <T> Iterable<List<T>>.flatTimes(other: Iterable<List<T>>): List<List<T>> {
    return this.flatMap { a -> other.map { b -> a + b } }
}

/**
 * "Exponentiation" of collections - works as repeated flat cartesian product.
 */
fun <T> Iterable<T>.pow(exp: Int): List<List<T>> {
    if (exp == 0) return listOf()
    var one = this.map { listOf(it) }
    var r = one
    for (i in 2..exp) {
        r = r flatTimes one
    }
    return r
}

/**
 * Create a thread listening to all items in a blocking queue until Nothing is received.
 * (Classic poison pill principle)
 */
fun <T: Any> BlockingQueue<Maybe<T>>.threadUntilPoisoned(onItem: (T) -> Unit) = thread {
    var job = this.take()
    while (job is Maybe.Just) {
        onItem(job.value)
        job = this.take()
    }   //got Nothing
}

/**
 * And poison it afterwards.
 */
fun <T: Any> BlockingQueue<Maybe<T>>.poison() = this.put(Maybe.Nothing())