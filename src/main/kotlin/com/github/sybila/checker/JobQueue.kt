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