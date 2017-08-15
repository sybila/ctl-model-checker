package com.github.sybila.funn

import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.dsl.Not
import com.github.sybila.huctl.dsl.and
import com.github.sybila.huctl.dsl.or
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Broad overview of what is going on here:
 *
 * First, a the formula is turned into a dependency graph of operators ([Deferred] objects).
 * The graph is created in a way that makes sure vertices which are not needed for further
 * computation can be garbage collected.
 *
 * Then the graph is evaluated. This involves some intricate load balancing, since we want to
 * compute independent operators in parallel, but also split the more complex operators into
 * parallel chunks.
 *
 * The parallelism works like this:
 *  - All user requested formulas are requested for concurrent computation (hopefully there won't be 10k of them)
 *  - Once the formula is started, it starts (and awaits) its dependencies.
 *      - If it's a standard formula, it just runs all the dependencies at once.
 *      - If it's a hybrid formula, it starts at most [parallelism] dependencies in order not to
 *      overload the scheduler.
 *  - When the dependencies are computed and the formula can actually do some computing, it divides the
 *  computation into chunks of work which are executed in parallel, but the chunk size changes dynamically
 *  so as to keep one chunk execution time at approx. [chunkTime]ms (of course, the formula does not know
 *  how long the next chunk will take, but if the behaviour is somewhat regular, that should be enough)
 */
class ModelChecker<S, P>(
        private val model: TransitionSystem<S, P>,
        private val solverFactory: () -> Solver<P>,
        private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
        private val name: String = "MC",
        private val chunkTime: Long = 500
) {

    private val executor = newFixedThreadPoolContext(parallelism, name)
    private val solver = ThreadLocal.withInitial(solverFactory)

    fun check(formulas: List<Pair<String, Formula>>): List<Pair<String, StateMap<S, P>>> = runBlocking {
        val names = formulas.map { it.first }
        val toCompute = buildGraph(formulas.map { it.second })
        // At this point, once a node in the graph looses all references, it can be GC'ed.
        toCompute.forEach { it.start() }        // start all the formulas in parallel!
        names zip toCompute.map { it.await() }  // and then await that shit...
    }

    private fun buildGraph(formulas: List<Formula>): List<Deferred<StateMap<S, P>>> {
        // We gave this a special scope to make sure the reference to graph
        // does not hang around for too long.
        val graph = GraphBuilder()
        return formulas.map { graph.build(it) }
    }

    //private fun makeNegation(inner: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> =

    private inner class GraphBuilder {

        private val graph = HashMap<String, Deferred<StateMap<S, P>>>()

        fun build(f: Formula): Deferred<StateMap<S, P>> {
            return graph.computeIfAbsent(f.canonicalKey) {
                when (f) {
                    is Formula.True -> lazyAsync { model.fullMap }
                    is Formula.False -> lazyAsync { model.emptyMap }
                    is Formula.Not -> makeNot(build(f.inner))
                    is Formula.And -> makeAnd(build(f.left), build(f.right))
                    is Formula.Or -> makeOr(build(f.left), build(f.right))
                    is Formula.Implies -> build(Not(f.left) or f.right)
                    is Formula.Equals -> build((Not(f.left) and Not(f.right)) or (f.left and f.right))
                    else -> lazyAsync { model.makeProposition(f) }
                }
            }
        }

    }

    fun makeNot(innerJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = lazyAsync {
        val inner = innerJob.await()
        val result = model.mutate(model.fullMap)
        inner.states.toList().parallelChunks { s ->
            result[s] = (ONE - inner[s]).takeUnless { it.isZero() }
        }
        result
    }

    fun makeAnd(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = lazyAsync {
        val left = leftJob.await()
        val right = rightJob.await()
        val result = model.mutate(left)
        left.states.toList().parallelChunks { s ->
            result[s] = (left[s] * right[s]).takeUnless { it.isZero() }
        }
        result
    }

    fun makeOr(leftJob: Deferred<StateMap<S, P>>, rightJob: Deferred<StateMap<S, P>>): Deferred<StateMap<S, P>> = lazyAsync {
        val left = leftJob.await()
        val right = rightJob.await()
        val result = model.mutate(left)
        right.states.toList().parallelChunks { s ->
            result[s] = left[s] + right[s]  // this actually can't be zero if left or right is not zero
        }
        result
    }

    private fun <T> lazyAsync(block: suspend CoroutineScope.() -> T): Deferred<T>
            = async(executor, CoroutineStart.LAZY, block)

    // Split the list into chunks which will be processed by given action in parallel.
    // The size of the chunks changes dynamically based on the complexity of taken actions.
    // Use given channel to further communicate the results of the actions.
    private suspend fun <T> List<T>.parallelChunks(action: Solver<P>.(T) -> Unit): Unit {
        val chunkSize = AtomicInteger(1)
        val chunks = produce<List<T>>(executor) {
            val original = this@parallelChunks
            var chunkStart = 0  //inclusive
            while (chunkStart != original.size) {
                val chunk = chunkSize.get()
                val chunkEnd = Math.min(original.size, chunkStart + chunk)  //exclusive
                send(original.subList(chunkStart, chunkEnd))
                chunkStart = chunkEnd
            }
            original.take(10)
            close()
        }
        (1..parallelism).map {
            async(executor) {
                chunks.consumeEach { items ->
                    val solver = solver.get()
                    val elapsed = measureTimeMillis {
                        items.forEach { solver.action(it) }
                    }
                    if (elapsed < 0.8 * chunkTime || elapsed > 1.2 * chunkTime) {
                        val chunk = items.size
                        val itemTime = elapsed / chunk.toDouble()
                        if (itemTime == 0.0) {
                            // If we are real fast, we can get a zero elapsed time and hence a zero item time
                            chunkSize.set(2 * chunk)    // in which case, just increase the chunk arbitrarily.
                        } else {
                            // On the other hand, we can also be really slow and get a zero chunk size
                            chunkSize.set(Math.max(1, (chunkTime / itemTime).toInt()))  // hence use a lower bound.
                        }
                    }
                }
            }
        }.forEach { it.await() }
    }


}