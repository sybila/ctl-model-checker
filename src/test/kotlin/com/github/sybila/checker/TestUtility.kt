package com.github.sybila.checker

import com.github.sybila.checker.new.Solver
import com.github.sybila.checker.new.StateMap
import com.github.sybila.checker.new.deepEquals
import kotlin.test.assertTrue

fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1;
    if ( b == 1)        return a;
    if ( b % 2 == 0)    return pow (a * a, b / 2); //even a=(a^2)^b/2
    else                return a * pow (a * a, b / 2); //odd  a=a*(a^2)^b/2
}

fun <Colors> assertDeepEquals(expected: StateMap<Colors>, actual: StateMap<Colors>, solver: Solver<Colors>)
        = assertTrue(deepEquals(expected, actual, solver), "Expected $expected, actual $actual")

fun <Colors> assertDeepEquals(full: Pair<StateMap<Colors>, Solver<Colors>>, partitions: List<Pair<StateMap<Colors>, Solver<Colors>>>)
        = assertTrue(deepEquals(full, partitions), "Expected ${full.first}, actual ${partitions.map { it.first }}")
/*
import com.github.daemontus.jafra.Terminator
import java.util.*

/**
 * Add here everything you find useful for testing, but can't quite see in the main package
 * (it's unsafe/slow/ugly or just too good for anyone to use!)
 */


val jobComparator = Comparator<Job<IDNode, IDColors>> { o1, o2 ->
    when {
        o1.source == o2.source && o1.target == o2.target -> o1.colors.toString().compareTo(o2.colors.toString())
        o1.source == o2.source -> o1.target.id.compareTo(o2.target.id)
        else -> o1.source.id.compareTo(o2.source.id)
    }
}

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

fun <N: Node, C: Colors<C>> createSingleThreadJobQueues(
        processCount: Int,
        partitioning: List<PartitionFunction<N>> = (1..processCount).map { UniformPartitionFunction<N>(it - 1) },
        communicators: List<Communicator>,
        terminators: List<Terminator.Factory>
): List<JobQueue.Factory<N, C>> {
    return (0..(processCount-1)).map { i ->
        object : JobQueue.Factory<N, C> {
            override fun createNew(initial: List<Job<N, C>>, onTask: JobQueue<N, C>.(Job<N, C>) -> Unit): JobQueue<N, C> {
                return SingleThreadQueue(initial, communicators[i], terminators[i], partitioning[i], onTask)
            }
        }
    }
}

fun <N: Node, C: Colors<C>> createMergeQueues(
        processCount: Int,
        partitioning: List<PartitionFunction<N>> = (1..processCount).map { UniformPartitionFunction<N>(it - 1) },
        communicators: List<Communicator>,
        terminators: List<Terminator.Factory>
): List<JobQueue.Factory<N, C>> {
    return (0..(processCount-1)).map { i ->
        object : JobQueue.Factory<N, C> {
            override fun createNew(initial: List<Job<N, C>>, onTask: JobQueue<N, C>.(Job<N, C>) -> Unit): JobQueue<N, C> {
                return MergeQueue(initial, communicators[i], terminators[i], partitioning[i], onTask)
            }
        }
    }
}*/