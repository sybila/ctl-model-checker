package com.github.sybila.checker

import com.github.daemontus.egholm.thread.guardedThread
import com.github.daemontus.jafra.Terminator
import java.util.*
import java.util.concurrent.FutureTask

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
 * Helper method to reify a generic class at compile time (Since you can't write K<A,B>::class)
 */
inline fun <reified T: Any> genericClass(): Class<T> = T::class.java

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