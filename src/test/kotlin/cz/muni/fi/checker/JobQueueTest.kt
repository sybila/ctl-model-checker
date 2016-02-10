package cz.muni.fi.checker

import org.junit.Test
import java.util.*
import java.util.concurrent.FutureTask
import kotlin.concurrent.thread
import kotlin.test.assertEquals


class BigSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val repetitions: Int = 2
    override val processCount: Int = 24

}

class SmallSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val repetitions: Int = 100
    override val processCount: Int = 4
}

class OneSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val repetitions: Int = 100
    override val processCount: Int = 1
}

abstract class SingleThreadJobQueueTest: JobQueueTest() {

    override fun createJobQueues(
            processCount: Int,
            partitioning: List<PartitionFunction<IDNode>>
    ): List<JobQueue.Factory<IDNode, IDColors>>
            = createSingleThreadJobQueues(
                processCount = processCount,
                partitioning = partitioning
            )

}

val jobComparator = Comparator<Job<IDNode, IDColors>> { o1, o2 ->
    when {
        o1.source == o2.source && o1.target == o2.target -> o1.colors.toString().compareTo(o2.colors.toString())
        o1.source == o2.source -> o1.target.id.compareTo(o2.target.id)
        else -> o1.source.id.compareTo(o2.source.id)
    }
}

/**
 * And abstract set of tests for your job queue implementation.
 * Just override queue constructor.
 */

abstract class JobQueueTest {

    abstract val processCount: Int
    abstract val repetitions: Int

    abstract fun createJobQueues(
            processCount: Int,
            partitioning: List<PartitionFunction<IDNode>> = (1..processCount).map { UniformPartitionFunction<IDNode>(it-1) }
    ): List<JobQueue.Factory<IDNode, IDColors>>

    private val allColors = (1..5).toSet()

    private fun randomEuJob(): Job<IDNode, IDColors> = Job(
            IDNode((Math.random() * 10).toInt()),
            IDNode((Math.random() * 10).toInt()),
            IDColors(allColors.randomSubset())
    )


    @Test(timeout = 1000)
    fun noMessages() {
        createJobQueues(processCount).map { f -> thread {
            val q = f.createNew() {  }
            q.waitForTermination()
        } }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun onlyInitialTest() {
        repeat(repetitions) {
            createJobQueues(processCount).map { f -> thread {

                val executed = ArrayList<Job<IDNode, IDColors>>()
                val jobs = (1..10).map { randomEuJob() }

                val q = f.createNew(jobs) { synchronized(executed) {
                    executed.add(it)
                } }

                q.waitForTermination()

                assertEquals(
                        jobs.sortedWith(jobComparator),
                        executed.sortedWith(jobComparator)
                )
            } }.map { it.join() }
        }
    }

    @Test(timeout = 5000)
    fun complexTest() {

        repeat(repetitions) {
            //As in messenger tests, create a flood of jobs that will jump across state space

            val allJobs = createJobQueues(
                    processCount,
                    (1..processCount).map { i -> FunctionalPartitionFunction<IDNode>(i-1) { it.id % processCount } }
            ).map { f -> FutureTask {

                val executed = ArrayList<Job<IDNode, IDColors>>()
                val posted = HashMap((1..processCount).associateBy({ it - 1 }, { ArrayList<Job<IDNode, IDColors>>() }))

                val initial = (1..(processCount)).map { randomEuJob() }
                initial.forEach { job -> synchronized(posted) {
                    posted[job.target.id % processCount]!!.add(job)
                } }

                //System.out.println("Initial size: ${initial.size}")
                val queue = f.createNew(initial) {
                   // System.out.println("job done")
                    synchronized(executed) { executed.add(it) }
                    if (it.target.id != 0) {
                        val newNodeId = it.target.id - 1
                        val newJob = Job(it.target, IDNode(newNodeId), it.colors)
                        synchronized(posted) {
                            posted[newNodeId % processCount]!!.add(newJob)
                        }
                        this.post(newJob)
                    }
                }

                queue.waitForTermination()

                Pair(posted, executed)
            } }.map { thread { it.run() }; it }.map { it.get() }

            //Merge sent messages by their destinations into something that has same type as received list
            val sent = allJobs.map { it.first }.foldRight(
                    HashMap((0..(processCount - 1)).map { Pair(it, listOf<Job<IDNode, IDColors>>()) }.toMap())
            ) { value, accumulator ->
                for ((key, list) in value) {
                    accumulator[key] = list + accumulator[key]!!
                }
                accumulator
            }.mapValues {
                it.value.sortedWith(jobComparator)
            }
            val received = allJobs.map { it.second }.mapIndexed { i, list -> Pair(i, list.sortedWith(jobComparator)) }.toMap()

            assertEquals(received, sent)

            //For debugging:
            //throw IllegalStateException("Transferred: ${sent.values.fold(0, { f, s -> f + s.size })}")
        }
    }

}