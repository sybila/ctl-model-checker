package com.github.sybila.checker

import com.github.daemontus.egholm.concurrent.guardedThreadUntilPoisoned
import com.github.daemontus.egholm.concurrent.poison
import com.github.daemontus.egholm.functional.Maybe
import com.github.daemontus.egholm.logger.lFine
import com.github.daemontus.egholm.logger.lFinest
import com.github.daemontus.egholm.logger.lInfo
import com.github.daemontus.jafra.Terminator
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import java.util.logging.Logger


/**
 * A model checking job carrying info about source/target node and propagated color set.
 */
data class Job<N: Node, C: Colors<C>>(
        val source: N,
        val target: N,
        val colors: C
)


/**
 * Queue of jobs waiting for execution. Do not confuse with communicator.
 * Communicator does not provide any synchronization. Job Queue should provide
 * basic termination detection.
 *
 * Termination is reached when all queues are waiting for termination, all queues are empty,
 * all callbacks are finished and no inter-queue messages are waiting in the system.
 */
interface JobQueue<N: Node, C: Colors<C>> {

    /**
     * Enqueue new job.
     * Should not block.
     */
    fun post(job: Job<N, C>)

    /**
     * Wait for termination detection and then finalize this queue.
     * Should block.
     * New jobs can be posted while waiting for termination, but not after terminating.
     */
    fun waitForTermination()

    interface Factory<N: Node, C: Colors<C>> {

        /**
         * Create new job queue that will execute onTask callback whenever job is posted.
         * This call should be a global barrier across all processes,
         * meaning that either all processes create new queue,
         * or no queue is created and the method blocks until
         * all processes reach it. (Or fails everywhere)
         * Also, this method returns only after all queues has been successfully initialized.
         * Simply: When this method returns, any process can post jobs and expect them to be delivered.
         *
         * You can also assume that the callback won't be called concurrently, so in general there is no need
         * for synchronization on fields accessed from it.
         */
        fun createNew(initial: List<Job<N, C>> = listOf(), onTask: JobQueue<N, C>.(Job<N, C>) -> Unit): JobQueue<N, C>
    }

    //Computation statistics
    //Time spent processing jobs (including state space generation)
    var timeInJobs: Long
    //Number of processed jobs
    var jobsProcessed: Long
    //Number of jobs posted to this queue
    var jobsPosted: Long
    //Number of jobs sent by this queue
    var jobsSent: Long
    //Number of jobs received from communicator
    var jobsReceived: Long

}

class SingleThreadJobQueue<N: Node, C: Colors<C>>(
        initial: List<Job<N, C>>,
        private val comm: Communicator,
        private val terminators: Terminator.Factory,
        partitioning: PartitionFunction<N>,
        private val onTask: JobQueue<N, C>.(Job<N, C>) -> Unit,
        private val logger: Logger = Logger.getLogger(SingleThreadJobQueue::class.java.canonicalName).apply {
            this.level = Level.OFF
        }
) :
        JobQueue<N, C>,
        PartitionFunction<N> by partitioning
{
    //Time spent processing jobs (including state space generation)
    override var timeInJobs = 0L
    //Number of processed jobs
    override var jobsProcessed = 0L
    //Number of jobs posted to this queue
    override var jobsPosted = 0L
    //Number of jobs sent by this queue
    override var jobsSent = 0L
    //Number of jobs received from communicator
    override var jobsReceived = 0L

    private var active = false

    private var lastProgressUpdate = 0L

    private val localQueue = LinkedBlockingQueue<Maybe<Job<N, C>>>()

    private val workRound = terminators.createNew() //run before constructor - it can be called from comm listener and that would deadlock

    init {
        //init communication session
        val initRound = terminators.createNew()
        comm.addListener(genericClass<Job<N, C>>()) {
            synchronized(localQueue) {
                //must be set first, otherwise we might process
                //message before terminator is marked as working
                jobsReceived += 1
                workRound.messageReceived()
                localQueue.add(Maybe.Just(it))
            }
        }
        initRound.setDone()
        initRound.waitForTermination()
        active = true
    }

    init {
        //add initial jobs, if any
        logger.lFine { "Init queue with ${initial.size} jobs."}
        initial.map { post(it) }
        doneIfEmpty()   //at this point, worker is not running, so if something went into our queue, it's still there!
    }

    //Last thing - start the worker
    //this can't be done sooner because we might be interleaving with job insertion
    private val worker = localQueue.guardedThreadUntilPoisoned { job ->
        jobsProcessed += 1
        val start = System.nanoTime()
        onTask(job)
        timeInJobs += System.nanoTime() - start
        if (start - lastProgressUpdate > 2 * 1000 * 1000 * 1000L) {
            //print progress every two seconds
            logger.lInfo { "Remaining: ${localQueue.size}, $lastProgressUpdate, $start" }
            lastProgressUpdate = start
        }
        doneIfEmpty()
    }

    override fun post(job: Job<N, C>) {
        jobsPosted += 1
        when {
            !active -> throw IllegalStateException("Posting on inactive JobQueue! $myId")
            job.target.ownerId() == myId -> {
                synchronized(localQueue) {
                    localQueue.put(Maybe.Just(job))
                }
            }
            else -> {
                jobsSent += 1
                workRound.messageSent()
                logger.lFinest { "Send job to ${job.target.ownerId()}" }
                comm.send(job.target.ownerId(), job)
            }
        }
    }

    override fun waitForTermination() {
        logger.lFine { "Waiting for termination" }
        workRound.waitForTermination()
        active = false
        val finalRound = terminators.createNew()
        comm.removeListener(genericClass<Job<N, C>>())
        localQueue.poison()
        worker.join()
        finalRound.setDone()
        finalRound.waitForTermination()
    }

    private fun doneIfEmpty() {
        synchronized(localQueue) {
            if (localQueue.isEmpty()) workRound.setDone()
        }
    }

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
                return SingleThreadJobQueue(initial, communicators[i], terminators[i], partitioning[i], onTask)
            }
        }
    }
}