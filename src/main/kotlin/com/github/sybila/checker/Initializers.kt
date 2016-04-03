package com.github.sybila.checker

import com.github.daemontus.egholm.functional.Maybe
import com.github.daemontus.egholm.logger.severeLoggers
import com.github.daemontus.egholm.thread.guardedThread
import com.github.daemontus.egholm.util.use
import com.github.daemontus.jafra.Terminator
import com.github.daemontus.jafra.Token
import java.util.concurrent.BlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger

/**
 * Create a group of shared mem communicators connected by blocking queues.
 */
fun createSharedMemoryCommunicators(
        processCount: Int,
        initialListeners: (Int) -> Map<Class<*>, (Any) -> Unit> = { id -> mapOf() },
        loggers: (Int) -> Logger = severeLoggers
): List<Communicator> {

    //Array of queues that deliver message and it's class and can be poisoned.
    //Nullable since we need to indicate communicator closing.
    val queues = Array<BlockingQueue<Maybe<Any>>?>(processCount, {
        LinkedBlockingQueue<Maybe<Any>>()
    })

    return (0 until processCount).map {
        SharedMemoryCommunicator(it, processCount, queues, initialListeners(it), loggers(it))
    }
}

fun <R> withSharedMemoryComm(
        processCount: Int,
        loggers: (Int) -> Logger = severeLoggers,
        action: (Communicator, Terminator.Factory) -> R): List<R> {
    val tokenMessengers = (0 until processCount).map { CommunicatorTokenMessenger(it, processCount) }

    @Suppress("UNCHECKED_CAST")
    val comm = createSharedMemoryCommunicators(processCount,
            { id -> mapOf((Token::class.java as Class<*>) to ({ m: Token -> tokenMessengers[id](m) } as (Any) -> Unit)) },
            loggers)

    tokenMessengers.zip(comm).forEach {
        it.first.comm = it.second
    }

    return comm.use { comm ->
        tokenMessengers.use { tokens ->
            val terminators = tokens.toFactories()
            comm.zip(terminators).map {
                FutureTask<R> {
                    action(it.first, it.second)
                }
            }.map {
                guardedThread { it.run() }; it
            }.map { it.get() }
        }
    }
}

fun List<CommunicatorTokenMessenger>.toFactories(): List<Terminator.Factory> = this.map { Terminator.Factory(it) }

/**
 * Initializes a single model checker with in memory messaging and then properly closes it.
 */
fun <N: Node, C: Colors<C>, R> withSingleModelChecker(
        model: KripkeFragment<N, C>,
        task: (ModelChecker<N, C>) -> R): R {
    return withModelCheckers( models = listOf(model), task = task ).first()
}

/**
 * Initialize multiple model checkers connected using shared memory and properly closes them after the task is complete.
 */
fun <N: Node, C: Colors<C>, R> withModelCheckers(
        models: List<KripkeFragment<N, C>>,
        partitions: (Int) -> PartitionFunction<N> = { UniformPartitionFunction<N>(it) },
        loggers: (Int) -> Logger = severeLoggers,
        queues: (Int, Communicator, Terminator.Factory) -> JobQueue.Factory<N, C> =
        { id: Int, comm: Communicator, term: Terminator.Factory -> object : JobQueue.Factory<N, C> {
            override fun createNew(initial: List<Job<N, C>>, onTask: JobQueue<N, C>.(Job<N, C>) -> Unit): JobQueue<N, C> {
                return SingleThreadQueue(initial, comm, term, partitions(id), onTask, loggers(id))
            }
        } },
        task: (ModelChecker<N, C>) -> R): List<R> {

    return withSharedMemoryComm(models.size, loggers) { comm, terminator ->
        val queue = queues(comm.id, comm, terminator)
        task(ModelChecker(models[comm.id], queue))
    }
}