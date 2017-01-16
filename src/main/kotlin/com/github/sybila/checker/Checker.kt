package com.github.sybila.checker

import com.github.daemontus.map
import com.github.daemontus.unwrapOr
import com.github.sybila.checker.map.asStateMap
import com.github.sybila.checker.map.emptyStateMap
import com.github.sybila.huctl.*
import java.io.Closeable
import java.io.PrintStream
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

class Checker(
        private val transitionSystem: TransitionSystem,
        private val parallelism: Int = Runtime.getRuntime().availableProcessors(),
        private val partition: Int.() -> Int = { this % parallelism },
        private val progress: PrintStream? = null
) : Closeable {

    init {
        if (parallelism < 1) throw IllegalStateException("Can't execute on $parallelism threads")
    }

    private val executor = Executors.newFixedThreadPool(parallelism)

    override fun close(): Unit { executor.shutdown() }

    fun verify(formula: Formula): StateMap = verify(mapOf("f" to formula))["f"]!!

    fun verify(formulas: Map<String, Formula>): Map<String, StateMap> {
        val dependencyTree: Map<String, Pair<Formula, Lazy<StateMap>>> = HashMap<Formula, Lazy<StateMap>>().let { tree ->

            fun resolve(key: Formula): Lazy<StateMap> {
                //TODO: caching is still suboptimal, because the references are not canonical, so
                //exists s: forall x: x && s and exists s: forall z: z && s will create two separate trees
                return tree.computeIfAbsent(key) {
                    @Suppress("USELESS_CAST")
                    when (key) {
                        is Formula.Atom -> when (key) {
                            is Formula.Atom.False -> lazy { emptyStateMap() }
                            is Formula.Atom.Reference -> lazy { key.name.toInt().asStateMap(TT) }
                            is Formula.Atom.True -> transitionSystem.lazy { TT.asStateMap(stateCount) }
                            is Formula.Atom.Float -> transitionSystem.lazy { key.eval() }
                            is Formula.Atom.Transition -> transitionSystem.lazy { key.eval() }
                        }
                        is Formula.Not -> transitionSystem.mkNot(resolve(key.inner))
                        is Formula.Bool<*> -> when (key as Formula.Bool<*>) {
                            is Formula.Bool.And -> transitionSystem.mkAnd(resolve(key.left), resolve(key.right))
                            is Formula.Bool.Or -> transitionSystem.mkOr(resolve(key.left), resolve(key.right))
                            is Formula.Bool.Implies -> resolve(com.github.sybila.huctl.not(key.left) or key.right)
                            is Formula.Bool.Equals -> resolve((key.left and key.right) or (not(key.left) and not(key.right)))
                        }
                        is Formula.Simple<*> -> when (key as Formula.Simple<*>) {
                            is Formula.Simple.Next -> if(key.quantifier.isExistential()) {
                                transitionSystem.mkExistsNext(key.quantifier.isNormalTimeFlow(), key.direction, resolve(key.inner))
                            } else {
                                transitionSystem.mkAllNext(key.quantifier.isNormalTimeFlow(), key.direction, resolve(key.inner))
                            }
                            //Until operators are not slower, because the path null check is fast and predictable
                            is Formula.Simple.Future -> if (key.quantifier.isExistential()) {
                                transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(True), resolve(key.inner))
                            } else {
                                transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(True), resolve(key.inner))
                            }
                            is Formula.Simple.WeakFuture -> if (key.quantifier.isExistential()) {
                                transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, true, resolve(True), resolve(key.inner))
                            } else {
                                transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, true, resolve(True), resolve(key.inner))
                            }
                            //EwX = !AX!
                            //AwX = !EX!
                            is Formula.Simple.WeakNext -> resolve(not(Formula.Simple.Next(
                                    key.quantifier.invertCardinality(),
                                    com.github.sybila.huctl.not(key.inner), key.direction
                            )))
                            //EG = !AF! / !AwF!
                            //AG = !EG! / !EwF!
                            is Formula.Simple.Globally -> if (key.direction == DirectionFormula.Atom.True) {
                                resolve(not(Formula.Simple.Future(
                                        key.quantifier.invertCardinality(),
                                        com.github.sybila.huctl.not(key.inner), key.direction
                                )))
                            } else {
                                resolve(not(Formula.Simple.WeakFuture(
                                        key.quantifier.invertCardinality(),
                                        com.github.sybila.huctl.not(key.inner), key.direction
                                )))
                            }
                        }
                        is Formula.Until -> if (key.quantifier.isExistential()) {
                            transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach))
                        } else {
                            transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach))
                        }
                        is Formula.FirstOrder<*> -> when (key as Formula.FirstOrder<*>) {
                            //forall x in B: A <=> !exists x in B: !A
                            is Formula.FirstOrder.ForAll -> resolve(not(Formula.FirstOrder.Exists(key.name, key.bound, not(key.target))))
                            is Formula.FirstOrder.Exists -> transitionSystem.mkExists(
                                    (0 until transitionSystem.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList(),
                                    resolve(key.bound)
                            )
                        }
                        is Formula.Hybrid<*> -> when (key as Formula.Hybrid<*>) {
                            is Formula.Hybrid.Bind -> transitionSystem.mkBind(
                                    (0 until transitionSystem.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList()
                            )
                            is Formula.Hybrid.At -> transitionSystem.mkAt(key.name.toInt(), resolve(key.target))
                        }
                    }
                }
            }

            formulas.mapValues {
                val (name, formula) = it
                formula to resolve(formula)
            }
        }

        return dependencyTree.mapValues {
            val (formula, operator) = it.value
            progress?.println("Start formula: $formula")
            operator.value
        }
    }

    private fun Int.partition(): Int = partition.invoke(this)

    //create a lazy value that is initialized with given transition system
    fun <T> TransitionSystem.lazy(initializer: TransitionSystem.() -> T): Lazy<T> = kotlin.lazy {
        this.run(initializer)
    }

    fun TransitionSystem.mkNot(innerOp: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val inner = innerOp.value
        progress?.println("Start operator: Not")
        val result = Array<Params?>(stateCount) { TT }

        inner.entries.map {
            val (state, value) = it
            executor.submit {
                result[state] = value.not()?.isSat()
            }
        }.forEach { it.get() }

        result.asStateMap()
    }

    fun TransitionSystem.mkAnd(left: Lazy<StateMap>, right: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val rightMap = right.value
        val leftMap = left.value
        progress?.println("Start operator: And")
        val result = arrayOfNulls<Params>(stateCount)

        val ack = ArrayList<Future<*>>()
        for ((state, leftValue) in leftMap.entries) {
            rightMap[state]?.let { rightValue ->
                ack.add(executor.submit {
                    result[state] = (leftValue and rightValue)?.isSat()
                })
            }
        }
        ack.forEach { it.get() }

        result.asStateMap()
    }

    fun TransitionSystem.mkOr(left: Lazy<StateMap>, right: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val l = left.value
        val r = right.value
        progress?.println("Start operator: Or")
        Array(stateCount) { l[it] or r[it] }.asStateMap()
    }

    fun TransitionSystem.mkExistsUntil(
            timeFlow: Boolean, direction: DirectionFormula, weak: Boolean,
            pathOp: Lazy<StateMap>, reachOp: Lazy<StateMap>
    ): Lazy<StateMap> = this.lazy {

        val path = pathOp.value
        val reach = reachOp.value

        progress?.println("Start operator: EU")

        val result = Array(stateCount) { reach[it] }

        val queues = Array<ConcurrentLinkedQueue<Pair<Params, Int>>>(parallelism) {
            ConcurrentLinkedQueue()
        }
        val counter = AtomicLong(0)

        val barrier = CyclicBarrier(parallelism)
        val map = HashMap<Int, MutableList<Int>>()
        //ensure each worker has at least an empty list
        reach.states.groupByTo(map, { it % parallelism })
        (0 until parallelism).forEach { map.computeIfAbsent(it) { ArrayList() } }
        map.map {
            val (workerId, initial) = it
            Thread {

                val localQueue = ArrayDeque<Pair<Params, Int>>()
                val queue = queues[workerId]

                fun Int.push() {
                    val current = result[this]
                    this.predecessors(timeFlow).map { t ->
                        t.assuming { direction.eval(t.direction) }
                            ?.let { sequenceOf(
                                    path[it.target], result[it.target].not(), current, t.bound
                            ).asConjunction()?.isSat() }?.to(t.target)
                    }.filterNotNull().forEach {
                        counter.incrementAndGet()
                        val partition = it.second.partition()
                        if (partition == workerId) {
                            localQueue.add(it)
                        } else {
                            queues[it.second.partition()].add(it)
                        }
                    }
                }

                if (!weak) {
                    initial.forEach(Int::push)
                } else {
                    (0 until stateCount).forEach { state ->
                        val witness = state.successors(timeFlow)
                                .map { t -> t.bound.assuming { direction.eval(t.direction) }?.not() }   //invalid direction or
                                .plus(reach[state])                                                     //reachable witness
                                .asDisjunction()?.isSat()
                        result[state] = witness
                        if (witness != null) state.push()
                    }
                }
                barrier.await()

                do {
                    //move data from global to local queue
                    var item: Pair<Params, Int>? = queue.poll()
                    while (item != null) {
                        localQueue.add(item)
                        item = queue.poll()
                    }

                    //process one item from local queue
                    var compute = localQueue.poll()
                    while (compute != null) {
                        val (new, state) = compute
                        result[state].extendWith(new).map {
                            result[state] = it
                            state.push()
                        }
                        counter.decrementAndGet()
                        compute = localQueue.poll()
                    }
                } while (counter.get() > 0)

            }.apply { this.start() }
        }.toList().map(Thread::join)

        result.asStateMap()
    }

    fun TransitionSystem.mkAllUntil(
            timeFlow: Boolean, direction: DirectionFormula, weak: Boolean,
            pathOp: Lazy<StateMap>, reachOp: Lazy<StateMap>
    ): Lazy<StateMap> = this.lazy {

        val path = pathOp.value
        val reach = reachOp.value

        progress?.println("Start operator: AU")

        val result = arrayOfNulls<Params>(stateCount)

        val candidates = HashSet<Int>()

        if (!weak) {
            reach.entries.flatMap {
                result[it.first] = it.second
                it.first.predecessors(timeFlow).map { it.target }
            }.filterTo(candidates) { it in path }
        } else {
            (0 until stateCount).asSequence().map { state ->
                //proposition or !(OR s) <=> proposition or (AND !s)
                val deadlock = state.successors(timeFlow)
                        .map { t -> t.bound.assuming { direction.eval(t.direction) }?.not() }
                        .asConjunction()
                (reach[state] or deadlock)?.isSat()?.let {
                    result[state] = it
                    state
                }
            }.filterNotNull().flatMap {
                it.predecessors(timeFlow).map { it.target }
            }.filterTo(candidates) { it in path }
        }

        do {
            //Map! - only read from results
            val map: List<Pair<Params, Int>> = candidates
                    .map { state ->
                        executor.submit(Callable {
                            val witness = state.successors(timeFlow).map {
                                val (target, dir, bound) = it
                                result[target]?.assuming { direction.eval(dir) } or bound.not()
                            }.plus(path[state]).asConjunction()
                            witness?.isSat()?.to(state)
                        })
                    }.map { it.get() }.filterNotNull()

            //Reduce! - only write to results once per state
            candidates.clear()
            map.map { executor.submit(Callable {
                val (witness, state) = it
                result[state].extendWith(witness).map {
                    result[state] = it
                    state.predecessors(timeFlow).map { it.target }.filter { it in path }.toList()
                }
            }) }.map { it.get().unwrapOr(null) }.filterNotNull().flatMapTo(candidates) { it }

        } while (candidates.isNotEmpty())

        result.asStateMap()
    }

    fun TransitionSystem.mkExistsNext(
            timeFlow: Boolean, direction: DirectionFormula, innerOp: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        val inner = innerOp.value
        progress?.println("Start operator: EX")

        //Map! - only read from results
        val map: Sequence<Pair<Params, Int>> = inner.entries
                .map { entry ->
                    executor.submit(Callable {
                        val (state, value) = entry
                        state.predecessors(timeFlow).map { transition ->
                            transition
                                    .assuming { direction.eval(it.direction) }
                                    ?.let { (value and it.bound)?.isSat() }
                                    ?.to(transition.target)
                        }.filterNotNull().toList()
                    })
                }.toList().map { it.get() }.asSequence().flatMap { it.asSequence() }

        //Reduce
        map.groupBy({ it.second }, { it.first }).mapValues {
            it.value.asDisjunction()!!
        }.asStateMap()
    }

    fun TransitionSystem.mkAllNext(
            timeFlow: Boolean, direction: DirectionFormula, innerOp: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        val reach = innerOp.value
        progress?.println("Start operator: AX")

        val candidates: Set<Int> = reach.states
                .flatMap { it.predecessors(timeFlow).map { it.target } }
                .toSet()

        candidates.map { state ->
            executor.submit(Callable {
                val witness = state.successors(timeFlow)
                        .map { transition ->
                            val (target, dir, bound) = transition
                            dir     .assuming { direction.eval(dir) }
                                    ?.let { reach[target] or bound.not() }
                            ?: bound.not()
                        }.asConjunction()
                witness?.isSat()?.let { state to it }
            })
        }.map { it.get() }.filterNotNull().toMap().asStateMap()
    }

    fun TransitionSystem.mkExists(
            inner: MutableList<Lazy<StateMap>?>, bound: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        progress?.println("Start operator: Exists")

        //exists x in B: A <=> exists x: ((at x: B) && A)

        /*
            for all <=> exists correctness
            forall x in B: A <=>
            forall x: ((at x: B) => A) <=>
            !exists x: !((at x: B) => A) <=>
            !exists x: ((at x: B) && !A) <=>
            !exists x in B: !A
         */

        val boundMap = bound.value
        val result = arrayOfNulls<Params>(stateCount)

        for (state in 0 until stateCount) {
            boundMap[state]?.let { stateBound ->
                inner[state]?.byTheWay { inner[state] = null }?.value?.entries?.map {
                    executor.submit(Callable {
                        val (at, value) = it
                        result[at] = result[at] or (value and stateBound)?.isSat()
                    })
                }?.toList()?.forEach { it.get() }
            }
        }

        result.asStateMap()

    }

    fun TransitionSystem.mkAt(
            state: Int, inner: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        progress?.println("Start operator: At")
        inner.value[state]?.asStateMap(stateCount) ?: emptyStateMap()
    }

    fun TransitionSystem.mkBind(
            inner: MutableList<Lazy<StateMap>?>
    ) : Lazy<StateMap> = this.lazy {
        progress?.println("Start operator: Bind")
        Array(stateCount) {
            inner[it]?.byTheWay { inner[it] = null }?.value?.get(it)
        }.asStateMap()
    }

}