package com.github.sybila.checker

import com.github.daemontus.jafra.Terminator
import com.github.daemontus.jafra.Token
import org.junit.Test
import java.util.*
import java.util.concurrent.CyclicBarrier
import kotlin.test.assertEquals
import kotlin.test.assertFails

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

    override val repetitions: Int = 2
    override val processCount: Int = 12

    override val communicatorConstructor: (Int) -> List<Communicator>
            = { c -> createSharedMemoryCommunicators(c) }

}


abstract class CommunicatorTest {

    abstract val processCount: Int
    abstract val repetitions: Int
    abstract val communicatorConstructor: (Int) -> List<Communicator>


    @Test(timeout = 2000)
    fun noListenerTest() {
        val barrier = CyclicBarrier(processCount)
        communicatorConstructor(processCount).map {
            guardedThread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message!")
                }
                it.send((it.id + 1) % processCount, Token(0, 0)) //trigger error
                barrier.await()
                assertFails {
                    it.close()
                }
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun doubleRemoveListenerTest() {
        communicatorConstructor(processCount).map {
            guardedThread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message!")
                }
                it.removeListener(TestMessage::class.java)
                assertFails {
                    it.removeListener(TestMessage::class.java)
                }
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun badRecipientTest() {
        communicatorConstructor(processCount).map {
            guardedThread {
                assertFails {
                    it.send(it.id, TestMessage(0))
                }
                assertFails {
                    it.send(processCount + 1, TestMessage(0))
                }
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun doubleAddListenerTest() {
        communicatorConstructor(processCount).map {
            guardedThread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message!")
                }
                assertFails {
                    it.addListener(TestMessage::class.java) {
                        throw IllegalStateException("Unexpected message!")
                    }
                }
                it.removeListener(TestMessage::class.java)
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun forgottenListenerTest() {
        communicatorConstructor(processCount).map {
            guardedThread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message!")
                }
                assertFails {
                    it.close()
                }
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun emptyRun() {
        communicatorConstructor(processCount).map { it.close() }
    }

    @Test(timeout = 2000)
    fun oneMessengerNoMessages() {
        communicatorConstructor(processCount).map {
            guardedThread {
                it.addListener(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                it.removeListener(TestMessage::class.java)
                it.close()
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun moreMessengersNoMessages() {
        communicatorConstructor(processCount).map {
            guardedThread {
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

    @Test(timeout = 4000)
    fun oneMessengerWithMessages() {

        //This barrier will be triggered by the receiver when all messages are received and
        //by sender when all messages are sent, this way the barrier is left when
        //termination has been "detected"
        //If something goes wrong, receiver will never trigger and timeout will be reached
        val terminationBarrier = CyclicBarrier(2 * processCount)
        val initBarrier = CyclicBarrier(processCount)

        repeat(repetitions) {
            communicatorConstructor(processCount).map { comm ->

                val expected = (0..(processCount - 1))
                        .filter { it != comm.id }
                        .map { TestMessage(it) }.sorted()

                guardedThread {
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

    @Test(timeout = 20000)
    fun moreMessengersWithMessages() {
        //Always sends messageCount messages to all other processes and then closes the messenger,
        //therefore we can safely predict how the expected received messages should look.
        //(It's going to be all messages repeated i-times except for message from itself)

        repeat(repetitions) { //Repeat this a lot and hope for the best!
            val allMessages = (1..processCount).map { TestMessage(it - 1) }
            val messageCount = 10

            //This barrier will be triggered by the receiver when all messages are received and
            //by sender when all messages are sent, this way the barrier is left when
            //termination has been "detected"
            //If something goes wrong, receiver will never trigger and timeout will be reached
            val terminationBarrier = CyclicBarrier(2 * processCount)
            val initBarrier = CyclicBarrier(processCount)

            communicatorConstructor(processCount).map { comm ->
                Pair(comm, guardedThread {
                    for (iteration in 1..10) {
                        //repeat to test if we are able to recreate messengers
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
                            if (p % comm.size != comm.id) {
                                //Don't send to yourself
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
        //WARNING: This thing is a mess. Someone should seriously rethink this and rewrite it.

        //Initialize 10 * processCount floods with various lifespans and send them to random receivers.
        //If you receive positive flood message, pass it to next random process.
        //Since we can't send messages to ourselves, for two processes, this is completely deterministic.

        //Also, in the parallel, termination messages are sent using the same communicator.

        repeat(repetitions) { //Repeat this a lot and hope for the best!

            val globalBarrier = CyclicBarrier(processCount)

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

                val worker = guardedThread {
                    for (i in 1..5) {
                        //Create more messengers in a row in order to fully test the communicator

                        //save this for later ;) - after init round
                        val terminator = lazy { terminators.createNew() }

                        //this barrier will make sure that messages will be processed only after initial messages have
                        //been sent. This way we can guarantee that setDone will be called at least once
                        //(because main thread can't call it since it doesn't know when)
                        //Note: In JobQueue, this can't happen because we have a separate job buffer
                        var barrierLeft = false
                        val barrier = CyclicBarrier(2)

                        comm.addListener(TestMessage::class.java) {
                            synchronized(barrierLeft) {
                                if (!barrierLeft) {
                                    barrier.await()
                                }
                                barrierLeft = true
                            }
                            synchronized(received) { received.add(it) }
                            terminator.value.messageReceived()
                            if (it.number > 0) {
                                val receiver = randomReceiver()
                                val message = TestMessage(it.number - 1)
                                synchronized(sent) { sent[receiver]!!.add(message) }
                                terminator.value.messageSent()
                                comm.send(receiver, message)
                            }
                            terminator.value.setDone()
                        }

                        globalBarrier.await()

                        //make sure that everyone gets at least one message, otherwise the
                        //barrier won't break
                        for (p in 0 until processCount) {
                            if (p == comm.id) continue
                            val message = TestMessage(0)
                            terminator.value.messageSent()
                            synchronized(sent) { sent[p]!!.add(message) }
                            comm.send(p, message)
                        }

                        for (p in 1..(processCount * 10)) {
                            val receiver = randomReceiver()
                            val message = TestMessage(p)
                            terminator.value.messageSent()
                            synchronized(sent) { sent[receiver]!!.add(message) }
                            comm.send(receiver, message)
                        }

                        barrier.await()

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

            //For debugging: throw IllegalStateException("Transferred: ${received.values.fold(0, { f, s -> f + s.size })}")
        }

    }

}