package com.github.sybila.checker.new

import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.True
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * State is just an integer ID
 */

/**
 * All color related actions are part of the solver.
 *
 * This provides a way to add decoupled caching and
 * other advanced options without complicating the
 * colors interface implementation.
 */
interface Solver<Colors> {

    val tt: Colors
    val ff: Colors

    /**
     * Logical conjunction.
     *
     * Complexity: constant
     */
    infix fun Colors.and(other: Colors): Colors

    /**
     * Logical disjunction.
     *
     * Complexity: constant
     */
    infix fun Colors.or(other: Colors): Colors

    /**
     * Logical negation.
     *
     * Complexity: constant
     */
    fun Colors.not(): Colors

    /**
     * Emptiness check.
     *
     * Complexity: exponential
     */
    fun Colors.isEmpty(): Boolean
    fun Colors.isNotEmpty(): Boolean = !this.isEmpty()

    /**
     * True if A > B (exists e in A: e not in B)
     *
     * Complexity: exponential
     */
    infix fun Colors.superset(other: Colors): Boolean = this.and(other.not()).isNotEmpty()

    /**
     * Try to reduce the size of the set representation.
     *
     * Complexity: exponential
     */
    fun Colors.minimize(): Colors

    /**
     * Return required number of bytes to serialize this object.
     *
     * Complexity: constant
     */
    fun Colors.byteSize(): Int

    /**
     * Write this object into a byte buffer.
     */
    fun ByteBuffer.putColors(colors: Colors): ByteBuffer
    fun ByteBuffer.getColors(): Colors

}

data class Transition<out Colors>(
        val target: Int,
        val direction: DirectionFormula.Atom.Proposition,
        val bound: Colors
)

interface PartitionFunction {
    val id: Int
    fun Int.owner(): Int
}

interface Fragment<Colors> : PartitionFunction {

    fun step(from: Int, future: Boolean): Iterator<Transition<Colors>>

    fun eval(atom: Formula.Atom): StateMap<Colors>

}

interface StateMap<Colors> : Iterable<Int> {
    operator fun get(state: Int): Colors
    operator fun contains(state: Int): Boolean
}


/**
 * Very slow, but very precise equality operation - use only for tests.
 */
fun <Colors> StateMap<Colors>.deepEquals(right: StateMap<Colors>, solver: Solver<Colors>): Boolean {
    val left = this
    return solver.run {
        (left + right).all {
            val l = left[it]
            val r = right[it]
            !(l superset r) && !(r superset l)
        }
    }
}

fun <Colors> Map<Int, Colors>.asStateMap(default: Colors): StateMap<Colors> = MapStateSet(default, this)

private data class MapStateSet<Colors>(val default: Colors, val map: Map<Int, Colors>) : StateMap<Colors> {
    override fun iterator(): Iterator<Int> = map.keys.iterator()
    override fun get(state: Int): Colors = map[state] ?: default
    override fun contains(state: Int): Boolean = state in map
}

class Checker<Colors>(
        private val setup: List<Pair<Fragment<Colors>, Solver<Colors>>>
) {

    private val barrier = CyclicBarrier(setup.size)
    private val executor = Executors.newCachedThreadPool()

    fun verify(formula: Formula): List<StateMap<Colors>> {
        synchronized(this) {    //only one verification allowed at a time
            return setup.map {
                Worker(it.first, it.second, setup.size)
            }.map {
                executor.submit(Callable<StateMap<Colors>> {
                    it.verify(formula, mapOf())
                })
            }.map { it.get() }
        }
    }

    private val waiting = arrayOfNulls<Worker>(setup.size)
    private val done = AtomicInteger(0)

    internal fun synchronize(result: HashMap<Int, Colors>, op: Worker): Boolean {
        waiting[op.id] = op
        barrier.await()
        //make local copy
        val fixPoints = waiting.toList().requireNoNulls()
        //exchange messages
        op.sendData(result, fixPoints)
        done.set(0)
        barrier.await()
        if (op.receiveData(result)) done.incrementAndGet()
        barrier.await()
        return done.get() == 0
    }

    inner class Worker(
            val fragment: Fragment<Colors>,
            val solver: Solver<Colors>,
            val workerCount: Int
    ) : Fragment<Colors> by fragment, Solver<Colors> by solver {

        private val incoming = arrayOfNulls<ByteBuffer>(workerCount)
        private val notification = Array(workerCount) { HashSet<Int>() }

        fun verify(formula: Formula, assignment: Map<String, Int>): StateMap<Colors> {
            return when (formula) {
                is Formula.Atom -> eval(formula)
                is Formula.Not -> {
                    val inner = verify(formula.inner, assignment)
                    val all = eval(True)
                    all.asSequence()
                            .map {
                                it to if (it in inner) { all[it] and inner[it].not() } else all[it]
                            }
                            .filter { it.second.isNotEmpty() }
                            .toMap()
                            .asStateMap(ff)
                }
                is Formula.Bool<*> -> {
                    val left = verify(formula.left, assignment)
                    val right = verify(formula.right, assignment)
                    @Suppress("USELESS_CAST")   //not s useless after all...
                    when (formula as Formula.Bool<*>) {
                        is Formula.Bool.And -> {
                            left.asSequence()
                                    .filter { it in right }
                                    .map { it to (left[it] and right[it]) }
                                    .filter { it.second.isNotEmpty() }
                        }
                        is Formula.Bool.Or -> {
                            (left + right).toSet().asSequence()
                                    .map { it to when {
                                        it in left && it in right -> (left[it] or right[it])
                                        it in left -> left[it]
                                        else -> right[it]
                                    } }
                        }
                        is Formula.Bool.Implies -> {
                            (left + right).toSet().asSequence()
                                    .map {
                                        it to when {
                                            it in left && it in right -> (left[it].not() or right[it])
                                            it in right -> right[it]
                                            else -> left[it].not()
                                        }
                                    }
                        }
                        is Formula.Bool.Equals -> {
                            (left + right).toSet().asSequence()
                                    .map {
                                        it to ((left[it] and right[it]) or (left[it].not() and right[it].not()))
                                    }
                                    .filter { it.second.isNotEmpty() }
                        }
                    }.toMap().asStateMap(ff)
                }
                else -> throw IllegalStateException("Unsupported formula $formula")
            }
        }

        private fun fixpoint(result: HashMap<Int, Colors>, onChange: (Int) -> Unit) {
            do {
                while (queue.isNotEmpty()) onChange(queue.remove())
            } while (synchronize(result, this))
        }

        /* Map manipulation */

        private fun HashMap<Int, Colors>.add(target: Int, value: Colors) {
            this.compute(target) { k,v ->
                if (v != null) v.or(value) else value
            }
        }

        /* Queue manipulation functions */

        private val queue = ArrayDeque<Int>()

        fun HashMap<Int, Colors>.push(source: Int, target: Int, value: Colors) {
            if (target.owner() == id) {
                this.enqueue(target, value)
            } else {
                notification[target.owner()].add(source)
            }
        }

        private fun HashMap<Int, Colors>.enqueue(target: Int, value: Colors) {
            this.add(target, value)
            queue.add(target)
        }

        /* Communication */

        fun sendData(result: HashMap<Int, Colors>, workers: List<Worker>) {
            for (target in 0 until workerCount) {
                val worker = workers[target]
                val notify = notification[target]
                val transmission = notify.map {
                    it to result[target]!!.minimize()
                }
                val bufferSize = transmission.fold(0) { a, i -> 4 + i.second.byteSize() }
                if (bufferSize > 0) {
                    val buffer = obtainBuffer(bufferSize)
                    buffer.clear()
                    buffer.putInt(transmission.size)
                    transmission.forEach {
                        buffer.putInt(it.first)
                        buffer.putColors(it.second)
                    }
                    buffer.flip()
                    worker.incoming[id] = buffer
                }
            }
        }

        //true if some data were received
        fun receiveData(result: HashMap<Int, Colors>): Boolean {
            var received = false
            for (buffer in incoming) {
                if (buffer != null) {
                    received = true
                    val size = buffer.int
                    repeat(size) {
                        val state = buffer.int
                        val colors = buffer.getColors()
                        result.enqueue(state, colors)
                    }
                    buffer.flip()
                    recycleBuffer(buffer)
                }
            }
            return received
        }

    }

}