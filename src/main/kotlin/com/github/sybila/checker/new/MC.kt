package com.github.sybila.checker.new

import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.PathQuantifier
import com.github.sybila.huctl.True
import com.github.sybila.huctl.not
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class Checker<Colors>(
        private val comm: Comm<Colors>,
        private val setup: List<Pair<Fragment<Colors>, Solver<Colors>>>
) {

    constructor(fragment: Fragment<Colors>, solver: Solver<Colors>) : this(NoComm<Colors>(), listOf(fragment to solver))

    init {
        if (comm.size != setup.size) throw IllegalArgumentException("Cannot use comm(${comm.size}) with setup(${setup.size})")
    }

    private val executor = Executors.newFixedThreadPool(setup.size)

    fun verify(formula: Formula): List<StateMap<Colors>> {
        synchronized(this) {    //only one verification allowed at a time
            return setup.map {
                Worker(it.first, it.second)
            }.map {
                executor.submit(Callable<StateMap<Colors>> {
                    it.verify(formula, mapOf())
                })
            }.map { it.get() }
        }
    }

    fun shutdown() {
        executor.shutdown()
    }

    inner class Worker(
            val fragment: Fragment<Colors>,
            val solver: Solver<Colors>
    ) : Fragment<Colors> by fragment, Solver<Colors> by solver {

        fun verify(formula: Formula, assignment: Map<String, Int>): StateMap<Colors> {
            println("Verify: $formula at $assignment")
            return when (formula) {
                //Propositions
                is Formula.Atom.Reference -> {
                    val state = assignment[formula.name] ?: throw IllegalStateException("Unknown reference ${formula.name}")
                    (state to tt).asStateMap(ff)
                }
                is Formula.Atom -> eval(formula)

                //First order
                is Formula.FirstOrder.ForAll -> {
                    val result = HashMap<Int, Colors>()
                    val assignmentCopy = HashMap(assignment)
                    for (s in eval(True)) result[s] = tt
                    for (state in verify(formula.bound, assignment)) {
                        assignmentCopy[formula.name] = state
                        val inner = verify(formula.target, assignmentCopy)
                        result.keys.forEach { result[it] = result[it]!! and inner[it] }
                    }
                    result.asStateMap(ff)
                }
                is Formula.FirstOrder.Exists -> {
                    val result = HashMap<Int, Colors>()
                    val assignmentCopy = HashMap(assignment)
                    for (state in verify(formula.bound, assignment)) {
                        assignmentCopy[formula.name] = state
                        val inner = verify(formula.target, assignmentCopy)
                        inner.forEach { result[it] = (result[it] ?: ff) or inner[it] }
                    }
                    result.asStateMap(ff)
                }

                //Hybrid
                is Formula.Hybrid.Bind -> {
                    val assignmentCopy = HashMap(assignment)
                    eval(True).asSequence().map {
                        assignmentCopy[formula.name] = it
                        val r = verify(formula.target, assignmentCopy)
                        it to r[it]
                    }.toMap().asStateMap(ff)
                }
                is Formula.Hybrid.At -> {
                    val stateCount = eval(True).count()
                    val state = assignment[formula.name] ?: throw IllegalStateException("Unbound name ${formula.name}")
                    val inner = verify(formula.target, assignment)
                    inner[state].asConstantStateMap(0 until stateCount)
                }

                //Boolean logic
                is Formula.Not -> {
                    val inner = verify(formula.inner, assignment)
                    val all = eval(True)
                    println("Not!: $all")
                    all.asSequence()
                            .map {
                                println("Check $it")
                                it to if (it in inner) { all[it] and inner[it].not() } else all[it]
                            }
                            .filter { it.second.isNotEmpty() }
                            .toMap()
                            .asStateMap(ff)
                }
                is Formula.Bool<*> -> {
                    val left = verify(formula.left, assignment)
                    val right = verify(formula.right, assignment)
                    @Suppress("USELESS_CAST")   //not so useless after all...
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

                //Temporal stuff
                is Formula.Simple<*> -> {
                    val timeFlow = formula.quantifier == PathQuantifier.A || formula.quantifier == PathQuantifier.E
                    val existsPath = formula.quantifier == PathQuantifier.E || formula.quantifier == PathQuantifier.pE
                    val inner = verify(formula.inner, assignment)
                    @Suppress("USELESS_CAST")   //not so useless after all...
                    when (formula as Formula.Simple<*>) {
                        is Formula.Simple.Next -> {
                            if (existsPath) { //EX
                                EX(timeFlow, formula.direction, inner, comm, solver, fragment)
                            } else {    //AX
                                AX(timeFlow, formula.direction, inner, comm, solver, fragment)
                            }.computeFixPoint().asStateMap()
                        }
                        is Formula.Simple.Future -> {
                            if (existsPath) {
                                EF(timeFlow, formula.direction, inner, comm, solver, fragment)
                            } else {
                                AF(timeFlow, formula.direction, inner, comm, solver, fragment)
                            }.computeFixPoint().asStateMap()
                        }
                        is Formula.Simple.Globally -> {
                            if (existsPath) {
                                EG(timeFlow, formula.direction, inner, eval(True), comm, solver, fragment)
                            } else {
                                AG(timeFlow, formula.direction, inner, eval(True), comm, solver, fragment)
                            }.computeFixPoint().asStateMap()
                        }
                        //Weak stuff is translated
                        is Formula.Simple.WeakNext -> {
                            //EwX = !AX!
                            //AwX = !EX!
                            verify(not(Formula.Simple.Next(
                                    formula.quantifier.invertCardinality(),
                                    not(formula.inner),
                                    formula.direction
                            )), assignment)
                        }
                        is Formula.Simple.WeakFuture -> {
                            //EwF = !AG!
                            //AwF = !EG!
                            verify(not(Formula.Simple.Globally(
                                    formula.quantifier.invertCardinality(),
                                    not(formula.inner),
                                    formula.direction
                            )), assignment)
                        }
                        else -> throw IllegalStateException("Unsupported formula $formula")
                    }
                }
                is Formula.Until -> {
                    val timeFlow = formula.quantifier == PathQuantifier.A || formula.quantifier == PathQuantifier.E
                    val existsPath = formula.quantifier == PathQuantifier.E || formula.quantifier == PathQuantifier.pE
                    val reach = verify(formula.reach, assignment)
                    val path = verify(formula.path, assignment)
                    if (existsPath) {
                        EU(timeFlow, formula.direction, path, reach, comm, solver, fragment)
                    } else {
                        AU(timeFlow, formula.direction, path, reach, comm, solver, fragment)
                    }.computeFixPoint().asStateMap()
                }
                else -> throw IllegalStateException("Unsupported formula $formula")
            }.apply { println(this.toString()) }
        }

    }

}