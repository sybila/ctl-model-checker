package cz.muni.fi.checker

import java.util.*
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread

/**
 * This is a utility file for all the goodies you won't find anywhere else.
 * Remember, with great power comes great responsibility.
 *
 * If something in here exceeds 100 lines or is connected strictly to something else,
 * refactor it away!
 */

/**
 * Haskell style repeat
 */
public fun <T: Any> T.repeat(n: Int): List<T> = (1..n).map { this }
public fun <I, T: Iterable<I>> T.flatRepeat(n: Int): List<I> = (1..n).flatMap { this }


/**
 * Haskell style Maybe
 * Note: We need this because queues don't accept null values.
 */
public sealed class Maybe<T: Any> {

    public class Just<T: Any>(public val value: T): Maybe<T>() {
        override fun equals(other: Any?): Boolean = value.equals(other)
        override fun hashCode(): Int = value.hashCode()
    }

    public class Nothing<T: Any>(): Maybe<T>()
}

/**
 * Helper method to reify a generic class at compile time (Since you can't write K<A,B>::class)
 */
public inline fun <reified T: Any> genericClass(): Class<T> = T::class.java

/**
 * Return random subset of this set - mainly for testing, etc.
 */
public fun <T> Set<T>.randomSubset(): Set<T> {
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

//We need our own interval, because we do not allow single points (i.e. (3,3)) as valid intervals
//rest is copied from original Kotlin range
data class Interval(val start: Double, val end: Double) {

    fun isEmpty(): Boolean = start >= end

    infix fun encloses(other: Interval): Boolean =
            (other == this /*Handles empty ranges*/) || (other.start >= this.start && other.end <= this.end)

    /**
     * Returns null if intervals can't be merged
     */
    infix fun merge(other: Interval): Interval? = when {
        this.isEmpty() -> other
        other.isEmpty() -> this
        Math.max(start, other.start) > Math.min(this.end, other.end) -> null    //"weaker" intersection for (0..1) (1..2) cases
        else -> Interval(Math.min(this.start, other.start), Math.max(this.end, other.end))
    }

    infix fun intersect(other: Interval): Interval = when {
        this.isEmpty() || other.isEmpty() -> Interval.EMPTY
        else -> Interval(Math.max(this.start, other.start), Math.min(this.end, other.end))
    }

    infix operator fun minus(other: Interval): Set<Interval> {
        if (this.isEmpty()) return emptySet()
        if (other.isEmpty()) return setOf(this)
        return setOf(Interval(this.start, other.start), Interval(other.end, this.end))
    }

    override fun equals(other: Any?): Boolean =
            other is Interval && (isEmpty() && other.isEmpty() ||
                    java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        val result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    companion object {
        /** An empty range of values of type Double. */
        public val EMPTY: Interval = Interval(0.0, 0.0)
    }

    override fun toString(): String {
        return "($start, $end)"
    }
}

/*
infix fun IntRange.encloses(other: IntRange): Boolean
        = this.first <= other.first && this.last >= other.last

infix fun IntRange.merge(other: IntRange): IntRange? = when {
    this.first > other.first -> other merge this
    this.last < other.first -> null
    else -> this.first..other.last
}

infix operator fun IntRange.minus(other: IntRange): IntRange = when {
    other encloses this -> 1..0
    this.first > other.last || this.last < other.first -> this
    this.first > other.first -> other.last..this.last
    else -> this.first..other.first
}

infix fun IntRange.intersect(other: IntRange): IntRange {
    return Math.max(this.first, other.first)..Math.min(this.last, other.last)
}*/


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
