package cz.muni.fi.checker

import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue

/**
 * NOTE: The strong/complicated semantics enforced in messenger-communicator relationship
 * is to prevent bugs that are a result of two phases of model checking algorithm interleaving
 * due to faulty synchronization.
 * This way all such problems should be easily detected, since every phase creates it's own messenger.
 */

/**
 * Messenger is responsible for sending messages of one type between processes.
 * Messenger should be active from the moment when it's returned from the factory.
 * The creation and closing of a messenger is a global synchronization event.
 * There can be more active messengers at one time, but they need to listen to different classes.
 * (Not just subclasses, the class object of message and listener has to be equal)
 */
public interface Messenger<M: Any> {

    /**
     * Send Job to process with specified ID.
     * Should not block or create unnecessary copy of messages.
     * Method should fail when sending message to itself.   (Bad design - use job queue for that)
     * Method should fail when message cannot be serialized.
     * There is no delivery notification mechanism. If message delivery fails, whole application is allowed to terminate.
     * (In other words: Channels should guarantee reliability)
     */
    fun sendTask(receiver: Int, message: M)

    /**
     * Use this method to cleanup all possible communications.
     * No tasks can be sent using a closed messenger.
     *
     * This call should be a global barrier across all processes,
     * meaning that either all processes close the messenger,
     * or no messenger is closed and the method blocks until
     * all processes reach it.
     *
     * If there are some unconsumed messages in message buffers after this operation, an exception will be thrown
     * as soon as the first one is taken from buffer. (Therefore you have to ensure proper termination before
     * closing the messengers)
     */
    fun close()

}

public interface Communicator {

    /**
     * Create new messenger instance accepting messages given by messageClass and performing onTask callback on
     * every message received (serially or in parallel).
     *
     * This way you can limit yourself only to specific types of messages.
     * WARNING: When message of unrecognized class is received, exception will be thrown.
     * When class is recognized, but there is no one to consume it,
     * it's also considered an error and exception will be also thrown.
     * For every class, a process can have only one active messenger.
     * If you try to register a messenger but there is already an active one,
     * exception should be thrown.
     *
     * You also should not create/close two messengers at the same time.
     *
     * WARNING: messageClass looses info about generics at run time!
     *
     * This call should be a global barrier across all processes,
     * meaning that either all processes create new messenger,
     * or no messenger is created and the method blocks until
     * all processes reach it. (Or all processes throw an exception in case one of the calls was invalid)
     */
    fun <M: Any> listenTo(messageClass: Class<M>, onTask: Messenger<M>.(M) -> Unit): Messenger<M>

    /**
     * Use this to clean up global environment - i.e. close MPI connections, join threads...
     */
    fun finalize()

    /**
     * Total number of participating processes
     */
    val size: Int

    /**
     * My id.
     */
    val id: Int
}

/**
 * Factory method that creates a set of in memory messenger communicators connected by BlockingQueues.
 * (Blocking queues are shared across created messengers, so that errors should appear
 * if you use multiple messengers from one process -> this should provide easy detection
 * of problems with global synchronization in main algorithm)
 *
 * Suitable for in-memory computing, but note that it's optimized for readability/testing and not speed.
 */
public fun createSharedMemoryCommunicators(
        processCount: Int
): List<Communicator> {

    //Global environment -> queues, barrier
    val queues = (1..processCount).map { LinkedBlockingQueue<Maybe<Any>>() }
    val barrier = CyclicBarrier(processCount)

    return (0..(processCount - 1)).map { id -> object : Communicator {

        //Lock that guards all state variables of this communicator and underlying messengers
        private val commLock = Object()

        private val messengers = HashMap<Class<*>, (Any) -> Unit>()

        val messageListener = queues[id].threadUntilPoisoned { message ->
            synchronized(commLock) {
                val receiver = messengers[message.javaClass]
                if (receiver != null) {
                    receiver(message)
                } else {
                    throw IllegalStateException("Received message of class ${message.javaClass} but no listener was " +
                            "found. Active listeners: ${messengers.keys}")
                }
            }
        }

        override fun <M : Any> listenTo(messageClass: Class<M>, onTask: Messenger<M>.(M) -> Unit): Messenger<M> {
            synchronized(commLock) {
                if (messengers.containsKey(messageClass)) {
                    throw IllegalStateException("Messenger for $messageClass already exist in $id, close it first")
                }

                //Access to barrier is guarded by commLock, so that one communicator can't count "twice" in same barrier round
                barrier.await()

                val messenger = object : Messenger<M> {

                    override fun sendTask(receiver: Int, message: M) {
                        if (receiver == id) throw IllegalArgumentException("Sending message to yourself!")
                        queues[receiver].put(Maybe.Just(message))
                    }

                    override fun close() {
                        synchronized(commLock) {
                            barrier.await()
                            messengers.remove(messageClass)
                            barrier.await()
                        }
                    }

                }

                messengers[messageClass] = {
                    @Suppress("UNCHECKED_CAST")
                    messenger.onTask(it as M)
                }

                //We need two barriers, because all messengers have to be already constructed
                //when returned to the outside world
                barrier.await()

                return messenger
            }
        }


        override fun finalize() {
            synchronized(commLock) {
                if (messengers.isNotEmpty()) {
                    throw IllegalStateException("Finalizing with unclosed messengers for ${messengers.keys}}")
                }
            }
            if (queues[id].isNotEmpty()) {
                throw IllegalStateException("Finalizing with unconsumed messages ${queues[id]}")
            }
            queues[id].put(Maybe.Nothing())
            messageListener.join()
        }

        override val size: Int = processCount
        override val id: Int = id

    } }

}