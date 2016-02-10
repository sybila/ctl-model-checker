package cz.muni.fi.checker

import com.github.daemontus.jafra.Terminator
import org.junit.Test
import java.util.*
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread
import kotlin.test.assertEquals

data class TestMessage(val number: Int): Comparable<TestMessage> {
    override fun compareTo(other: TestMessage): Int = number.compareTo(other.number)
}


class SmallSharedMemoryCommunicatorTest : CommunicatorTest() {

    override val repetitions: Int = 100
    override val processCount: Int = 2

    override val communicatorConstructor: (Int) -> List<Communicator>
            = { c -> createSharedMemoryCommunicators(c) }

}

class BigSharedMemoryCommunicatorTest : CommunicatorTest() {

    override val repetitions: Int = 1
    override val processCount: Int = 24

    override val communicatorConstructor: (Int) -> List<Communicator>
            = { c -> createSharedMemoryCommunicators(c) }

}


abstract class CommunicatorTest {

    abstract val processCount: Int
    abstract val repetitions: Int
    abstract val communicatorConstructor: (Int) -> List<Communicator>


    @Test(timeout = 1000)
    fun emptyRun() {
        communicatorConstructor(processCount).map { it.close() }
    }

    @Test(timeout = 1000)
    fun oneMessengerNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                it.removeListener(TestMessage::class.java)
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun moreMessengersNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                it.removeListener(TestMessage::class.java)

                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                it.removeListener(TestMessage::class.java)
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun oneMessengerWithMessages() {

        //This barrier will be triggered by the receiver when all messages are received and
        //by sender when all messages are sent, this way the barrier is left when
        //termination has been "detected"
        //If something goes wrong, receiver will never trigger and timeout will be reached
        val terminationBarrier = CyclicBarrier(2 * processCount)
        val initBarrier = CyclicBarrier(processCount)

        for (a in 1..repetitions) {
            communicatorConstructor(processCount).map { comm ->

                val expected = (0..(processCount - 1))
                        .filter { it != comm.id }
                        .map { TestMessage(it) }.sorted()

                thread {
                    val received = ArrayList<TestMessage>()

                    comm.addListener(TestMessage::class.java) {
                        received.add(it)
                        synchronized(expected) {
                            if (received.sorted() == expected) {
                                terminationBarrier.await()
                            }
                        }
                    }

                    initBarrier.await() //don't start sending before everyone is initialized!

                    (0..(processCount - 1))
                        .filter { it != comm.id }
                        .map { comm.send(it, TestMessage(comm.id)) }

                    terminationBarrier.await()

                    comm.removeListener(TestMessage::class.java)
                    comm.close()
                    assertEquals(expected.sorted(), received.sorted())
                }
            }.map { it.join() }
        }
    }

    @Test(timeout = 10000)
    fun moreMessengersWithMessages() {
        //Always sends messageCount messages to all other processes and then closes the messenger,
        //therefore we can safely predict how the expected received messages should look.
        //(It's going to be all messages repeated i-times except for message from itself)

        for (a in 1..repetitions) { //Repeat this a lot and hope for the best!
            val allMessages = (1..processCount).map { TestMessage(it - 1) }
            val messageCount = 10

            //This barrier will be triggered by the receiver when all messages are received and
            //by sender when all messages are sent, this way the barrier is left when
            //termination has been "detected"
            //If something goes wrong, receiver will never trigger and timeout will be reached
            val terminationBarrier = CyclicBarrier(2 * processCount)
            val initBarrier = CyclicBarrier(processCount)

            communicatorConstructor(processCount).map { comm ->
                Pair(comm, thread {
                    for (iteration in 1..10) {  //repeat to test if we are able to recreate messengers
                        val received = ArrayList<TestMessage>()
                        val expected = (allMessages - TestMessage(comm.id)).flatRepeat(messageCount)

                        comm.addListener(TestMessage::class.java) {
                            received.add(it)
                            if (received.sorted() == expected.sorted()) {
                                terminationBarrier.await()
                            }
                        }

                        initBarrier.await()

                        for (p in 0..(processCount * messageCount - 1)) {
                            if (p % comm.size != comm.id) {      //Don't send to yourself
                                comm.send(p % comm.size, TestMessage(comm.id))
                            }
                        }

                        terminationBarrier.await()

                        comm.removeListener(TestMessage::class.java)
                    }
                })
            }.map { it.second.join(); it.first.close() }
        }
    }


    @Test(timeout = 20000)
    fun complexTest() {
        //WARNING: this can actually take a while (Like 7s on a 2ghz dual core)

        //Initialize 10 * processCount floods with various lifespans and send them to random receivers.
        //If you receive positive flood message, pass it to next random process.
        //Since we can't send messages to ourselves, for two processes, this is completely deterministic.

        //Also, in the parallel, termination messages are sent using the same communicator.

        for (a in 1..repetitions) { //Repeat this a lot and hope for the best!

            val allMessages = communicatorConstructor(processCount).map { comm ->
                Pair(CommunicatorTokenMessenger(comm), comm)
            }.map {

                val (termComm, comm) = it
                val terminators = Terminator.Factory(termComm)

                fun randomReceiver(): Int {
                    var receiver = comm.id
                    while (receiver == comm.id) {
                        receiver = (Math.random() * this.processCount).toInt()
                    }
                    return receiver
                }

                val received = ArrayList<TestMessage>()
                val sent = HashMap((1..comm.size).associateBy({ it - 1 }, { ArrayList<TestMessage>() }))

                val worker = thread {
                    for (i in 1..5) {   //Create more messengers in a row in order to fully test the communicator

                        //save this for later ;) - after init round
                        val terminator = lazy { terminators.createNew() }

                        val initRound = terminators.createNew()

                        var doneSending = false

                        comm.addListener(TestMessage::class.java) {
                            synchronized(received) { received.add(it) }
                            terminator.value.messageReceived()
                            if (it.number > 0) {
                                val receiver = randomReceiver()
                                val message = TestMessage(it.number - 1)
                                synchronized(sent) { sent[receiver]!!.add(message) }
                                terminator.value.messageSent()
                                comm.send(receiver, message)
                            }
                            synchronized(doneSending) {
                                if (doneSending) terminator.value.setDone()
                            }
                        }

                        initRound.setDone()
                        initRound.waitForTermination()

                        for (p in 1..(processCount * 10)) {
                            val receiver = randomReceiver()
                            val message = TestMessage(p)
                            terminator.value.messageSent()
                            synchronized(sent) { sent[receiver]!!.add(message) }
                            comm.send(receiver, message)
                        }

                        synchronized(doneSending) {
                            doneSending = true
                        }

                        terminator.value.waitForTermination()

                        comm.removeListener(TestMessage::class.java)
                    }

                    termComm.close()
                    comm.close()
                }

                Pair(worker, Pair(sent, received))
            }.map {
                it.first.join(); it.second
            }

            //Merge sent messages by their destinations into something that has same type as received list
            val sent = allMessages.map { it.first }.foldRight(
                    HashMap((0..(processCount - 1)).map { Pair(it, listOf<TestMessage>()) }.toMap())
            ) { value, accumulator ->
                for ((key, list) in value) {
                    accumulator[key] = list + accumulator[key]!!
                }
                accumulator
            }.mapValues {
                it.value.sorted()
            }
            val received = allMessages.map { it.second }.mapIndexed { i, list -> Pair(i, list.sorted()) }.toMap()

            assertEquals(received, sent)
        }

    }

}