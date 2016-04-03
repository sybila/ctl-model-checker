package com.github.sybila.checker

import com.github.daemontus.egholm.concurrent.guardedThreadUntilPoisoned
import com.github.daemontus.egholm.concurrent.poison
import com.github.daemontus.egholm.functional.Maybe
import com.github.daemontus.egholm.logger.lFine
import com.github.daemontus.egholm.logger.lFiner
import com.github.daemontus.egholm.logger.lFinest
import com.github.daemontus.jafra.IdTokenMessenger
import com.github.daemontus.jafra.Token
import java.io.Closeable
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import kotlin.properties.Delegates


/**
 * Low level communication primitive.
 *
 * When using communicator, you are responsible for your synchronisation.
 * Communicator only guarantees message delivery and order preservation.
 *
 * Creation of a communicator should be a global synchronisation event.
 * That way, you can guarantee no one will be able to send messages before everyone is listening.
 */
interface Communicator: Closeable {

    /**
     * My id.
     */
    val id: Int

    /**
     * Total number of participating processes
     */
    val size: Int

    /**
     * Add onTask listener that will be fired every time message of given class is received.
     * (Serially or in parallel, depends on implementation)
     *
     * Only one listener can be active for one message class at the same time. Attempt to add
     * multiple listeners will result in an exception.
     *
     * Note that this call is in NO WAY synchronized with other processes.
     */
    fun <M: Any> addListener(messageClass: Class<M>, onTask: (M) -> Unit)

    /**
     * Remove active listener.
     * Throws exception if no such listener is active.
     * Also provides no global synchronization or termination detection.
     */
    fun removeListener(messageClass: Class<*>)

    /**
     * Async send.
     * Should fail in case you are sending message to yourself - use job queue for that!
     */
    fun send(dest: Int, message: Any)

    //Communication statistics
    var sendCount: Long
    var sendSize: Long
    var receiveCount: Long
    var receiveSize: Long

}

class SharedMemoryCommunicator(
        override val id: Int,
        override val size: Int,
        private val channels: Array<BlockingQueue<Maybe<Any>>?>,
        initialListeners: Map<Class<*>, (Any) -> Unit> = mapOf(),
        private val logger: Logger = Logger.getLogger(SharedMemoryCommunicator::class.java.canonicalName+"#$id")
) : Communicator {

    override var sendCount = 0L
    override var receiveCount = 0L
    //we don't provide info about message size
    override var sendSize = -1L
    override var receiveSize = -1L

    private val listeners = HashMap<Class<*>, (Any) -> Unit>(initialListeners)

    private val channelListener = channels[id]!!.guardedThreadUntilPoisoned {
        logger.lFinest { "Received $it" }
        val listener = synchronized(listeners) {
            listeners[it.javaClass]
        } ?: throw IllegalStateException("Message with no listener received! $id $it - listeners: $listeners")
        receiveCount += 1
        listener(it)
    }

    override fun <M : Any> addListener(messageClass: Class<M>, onTask: (M) -> Unit) {
        logger.lFine { "Adding listener: $messageClass" }
        synchronized(listeners) {
            @Suppress("UNCHECKED_CAST") //Cast is ok, we just have to get rid of the type in the map.
            val previous = listeners.put(messageClass, onTask as (Any) -> Unit)
            if (previous != null) {
                throw IllegalStateException("Replacing already present listener: $id, $messageClass")
            }
        }
    }

    override fun removeListener(messageClass: Class<*>) {
        logger.lFine { "Removing listener: $messageClass" }
        synchronized(listeners) {
            listeners.remove(messageClass) ?: throw IllegalStateException("Removing non existent listener: $id, $messageClass")
        }
    }

    override fun send(dest: Int, message: Any) {
        logger.lFinest { "Sending to $dest: $message" }
        if (dest == id) throw IllegalArgumentException("Can't send message to yourself")
        channels[dest]!!.put(Maybe.Just(message))
        sendCount += 1
    }

    //checks if all messages have been delivered
   override fun close() {
        logger.lFiner { "Attempting to close communicator" }
        val queue = channels[id]
        channels[id] = null
        if (queue == null) {
            throw IllegalStateException("Closing a communicator that is already closed")
        }
        if (queue.isNotEmpty()) {
            throw IllegalStateException("Unconsumed messages in queue before closing! ${queue.first()}")
        }
        synchronized(listeners) {
            if (listeners.isNotEmpty()) {
                throw IllegalStateException("Someone is still listening! $listeners")
            }
        }
        queue.poison()
        channelListener.join()
        logger.lFine { "Communicator closed" }
    }

}

//adapter between token messenger from Jafra and our communicator
class CommunicatorTokenMessenger(
        id: Int, size: Int
) : IdTokenMessenger(id, size), Closeable, (Token) -> Unit {

    var comm: Communicator by Delegates.notNull()

    private val pending = LinkedBlockingQueue<Token>()

    override fun invoke(p1: Token) {
        pending.add(p1)
    }

    override fun sendTokenAsync(destination: Int, token: Token) {
        if (destination == comm.id) {
            pending.add(token)
        } else {
            comm.send(destination, token)
        }
    }

    override fun waitForToken(source: Int): Token = pending.take()

    override fun close() {
        comm.removeListener(Token::class.java)
    }

}