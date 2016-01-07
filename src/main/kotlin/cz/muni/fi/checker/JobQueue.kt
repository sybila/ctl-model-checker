package cz.muni.fi.checker

import com.github.daemontus.jafra.Terminator
import com.github.daemontus.jafra.createSharedMemoryTerminators
import java.util.concurrent.LinkedBlockingQueue


/**
 * A base class for all types of jobs that can be created during the algorithm processing.
 *
 * The distinction is made in order to provide better flexibility for future changes
 * (SSC detection, etc.) - also semantics of data may vary across algorithms for each operator.
 *
 * Note that messenger you are using must implement suitable serialization algorithms
 * if you want to send Jobs through it.
 */
interface Job<N: Node, C: Colors<C>> {

    val destination: N

    data class AU<N: Node, C: Colors<C>>(
            val sourceNode: N,
            val targetNode: N,
            val colors: C
    ) : Job<N, C> {
        override val destination: N = targetNode
    }

    data class EU<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C> {
        override val destination: N = node
    }

    data class EX<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C> {
        override val destination: N = node
    }

}


/**
 * Queue of jobs waiting for execution. Do not confuse with message queue.
 * Message queue should accept any message and only throw error if it can't send it.
 * Jobs are specific to model checking algorithm and every job queue should have it's specific type.
 *
 * Job queue should use some sort of termination detection mechanism to detect when no jobs are
 * being posted in the system.
 *
 * Termination is reached when all queues are waiting for termination, all queues are empty,
 * all callbacks are finished and no inter-queue messages are waiting in the system.
 */
public interface JobQueue<N: Node, C: Colors<C>, J: Job<N, C>> {

    /**
     * Enqueue new job.
     * Should not block.
     */
    fun post(job: J)

    /**
     * Wait for termination detection and then finalize this queue.
     * Should block.
     * New jobs can be posted while waiting for termination, but not after terminating.
     */
    fun waitForTermination()

    public interface Factory<N: Node, C: Colors<C>> {

        /**
         * Create new job queue that will execute onTask callback whenever job is posted.
         * This call should be a global barrier across all processes,
         * meaning that either all processes create new queue,
         * or no queue is created and the method blocks until
         * all processes reach it.
         * Also, this method returns only after all queues has been successfully initialized.
         * Simply: When this method returns, any process can post jobs and expect them to be delivered.
         */
        fun <J : Job<N, C>> createNew(initial: List<J> = listOf(), jobClass: Class<J>, onTask: JobQueue<N, C, J>.(J) -> Unit): JobQueue<N, C, J>
    }

}

/**
 * Single threaded implementation of job queue. Relies on Communicator contracts for global synchronisation.
 */
public class SingleThreadJobQueue<N: Node, C: Colors<C>, J: Job<N, C>> (
        initialJobs: List<J> = listOf(),
        comm: Communicator,
        terminators: Terminator.Factory,
        partitioning: PartitionFunction<N>,
        jobClass: Class<J>,
        private val onTask: JobQueue<N, C, J>.(J) -> Unit   //extension function allows recursive adding
) :
        JobQueue<N, C, J>,
        PartitionFunction<N> by partitioning
{

    private val queueLock = Object()

    private var active: Boolean = true

    private val localTaskQueue = LinkedBlockingQueue<Maybe<J>>(initialJobs.map { Maybe.Just(it) })

    private val terminator = terminators.createNew()

    init {
        //If we don't have initial jobs, we are done straight away
        if (initialJobs.isEmpty()) doneIfEmpty()
    }

    private val messenger = comm.listenTo(jobClass) {
        synchronized(queueLock) {
        //    println("${System.nanoTime()} ($myId) Putting into queue: ${localTaskQueue.size}")
            localTaskQueue.put(Maybe.Just(it))
            terminator.messageReceived()
        //    println("${System.nanoTime()} ($myId) Accepted: ${localTaskQueue.size}")
        }
    }

    private val worker: Thread = localTaskQueue.threadUntilPoisoned {
    //    println("${System.nanoTime()} ($myId) Before task")
        onTask(it)
     //   println("${System.nanoTime()} ($myId) Queue: ${localTaskQueue.size}")
        doneIfEmpty()
    }

    private fun doneIfEmpty() {
        synchronized(queueLock) {
            if (localTaskQueue.isEmpty()) {
          //      println("${System.nanoTime()} ($myId) Before done: ${terminator.working}")
                terminator.setDone()
          //      println("${System.nanoTime()} ($myId) After done: ${terminator.working}")
            }
        }
    }

    override fun post(job: J): Unit {
        synchronized(queueLock) {
         //   println("${System.nanoTime()} ($myId) Posting")
            if (!active) throw IllegalStateException("Posting job on finished job queue")
            if (job.destination.ownerId() == myId) {
                localTaskQueue.put(Maybe.Just(job))
            } else {
                messenger.sendTask(job.destination.ownerId(), job)
            }
        }
    }

    override fun waitForTermination() {
      //  println("($myId) Waiting for termination")
        terminator.waitForTermination()
      //  println("($myId) Waiting for messenger")
        messenger.close()
        synchronized(queueLock) {
            active = false
            localTaskQueue.put(Maybe.Nothing())
        }
        worker.join()
    }

}

fun <N: Node, C: Colors<C>> createSharedMemoryJobQueue(
        processCount: Int,
        partitioning: List<PartitionFunction<N>> = (1..processCount).map { UniformPartitionFunction<N>(it-1) },
        terminators: List<Terminator.Factory> = createSharedMemoryTerminators(processCount),
        communicators: List<Communicator> = createSharedMemoryCommunicators(processCount)
): List<JobQueue.Factory<N, C>> {
    return (0..(processCount-1)).map { i ->
        object : JobQueue.Factory<N, C> {
            override fun <J : Job<N, C>> createNew(initial: List<J>, jobClass: Class<J>, onTask: JobQueue<N, C, J>.(J) -> Unit): JobQueue<N, C, J> {
                return SingleThreadJobQueue(
                        initialJobs = initial,
                        comm = communicators[i],
                        terminators = terminators[i],
                        jobClass = jobClass,
                        partitioning = partitioning[i],
                        onTask = onTask)
            }
        }
    }
}