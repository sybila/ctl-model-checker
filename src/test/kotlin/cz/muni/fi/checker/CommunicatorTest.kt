package cz.muni.fi.checker

import com.github.daemontus.jafra.createSharedMemoryTerminators
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


public abstract class CommunicatorTest {

    abstract val processCount: Int
    abstract val repetitions: Int
    abstract val communicatorConstructor: (Int) -> List<Communicator>


    @Test(timeout = 1000)
    fun emptyRun() {
        communicatorConstructor(processCount).map { it.finalize() }
    }

    @Test(timeout = 1000)
    fun oneMessengerNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                val messenger = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                messenger.close()
                it.finalize()
            }
        }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun moreMessengersNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                val m1 = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                m1.close()

                val m2 = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                m2.close()
                it.finalize()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun oneMessengerWithMessages() {

        //This barrier will be triggered by the receiver when all messages are received and
        //by sender when all messages are sent, this way the barrier is left when
        //termination has been "detected"
        //If something goes wrong, receiver will never trigger and timeout will be reached
        val globalBarrier = CyclicBarrier(2 * processCount)

        for (a in 1..repetitions) {
            communicatorConstructor(processCount).map { comm ->
                val expected = (0..(processCount - 1))  //we need to compute this in advance, because of timing issues
                        .filter { it != comm.id }
                        .map { TestMessage(it) }
                Pair(expected, comm)
            }.map { it ->
                thread {
                    val (expected, comm) = it
                    val received = ArrayList<TestMessage>()

                    val messenger = comm.listenTo(TestMessage::class.java) {
                        received.add(it)
                        synchronized(expected) {
                            if (received.sorted() == expected.sorted()) {
                                globalBarrier.await()
                            }
                        }
                    }

                    (0..(processCount - 1))
                        .filter { it != comm.id }
                        .map { messenger.sendTask(it, TestMessage(comm.id)) }

                    globalBarrier.await()

                    messenger.close()
                    comm.finalize()
                    assertEquals(expected.sorted(), received.sorted())
                }
            }.map { it.join() }
        }
    }

    @Test(timeout = 2000)
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
            val globalBarrier = CyclicBarrier(2 * processCount)

            communicatorConstructor(processCount).map { comm ->
                Pair(comm, thread {
                    for (iteration in 1..10) {  //repeat to test if we are able to recreate messengers
                        val received = ArrayList<TestMessage>()
                        val expected = (allMessages - TestMessage(comm.id)).flatRepeat(messageCount)

                        val messenger = comm.listenTo(TestMessage::class.java) {
                            received.add(it)
                            if (received.sorted() == expected.sorted()) {
                                globalBarrier.await()
                            }
                        }
                        for (p in 0..(processCount * messageCount - 1)) {
                            if (p % comm.size != comm.id) {      //Don't send to yourself
                                messenger.sendTask(p % comm.size, TestMessage(comm.id))
                            }
                        }
                        globalBarrier.await()
                        messenger.close()
                    }
                })
            }.map { it.second.join(); it.first.finalize() }
        }
    }

    @Test(timeout = 10000)
    fun complexTest() {
        //WARNING: this can actually take a while (Like 7s on a 2ghz dual core)

        //Initialize 10 * processCount floods with various lifespans and send them to random receivers.
        //If you receive positive flood message, pass it to next random process.
        //Since we can't send messages to ourselves, for two processes, this is completely deterministic.

        for (a in 1..repetitions) { //Repeat this a lot and hope for the best!

            val terminators = createSharedMemoryTerminators(processCount)

            val allMessages = communicatorConstructor(processCount).zip(terminators).map { it ->

                val (comm, term) = it

                fun randomReceiver(): Int {
                    var receiver = comm.id
                    while (receiver == comm.id) {
                        receiver = (Math.random() * this.processCount).toInt()
                    }
                    return receiver
                }

                val received = ArrayList<TestMessage>()
                val sent = HashMap((1..comm.size).toMap({it - 1}, { ArrayList<TestMessage>() }))

                val worker = thread {
                    for (i in 1..5) {   //Create more messengers in a row in order to fully test the communicator

                        val terminator = term.createNew()

                        var doneSending = false

                        val messenger = comm.listenTo(TestMessage::class.java) {
                            synchronized(received) { received.add(it) }
                            terminator.messageReceived()
                            if (it.number > 0) {
                                val receiver = randomReceiver()
                                val message = TestMessage(it.number - 1)
                                synchronized(sent) { sent[receiver]!!.add(message) }
                                terminator.messageSent()
                                this.sendTask(receiver, message)
                            }
                            synchronized(doneSending) {
                                if (doneSending) terminator.setDone()
                            }
                        }

                        for (p in 1..(processCount * 10)) {
                            val receiver = randomReceiver()
                            val message = TestMessage(p)
                            terminator.messageSent()
                            synchronized(sent) { sent[receiver]!!.add(message) }
                            messenger.sendTask(receiver, message)
                        }

                        synchronized(doneSending) {
                            doneSending = true
                        }

                        terminator.waitForTermination()

                        messenger.close()
                    }
                    comm.finalize()
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