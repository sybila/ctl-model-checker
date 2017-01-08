package com.github.sybila.checker.shared

import com.github.sybila.checker.*
import com.github.sybila.huctl.*
import java.io.Closeable
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class Checker(
        private val transitionSystem: TransitionSystem,
        parallelism: Int = Runtime.getRuntime().availableProcessors()
) : Closeable {

    init {
        if (parallelism < 1) throw IllegalStateException("Can execute on $parallelism threads")
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
                            is Formula.Atom.False -> lazy { EmptyStateMap }
                            is Formula.Atom.Reference -> lazy { SingletonStateMap(key.name.toInt(), TT) }
                            is Formula.Atom.True -> transitionSystem.lazy { FullStateMap(stateCount, TT) }
                            is Formula.Atom.Float -> transitionSystem.lazy { key.eval() }
                            is Formula.Atom.Transition -> transitionSystem.lazy { key.eval() }
                        }
                        is Formula.Not -> transitionSystem.mkNot(resolve(key.inner))
                        is Formula.Bool<*> -> when (key as Formula.Bool<*>) {
                            is Formula.Bool.And -> transitionSystem.mkAnd(resolve(key.left), resolve(key.right))
                            is Formula.Bool.Or -> transitionSystem.mkOr(resolve(key.left), resolve(key.right))
                            is Formula.Bool.Implies -> resolve(not(key.left) or key.right)
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
                                transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, null, resolve(key.inner))
                            } else {
                                transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, null, resolve(key.inner))
                            }
                            is Formula.Simple.WeakFuture -> if (key.quantifier.isExistential()) {
                                transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, true, null, resolve(key.inner))
                            } else {
                                transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, true, null, resolve(key.inner))
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
                            else -> throw IllegalStateException()
                        }
                        is Formula.Until -> if (key.quantifier.isExistential()) {
                            transitionSystem.mkExistsUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach))
                        } else {
                            transitionSystem.mkAllUntil(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach))
                        }
                        is Formula.FirstOrder<*> -> when (key as Formula.FirstOrder<*>) {
                            is Formula.FirstOrder.ForAll -> transitionSystem.mkForAll(
                                    (0 until transitionSystem.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList(),
                                    resolve(key.bound)
                            )
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
                        else -> error("not implemented")
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
            operator.value
        }
    }

    fun <T> TransitionSystem.lazy(initializer: TransitionSystem.() -> T): Lazy<T> = kotlin.lazy {
        this.run(initializer)
    }

    fun TransitionSystem.mkNot(inner: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val valid = inner.value
        val result = kotlin.arrayOfNulls<Params>(stateCount)

        val ack = ArrayList<Future<*>>()
        for (state in 0 until stateCount) {
            val v = valid[state]
            if (v == null) {
                result[state] = TT
            } else {
                ack.add(executor.submit {
                    val not = Not(v)
                    result[state] = not.isSat()
                })
            }
        }
        ack.forEach { it.get() }

        ArrayStateMap(result)
    }

    fun TransitionSystem.mkAnd(left: Lazy<StateMap>, right: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val l = left.value
        val r = right.value
        val result = kotlin.arrayOfNulls<Params>(stateCount)

        val ack = ArrayList<Future<*>>()
        for ((s, lp) in l.entries) {
            val rp = r[s]
            if (rp != null) {
                ack.add(executor.submit {
                    val and = And(listOf(lp, rp))
                    result[s] = and.isSat()
                })
            }
        }
        ack.forEach { it.get() }

        ArrayStateMap(result)
    }

    fun TransitionSystem.mkOr(left: Lazy<StateMap>, right: Lazy<StateMap>): Lazy<StateMap> = this.lazy {
        val l = left.value
        val r = right.value
        val result = kotlin.arrayOfNulls<Params>(stateCount)

        for ((s, lp) in l.entries) {
            val rp = r[s]
            if (rp != null) {
                result[s] = Or(listOf(lp, rp))
            } else {
                result[s] = lp
            }
        }
        for ((s, rp) in r.entries) {
            val current = result[s]
            if (current == null) {
                result[s] = rp
            }
        }

        ArrayStateMap(result)
    }

    fun TransitionSystem.mkExistsUntil(
            timeFlow: Boolean, direction: DirectionFormula, weak: Boolean,
            pathOp: Lazy<StateMap>?, reach: Lazy<StateMap>
    ): Lazy<StateMap> = this.lazy {

        val path = pathOp?.value

        val result = kotlin.arrayOfNulls<Params>(stateCount)

        var recompute: List<Int>

        if (!weak) {
            recompute = reach.value.states.toList()
            for ((state, value) in reach.value.entries) {
                result[state] = value
            }
        } else {
            val compute = ArrayList<Int>()
            val r = reach.value
            (0 until stateCount).forEach { state ->
                val existsWrongDirection = state.successors(timeFlow).asSequence().fold(FF as Params) { a, t ->
                    if (!direction.eval(t.direction)) a or t.bound else a
                }
                val value = (r[state] ?: FF) or existsWrongDirection
                value.isSat()?.apply {
                    result[state] = value
                    compute.add(state)
                }
            }
            recompute = compute
        }

        do {
            //Map! - only read from results
            val ack: List<Pair<Int, Params>> = recompute
            .map { state ->
                executor.submit(Callable {
                    val value = result[state]!!
                    state.predecessors(timeFlow).asSequence().map { t ->
                        if (direction.eval(t.direction)) {
                            (value and t.bound).isSat()?.run { t.target to this }
                        } else null
                    }.filterNotNull().filter { (path == null || it.first in path) }.toList()
                })
            }.flatMap { it.get() }

            //Reduce! - only write to results once per state
            val m = ack.groupBy({ it.first }, { it.second })
            recompute = m
                .map { executor.submit(Callable {
                    val (state, new) = it
                    val disjunction = if (new.size == 1) new.first() else Or(new)
                    val withPath = if (path != null) disjunction and path[state]!! else disjunction
                    val current = result[state]
                    if (current == null) {
                        result[state] = withPath
                        state
                    } else {
                        val union = current.extendWith(withPath)
                        if (union != null) {
                            result[state] = union
                            state
                        } else {
                            null
                        }
                    }
                }) }.map { it.get() }.filterNotNull()
        } while (recompute.isNotEmpty())

        result.asStateMap()
    }

    fun TransitionSystem.mkAllUntil(
            timeFlow: Boolean, direction: DirectionFormula, weak: Boolean,
            pathOp: Lazy<StateMap>?, reach: Lazy<StateMap>
    ): Lazy<StateMap> = this.lazy {

        val path = pathOp?.value

        val result = kotlin.arrayOfNulls<Params>(stateCount)

        var candidates: Set<Int>

        if (!weak) {
            for ((state, value) in reach.value.entries) {
                result[state] = value
            }

            candidates = reach.value.states.asSequence()
                    .flatMap { it.predecessors(timeFlow).asSequence().map { it.target } }
                    .filter { path == null || it in path }
                    .toSet()
        } else {
            val r = reach.value
            val compute = ArrayList<Int>()
            (0 until stateCount).forEach { state ->
                val existsValidDirection = state.successors(timeFlow).asSequence().fold(FF as Params) { a, t ->
                    if (direction.eval(t.direction)) a or t.bound else a
                }
                val value = (r[state] ?: FF) or existsValidDirection.not()  //proposition or deadlock
                value.isSat()?.let { value ->
                    result[state] = value
                    compute.add(state)
                }
            }
            candidates = compute.asSequence()
                    .flatMap { it.predecessors(timeFlow).asSequence().map { it.target } }
                    .filter { path == null || it in path }
                    .toSet()
        }

        do {
            //Map! - only read from results
            val ack: List<Pair<Int, Params>> = candidates
                    .map { state ->
                        executor.submit(Callable {
                            val witnessList = state.successors(timeFlow).asSequence()
                                    .map {
                                        if (!direction.eval(it.direction)) it.bound.not()
                                        else (result[it.target] ?: FF) or it.bound.not()
                                    }.toList()
                            val witness = if (path == null) And(witnessList) else And(witnessList + path[state]!!)
                            witness.isSat()?.let { state to it }
                        })
                    }.map { it.get() }.filterNotNull()

            //Reduce! - only write to results once per state
            candidates = ack.map { executor.submit(Callable {
                val (state, new) = it
                val current = result[state]
                val changed = if (current == null) {
                    result[state] = new
                    true
                } else {
                    val union = current.extendWith(new)
                    if (union != null) {
                        result[state] = union
                        true
                    } else false
                }
                if (!changed) listOf() else {
                    state.predecessors(timeFlow).asSequence().map { it.target }
                            .filter { path == null || it in path }
                            .toList()
                }
            }) }.flatMap { it.get() }.toSet()

        } while (candidates.isNotEmpty())

        result.asStateMap()
    }

    fun TransitionSystem.mkExistsNext(
            timeFlow: Boolean, direction: DirectionFormula, inner: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {

        val reach = inner.value

        //Map! - only read from results
        val ack: List<Pair<Int, Params>> = reach.entries
                .map { entry ->
                    executor.submit(Callable {
                        val (state, value) = entry
                        state.predecessors(timeFlow).asSequence().map { t ->
                            if (!direction.eval(t.direction)) null
                            else (value and t.bound).isSat()?.run { t.target to this }
                        }.filterNotNull().toList()
                    })
                }.toList().flatMap { it.get() }

        //Reduce
        ack.groupBy({ it.first }, { it.second }).mapValues {
            if(it.value.size == 1) it.value.first() else Or(it.value)
        }.asStateMap()
    }

    fun TransitionSystem.mkAllNext(
            timeFlow: Boolean, direction: DirectionFormula, inner: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {

        val reach = inner.value

        val candidates: Set<Int> = reach.states.asSequence()
                .flatMap { it.predecessors(timeFlow).asSequence().map { it.target } }
                .toSet()

        candidates.map { state ->
            executor.submit(Callable {
                val witnessList = state.successors(timeFlow).asSequence()
                        .map {
                            if (!direction.eval(it.direction)) it.bound.not()
                            else (reach[it.target] ?: FF) or it.bound.not()
                        }.toList()
                val witness = And(witnessList)
                witness.isSat()?.let { state to it }
            })
        }.map { it.get() }.filterNotNull().toMap().asStateMap()
    }

    fun TransitionSystem.mkForAll(
            inner: MutableList<Lazy<StateMap>?>, bound: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        //forall x in B: A <=> forall x: ((at x: B) => A) <=> forall x: (!(at x: B) || A)

        val b = bound.value

        val result = Array<Params?>(stateCount) { TT }
        for (state in 0 until stateCount) {
            val stateBound = b[state]
            if (stateBound != null) {
                val i = inner[state]!!.value
                for (j in 0 until stateCount) {
                    val k = i[j]
                    val current = result[j]
                    if (current != null) {
                        if (k != null) {
                            result[j] = (current and (k or stateBound.not())).isSat()
                        } else {
                            result[j] = (current and stateBound.not()).isSat()
                        }
                    }
                }
            }
            inner[state] = null //GC!
        }

        result.asStateMap()
    }

    fun TransitionSystem.mkExists(
            inner: MutableList<Lazy<StateMap>?>, bound: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {

        //exists x in B: A <=> exists x: ((at x: B) && A)

        val b = bound.value

        val result = arrayOfNulls<Params>(stateCount)
        for (state in 0 until stateCount) {
            b[state]?.let { stateBound ->
                val i = inner[state]!!.value
                i.entries.forEach {
                    result[it.first] = (result[it.first] ?: FF) or (it.second and stateBound)
                }
            }
            inner[state] = null
        }

        result.indices.forEach {
            result[it] = result[it]?.isSat()
        }

        result.asStateMap()

    }

    fun TransitionSystem.mkAt(
            state: Int, inner: Lazy<StateMap>
    ) : Lazy<StateMap> = this.lazy {
        val value = inner.value[state]
        value?.asStateMap(stateCount) ?: emptyStateMap()
    }

    fun TransitionSystem.mkBind(
            inner: MutableList<Lazy<StateMap>?>
    ) : Lazy<StateMap> = this.lazy {
        val result = kotlin.arrayOfNulls<Params>(stateCount)
        for (state in 0 until stateCount) {
            result[state] = inner[state]!!.value[state]
            inner[state] = null
        }
        result.asStateMap()
    }

}