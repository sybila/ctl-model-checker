package com.github.sybila.sharedmem

import com.github.sybila.checker.Model
import com.github.sybila.checker.Solver
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.PathQuantifier
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ColouredGraph<P: Any>(
        private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
        private val model: Model<P>,
        private val solver: Solver<P>
) : Closeable {

    val pool: ExecutorService = Executors.newWorkStealingPool(parallelism)//.newFixedThreadPool(parallelism)

    fun parallel(action: () -> Unit) {
        (1..parallelism).map {
            pool.submit(action)
        }.map { it.get() }
    }

    fun <T, R> List<T>.mapParallel(action: (T) -> R): List<R> {
        return this.map {
            pool.submit<R> { action(it) }
        }.map { it.get() }
    }

    override fun close() {
        pool.shutdownNow()
    }

    val stateCount = model.stateCount

    private fun newMap(): StateMap<P> = StateMap(stateCount, solver)

    private fun StateMap<P>.reachForward(guard: StateMap<P>? = null): StateMap<P> {
        val shouldUpdate = RepeatingConcurrentStateQueue(stateCount)
        val result = newMap()
        // init reach
        for (s in 0 until stateCount) {
            val c = this.getOrNull(s)
            if (c != null) {
                result.union(s, this.get(s))
                shouldUpdate.set(s)
            }
        }
        println("Start reach forward.")
        // repeat
        parallel {
            var state = shouldUpdate.next(0)
            while (state > -1) {
                while (state > -1) {
                    // go through all neighbours
                    model.run {
                        state.successors(true).forEach { (source, _, edgeParams) ->
                            solver.run {
                                // bring colors from source state, bounded by guard
                                val bound = if (guard == null) result.get(state) else {
                                    result.get(state) and guard.get(source)
                                }
                                // update target -> if changed, mark it as working
                                val changed = result.union(source, edgeParams and bound)
                                if (changed) {
                                    shouldUpdate.set(source)
                                }
                            }
                        }
                    }

                    state = shouldUpdate.next(state + 1)
                }
                state = shouldUpdate.next(0)
            }
        }

        return result
    }

    private fun StateMap<P>.reachBackward(guard: StateMap<P>? = null): StateMap<P> {
        val shouldUpdate = RepeatingConcurrentStateQueue(stateCount)
        val result = newMap()
        // init reach
        for (s in 0 until stateCount) {
            val c = this.getOrNull(s)
            if (c != null) {
                result.union(s, this.get(s))
                shouldUpdate.set(s)
            }
        }
        kotlin.io.println("Start reach backward.")
        // repeat
        parallel {
            var state = shouldUpdate.next(0)
            while (state > -1) {
                while (state > -1) {
                    // go through all neighbours
                    model.run {
                        state.predecessors(true).forEach { (source, _, edgeParams) ->
                            solver.run {
                                // bring colors from source state, bounded by guard
                                val bound = if (guard == null) result.get(state) else {
                                    result.get(state) and guard.get(source)
                                }
                                // update target -> if changed, mark it as working
                                val changed = result.union(source, edgeParams and bound)
                                if (changed) {
                                    shouldUpdate.set(source)
                                }
                            }
                        }
                    }
                    state = shouldUpdate.next(state + 1)
                }
                // double check - maybe someone added another thing
                state = shouldUpdate.next(0)
            }
        }

        return result
    }

    fun checkCTLFormula(formula: Formula): StateMap<P> = when (formula) {
        is Formula.Atom.Float -> model.run {
            val result = newMap()
            formula.eval().entries().forEach { (s, p) ->
                result.union(s, p)
            }
            result
        }
        is Formula.Until -> {
            if (formula.quantifier != PathQuantifier.E) error("Unknown $formula")
            val reach = checkCTLFormula(formula.reach)
            val path = checkCTLFormula(formula.path)
            reach.reachBackward(path)
        }
        is Formula.Simple.Future -> {
            if (formula.quantifier != PathQuantifier.E) error("Unknown $formula")
            checkCTLFormula(formula.inner).reachBackward()
        }
        is Formula.Bool.And -> {
            val result = newMap()
            val left = checkCTLFormula(formula.left)
            val right = checkCTLFormula(formula.right)
            (0 until stateCount).toList().mapParallel { s ->
                solver.run {
                    result.union(s, left.get(s) and right.get(s))
                }
            }
            result
        }
        is Formula.Not -> checkCTLFormula(formula.inner).invert()
        else -> error("Unknown $formula")
    }

    private fun StateMap<P>.subtract(that: StateMap<P>): StateMap<P> {
        val result = newMap()
        solver.run {
            (0 until stateCount).toList()
                    .map { s -> Triple(s, get(s), that.get(s)) }
                    .mapParallel { (s, a, b) ->
                        (s to (a and b.not()) ).takeIf { it.second.isSat() }
                    }
                    .filterNotNull()
                    .forEach { (s, p) ->
                        result.union(s, p)
                    }
            return result
        }
    }

    private fun StateMap<P>.invert(): StateMap<P> {
        val result = newMap()
        solver.run {
            (0 until stateCount).toList()
                    .map { s -> s to get(s) }
                    .mapParallel { (s, c) ->
                        (s to c.not() ).takeIf { it.second.isSat() }
                    }
                    .filterNotNull()
                    .forEach { (s, p) ->
                        result.union(s, p)
                    }
            return result
        }
    }

    fun findComponents(onComponents: (StateMap<P>) -> Unit) = solver.run {
        // First, detect all sinks - this will prune A LOT of state space...
        val sinks = newMap()
        println("Detecting sinks!")
        (0 until stateCount).toList().mapParallel { s ->
            if (s%10000 == 0) println("Sink progress $s/$stateCount")
            val hasNext = model.run {
                s.successors(true).asSequence()
                        .filter { it.target != s }.map { it.bound }
                        .toList().merge { a, b -> a or b }
            }
            val isSink = hasNext.not()
            if (isSink.isSat()) {
                sinks.union(s, isSink)
                val map = newMap()
                map.union(s, isSink)
                onComponents(map)
            }
        }
        val canReachSink = sinks.reachBackward()
        //val canReachSink = newMap()
        val workQueue = ArrayList<StateMap<P>>()
        val groundZero = canReachSink.invert()
        if (groundZero.size > 0) workQueue.add(groundZero)
        while (workQueue.isNotEmpty()) {
            val universe = workQueue.removeAt(workQueue.lastIndex)
            println("Universe state count: ${universe.size} Remaining work queue: ${workQueue.size}")
            val pivots = findPivots(universe)
            println("Pivots state count: ${pivots.size}")

            // Find universe of terminal components reachable from pivot (and the component containing pivot)
            val forward = pivots.reachForward(universe)
            val currentComponent = pivots.reachBackward(forward)
            val reachableTerminalComponents = forward.subtract(currentComponent)

            // current component can be terminal for some subset of parameters
            val terminal = allColours(reachableTerminalComponents).not()

            if (terminal.isSat()) {
                onComponents(currentComponent.restrict(terminal))
            }

            if (reachableTerminalComponents.size > 0) {
                workQueue.add(reachableTerminalComponents)
            }

            // Find universe of terminal components not reachable from pivot
            val basinOfReachableComponents = forward.reachBackward(universe)
            val unreachableComponents = universe.subtract(basinOfReachableComponents)
            if (unreachableComponents.size > 0) {
                workQueue.add(unreachableComponents)
            }
        }
    }

    /*private fun StateMap.printFull() {
        this.forEach { (s, p) ->
            solver.run {
                println("S: ${states.decode(s).toList()} has ${p.cardinality()}")
                p.print()
            }
        }
    }*/

    /*private fun StateMap.trim(): StateMap {
        if (!trimEnabled) return this
        val trimmed = DecreasingStateMap(this, solver)
        val update = BitSet(stateCount)
        for (s in 0 until stateCount) {
            val c = trimmed.getOrNull(s)
            if (c != null) {
                update.set(s)
            }
        }
        solver.run {
            while (!update.isEmpty) {
                var state = update.nextSetBit(0)
                while (state > -1) {
                    // compute trim colours
                    var hasPredecessor = solver.empty
                    for (d in 0 until dimensions) {
                        val predecessor = states.flipValue(state, d)
                        val predecessorParams = trimmed.get(predecessor)
                        if (predecessorParams.isNotEmpty()) {
                            val edgeParams = solver.transitionParams(predecessor, d)
                            hasPredecessor = hasPredecessor or (edgeParams and predecessorParams)
                        }
                    }
                    if (trimmed.intersect(state, hasPredecessor)) {
                        // State params decreased - this means some successors may be trimmed
                        for (d in 0 until dimensions) {
                            val successor = states.flipValue(state, d)
                            update.set(successor)
                        }
                    }
                    update.clear(state)
                    state = update.nextSetBit(state + 1)
                }
            }
        }
        println("Trimmed ${this.size} -> ${trimmed.size}")
        return trimmed.toStateMap()
    }*/

    private fun StateMap<P>.restrict(colours: P): StateMap<P> {
        val result = newMap()
        (0 until stateCount).toList()
                .mapNotNull { s -> getOrNull(s)?.let { s to it } }
                .mapParallel { (s, c) ->
                    s to solver.run { c and colours }
                }
                .forEach { (s, p) ->
                    result.union(s, p)
                }
        return result
    }

    private fun allColours(map: StateMap<P>): P = solver.run {
        val list = (0 until stateCount)
                .mapNotNull { map.getOrNull(it) }
        return if (list.isEmpty()) ff else {
            list.merge { a, b -> a or b }
        }
    }

    private fun findPivots(map: StateMap<P>): StateMap<P> = solver.run {
        val result = newMap()
        var toCover = allColours(map)
        var remaining = (0 until stateCount)
                .mapNotNull { s -> map.getOrNull(s)?.let { s to (it) } }
        while (toCover.isSat()) {
            // there must be a gain in the first element of remaining because we remove all empty elements
            val (s, gain) = remaining.first().let { (s, p) -> s to (p and toCover) }
            toCover = toCover and gain.not()
            result.union(s, gain)
            remaining = remaining.mapParallel { (s, p) ->
                (s to (p and toCover)).takeIf { it.second.isSat() }
            }.filterNotNull()
        }
        result
    }

}