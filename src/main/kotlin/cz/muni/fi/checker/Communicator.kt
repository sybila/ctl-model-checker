package cz.muni.fi.checker

import com.github.daemontus.jafra.IdTokenMessenger
import com.github.daemontus.jafra.Token
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


/**
 * Low level communication primitive.
 *
 * When using communicator, you are responsible for your synchronization.
 * Communicator only guarantees message delivery and order preservation.
 */
interface Communicator {

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

    /**
     * Use this to finish all communication - algorithm does not require this, but it's useful for testing :)
     */
    fun close()
}

class SharedMemoryCommunicator(
        override val id: Int,
        override val size: Int,
        private val channels: Array<BlockingQueue<Maybe<Pair<Class<*>, Any>>>?>
) : Communicator {

    private val listeners = HashMap<Class<*>, (Any) -> Unit>()

    private val channelListener = channels[id]!!.threadUntilPoisoned {
        val listener = synchronized(listeners) {
            listeners[it.first]
        } ?: throw IllegalStateException("Message with no listener received! $id $it - listeners: $listeners")
        listener(it.second)
    }

    override fun <M : Any> addListener(messageClass: Class<M>, onTask: (M) -> Unit) {
        synchronized(listeners) {
            @Suppress("UNCHECKED_CAST") //Cast is ok, we have to get rid of the type in the map.
            val previous = listeners.put(messageClass, onTask as (Any) -> Unit)
            if (previous != null) throw IllegalStateException("Replacing already present listener: $id, $messageClass")
        }
    }

    override fun removeListener(messageClass: Class<*>) {
        synchronized(listeners) {
            listeners.remove(messageClass) ?: throw IllegalStateException("Removing non existent listener: $id, $messageClass")
        }
    }

    override fun send(dest: Int, message: Any) {
        channels[dest]!!.put(Maybe.Just(Pair(message.javaClass, message)))
    }

    /**
     * Call this only after all messages have been delivered!
     */
   override fun close() {
        val queue = channels[id]
        channels[id] = null
        if (queue!!.isNotEmpty())
            throw IllegalStateException("Unconsumed messages in queue before closing! ${queue.first()}")
        synchronized(listeners) {
            if (listeners.isNotEmpty())
                throw IllegalStateException("Someone is still listening! $listeners")
        }
        queue.poison()
        channelListener.join()
    }

}

fun createSharedMemoryCommunicators(
        processCount: Int
): List<Communicator> {

    //Array of queues that deliver message and it's class and can be poisoned.
    //Nullable since we need to indicate communicator closing.
    val queues = Array<BlockingQueue<Maybe<Pair<Class<*>, Any>>>?>(processCount, {
        LinkedBlockingQueue<Maybe<Pair<Class<*>, Any>>>()
    })

    return (0 until processCount).map {
        SharedMemoryCommunicator(it, processCount, queues)
    }
}

class CommunicatorTokenMessenger(
        private val comm: Communicator
) : IdTokenMessenger(comm.id, comm.size) {

    private val pending = LinkedBlockingQueue<Token>()

    init {
        comm.addListener(Token::class.java) { pending.add(it) }
    }

    override fun sendTokenAsync(destination: Int, token: Token) = comm.send(destination, token)

    override fun waitForToken(source: Int): Token = pending.take()

    fun close() {
        comm.removeListener(Token::class.java)
    }

}