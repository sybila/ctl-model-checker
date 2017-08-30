package com.github.sybila.funn

import com.github.sybila.algorithm.BooleanLogic
import com.github.sybila.algorithm.Components
import com.github.sybila.algorithm.components.PickRandom
import com.github.sybila.algorithm.components.PivotSelector
import com.github.sybila.algorithm.makeDeferred
import com.github.sybila.collection.CollectionContext
import com.github.sybila.collection.EmptyStateMap
import com.github.sybila.collection.StateMap
import com.github.sybila.funn.ode.ODETransitionSystem
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.PathQuantifier
import com.github.sybila.huctl.dsl.Not
import com.github.sybila.huctl.dsl.and
import com.github.sybila.huctl.dsl.or
import com.github.sybila.model.TransitionSystem
import com.github.sybila.solver.Solver
import com.github.sybila.solver.grid.Grid2
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

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
class ModelChecker(
        private val model: ODETransitionSystem,
        maps: CollectionContext<Int, Grid2>,
        override val solver: Solver<Grid2>,
        override val fork: Int = Runtime.getRuntime().availableProcessors(),
        override val meanChunkTime: Long = 25,
        name: String = "MC"
) : BooleanLogic<Int, Grid2>,
        Components<Int, Grid2>,
        TransitionSystem<Int, Grid2> by model,
        CollectionContext<Int, Grid2> by maps,
        PivotSelector<Int, Grid2> by PickRandom(solver, maps)
{

    override val executor = newFixedThreadPoolContext(fork, name)


    fun check(formulas: List<Pair<String, Formula>>): List<Pair<String, StateMap<Int, Grid2>>> = runBlocking {
        val names = formulas.map { it.first }
        val toCompute = buildGraph(formulas.map { it.second })
        // At this point, once a node in the graph looses all references, it can be GC'ed.
        toCompute.forEach { it.start() }        // start all the formulas in parallel!
        names zip toCompute.map { it.await() }  // and then await that shit...
    }

    private fun buildGraph(formulas: List<Formula>): List<Deferred<StateMap<Int, Grid2>>> {
        // We gave this a special scope to make sure the reference to graph
        // does not hang around for too long.
        val graph = GraphBuilder()
        return formulas.map { graph.build(it) }
    }

    private inner class GraphBuilder {

        private val graph = HashMap<String, Deferred<StateMap<Int, Grid2>>>()

        fun build(f: Formula): Deferred<StateMap<Int, Grid2>> {
            return graph.computeIfAbsent(f.canonicalKey) {
                println("Build $f")
                when (f) {
                    is Formula.True -> makeDeferred { universe }
                    is Formula.False -> makeDeferred { EmptyStateMap<Int, Grid2>() }
                    is Formula.Not -> makeComplement(build(f.inner), makeDeferred { universe })
                    is Formula.And -> makeAnd(build(f.left), build(f.right))
                    is Formula.Or -> makeOr(build(f.left), build(f.right))
                    is Formula.Implies -> build(Not(f.left) or f.right)
                    is Formula.Equals -> build((Not(f.left) and Not(f.right)) or (f.left and f.right))
                    is Formula.Globally -> build(Not(Formula.Future(f.quantifier.invert(), Not(f.inner), f.direction)))
                    is Formula.Future -> makeReachability(build(f.inner), build(Formula.True),true)
                    else -> makeDeferred { makeProposition(f) }
                }
            }
        }

    }

    private fun PathQuantifier.invert() = when (this) {
        PathQuantifier.A -> PathQuantifier.E
        PathQuantifier.E -> PathQuantifier.A
        PathQuantifier.pA -> PathQuantifier.pE
        PathQuantifier.pE -> PathQuantifier.pA
    }

}