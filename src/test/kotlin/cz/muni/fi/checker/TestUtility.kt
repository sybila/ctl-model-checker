package cz.muni.fi.checker

import java.util.concurrent.FutureTask

/**
 * Add here everything you find useful for testing, but can't quite see in the main package
 * (it's unsafe/slow/ugly or just too good for anyone to use!)
 */

fun <T> T.repeat(n: Int): List<T> = (1..n).map { this }

fun <N: Node, C: Colors<C>> withSingleModelChecker(
        model: KripkeFragment<N, C>,
        task: (ModelChecker<N, C>) -> Unit) {

    withModelCheckers(
            listOf(model),
            listOf(UniformPartitionFunction()),
            task
    )

}

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

fun pow (a: Int, b: Int): Int {
    if ( b == 0)        return 1;
    if ( b == 1)        return a;
    if ( b % 2 == 0)    return     pow ( a * a, b/2); //even a=(a^2)^b/2
    else                return a * pow ( a * a, b/2); //odd  a=a*(a^2)^b/2
}