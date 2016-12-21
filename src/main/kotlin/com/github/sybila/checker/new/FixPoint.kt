package com.github.sybila.checker.new

import java.nio.ByteBuffer
import java.util.*


abstract class GreatestFixPoint<Colors>(
        initial: StateMap<Colors>,
        comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : FixPoint<Colors>(comm, solver, fragment) {

    init {
        for (state in initial) {
            localData[state] = initial[state]
            enqueue(state)
        }
    }

    override fun asStateMap(): StateMap<Colors> = localData.asStateMap(ff)

    override fun onUpdate(state: Int, value: Colors) = onAdded(state, value)

    protected fun add(state: Int, value: Colors): Boolean = update(state, value)

    abstract fun onAdded(state: Int, value: Colors)
}

/**
 * Warning: Smallest fix point stores the colors which DON'T belong to the final result
 * and then inverts them in the end.
 *
 * Also note that removed value can be false. This is needed to check directional condition.
 */
abstract class SmallestFixPoint<Colors>(
        initial: StateMap<Colors>,
        all: StateMap<Colors>,
        comm: Comm<Colors>, solver: Solver<Colors>, fragment: Fragment<Colors>
) : FixPoint<Colors>(comm, solver, fragment) {

    init {
        for (state in all) {
            @Suppress("LeakingThis")    //the only leaked thing is solver, which is already constructed
            localData[state] = all[state] and initial[state].not()
            enqueue(state)
        }
    }

    override fun asStateMap(): StateMap<Colors> {
        return localData.mapValues { it.value.not() }.asStateMap(ff)
    }

    override fun onUpdate(state: Int, value: Colors) = onRemoved(state, value)

    protected fun remove(state: Int, value: Colors): Boolean = update(state, value)

    abstract fun onRemoved(state: Int, value: Colors)
}

/**
 * An abstract representation of a value that is computed iteratively and distributively as a fix point.
 *
 * Communication is provided by a communicator, which has one synchronization point
 * where all fix points exchange messages using a map/reduce scheme (first, all fix points perform
 * a map operation which constructs and distributes the messages, then the reduce will gather all messages
 * into the main queue of each fix point).
 *
 * Both Smallest and Greatest fix point are computed using accumulation, but smallest fix point has
 * negation included in the beginning and
 *
 * Use the get/update functions to retrieve and modify content stared in this fix point.
 * Use sync to signalize that a specific value should be transmitted to other partitions
 * (this is mainly to reduce communication overhead when not needed)
 */
abstract class FixPoint<Colors>(
        private val comm: Comm<Colors>,
        solver: Solver<Colors>,
        fragment: Fragment<Colors>
) : Fragment<Colors> by fragment, Solver<Colors> by solver {

    private val queue = ArrayDeque<Int>()
    protected val localData = HashMap<Int, Colors>()
    protected val remoteData = HashMap<Int, Colors>()

    //comm primitives
    private val sync = Array(comm.size) { HashSet<Int>() }
    private val incoming = arrayOfNulls<ByteBuffer>(comm.size)

    abstract fun onUpdate(state: Int, value: Colors)
    abstract fun asStateMap(): StateMap<Colors>

    fun update(state: Int, value: Colors): Boolean {
        val storage = if (state.owner() == id) localData else remoteData
        val current = storage[state]
        return if (current == null) {
            storage[state] = value
            enqueue(state)
            true
        } else if (value andNot current) {
            storage[state] = value or current
            enqueue(state)
            true
        } else false
    }

    fun computeFixPoint(): FixPoint<Colors> {
        do {
            while (queue.isNotEmpty()) {
                val state = queue.remove()
                onUpdate(state, get(state))
            }
            //println("Sync")
        } while (comm.synchronize(this))
        return this
    }

    protected fun get(state: Int): Colors {
        return (if (state.owner() == id) localData[state] else remoteData[state]) ?: ff
    }

    protected fun sync(state: Int, to: Int) {
        if (state.owner() != id && state.owner() != to) {
            throw IllegalStateException("Invalid sync operation: Sender $id nor receiver $to own node $state owned by ${state.owner()}")
        }
        sync[to].add(state)
    }

    protected fun enqueue(state: Int) {
        queue.add(state)
    }

    fun map(fixPoints: List<FixPoint<Colors>>) {
        for (remoteId in 0 until comm.size) {
            val worker = fixPoints[remoteId]
            val toSync = sync[remoteId]
            val transmission = toSync.map { it to get(it).minimize() }
            toSync.clear()
            val bufferSize = transmission.fold(4) { a, i -> a + 4 + i.second.byteSize() }
            //println("Transmission: ${transmission} with size $bufferSize")
            if (transmission.isNotEmpty()) {
                val buffer = obtainBuffer(bufferSize)
                buffer.clear()
                buffer.putInt(transmission.size)
                transmission.forEach {
                    buffer.putInt(it.first)
                    buffer.putColors(it.second)
                }
                buffer.flip()
                worker.incoming[id] = buffer
            } else {
                worker.incoming[id] = null
            }
        }
    }

    fun reduce(): Boolean {
        return incoming.fold(false) { received, buffer ->
            (buffer?.let { buffer ->
                repeat(buffer.int) {
                    update(buffer.int, buffer.getColors())
                }
                buffer.flip()
                recycleBuffer(buffer)
                true
            } ?: false) || received
        }
    }

}
