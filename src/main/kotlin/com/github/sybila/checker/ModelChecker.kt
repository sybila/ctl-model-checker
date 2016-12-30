package com.github.sybila.checker

import com.github.sybila.checker.channel.SingletonChannel
import com.github.sybila.checker.operator.*
import com.github.sybila.checker.partition.SingletonPartition
import com.github.sybila.huctl.*
import java.io.Closeable
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Read Me:
 *
 * State identifiers are integers and should be unique across the whole system.
 * (If you have more than 2^31 states, you are going to time-out anyway)
 * Reason: They are easy to serialize and pass around.
 *
 * Params don't have any direct operations, everything is implemented in the solver.
 * The checker/communicator ensures that:
 *   - only colors created by the solver are passed to it as parameters
 *   - only one thread can access a specific solver at a time
 *   - colors are transferred between solvers:
 *      - in a solver-agnostic format (ByteBuffer)
 *      - using clone operation which locks both outgoing and incoming solver
 */

interface Operator<out Params : Any> {

    fun compute(): StateMap<Params>

}

class SequentialChecker<out Params : Any>(model: Model<Params>) : Closeable {

    private val checker = Checker(model)

    fun verify(formula: Formula): StateMap<Params> = verify(mapOf("formula" to formula))["formula"]!!

    fun verify(formulas: Map<String, Formula>): Map<String, StateMap<Params>>
            = checker.verify(formulas).mapValues { it.value.first() }

    override fun close() {
        checker.close()
    }

}

class Checker<Params : Any>(
    private val config: List<Channel<Params>>
) : Closeable {

    constructor(model: Model<Params>) : this(listOf(SingletonChannel(SingletonPartition(model))))

    private val executor = Executors.newFixedThreadPool(config.size)

    fun verify(formula: Formula): List<StateMap<Params>> = verify(mapOf("formula" to formula))["formula"]!!

    fun verify(formulas: Map<String, Formula>): Map<String, List<StateMap<Params>>> {
        synchronized(this) {
            val result: List<Map<String, StateMap<Params>>> = config.map {
                Worker(it)
            }.map {
                executor.submit(Callable<Map<String, StateMap<Params>>> {
                    it.verify(formulas)
                })
            }.map {
                it.get()
            }

            return formulas.mapValues { name ->
                result.map { it[name.key]!! }
            }
        }
    }

    override fun close() {
        executor.shutdown()
    }

}

private class Worker<out Params : Any>(
        private val channel : Channel<Params>
) {

    fun verify(formulas: Map<String, Formula>): Map<String, StateMap<Params>> {
        val dependencyTree: Map<String, Pair<Formula, Operator<Params>>> = HashMap<Formula, Operator<Params>>().let { tree ->

            fun resolve(formula: Formula): Operator<Params> {
                val key = formula
                return tree.computeIfAbsent(key) {
                    @Suppress("USELESS_CAST", "RemoveExplicitTypeArguments")
                    when (key) {
                        is Formula.Atom -> when (key) {
                            is Formula.Atom.False -> FalseOperator(channel)
                            is Formula.Atom.True -> TrueOperator(channel)
                            is Formula.Atom.Reference -> ReferenceOperator(key.name.toInt(), channel)
                            is Formula.Atom.Float -> FloatOperator(key, channel)
                            is Formula.Atom.Transition -> TransitionOperator(key, channel)
                        }
                        is Formula.Not -> ComplementOperator(resolve(True), resolve(key.inner), channel)
                        is Formula.Bool<*> -> when (key as Formula.Bool<*>) {
                            is Formula.Bool.And -> AndOperator(resolve(key.left), resolve(key.right), channel)
                            is Formula.Bool.Or -> OrOperator(resolve(key.left), resolve(key.right), channel)
                            is Formula.Bool.Implies -> resolve(not(key.left) or key.right)
                            is Formula.Bool.Equals -> resolve((key.left and key.right) or (not(key.left) and not(key.right)))
                        }
                        is Formula.Simple<*> -> when (key as Formula.Simple<*>) {
                            is Formula.Simple.Next -> if (key.quantifier.isExistential()) {
                                ExistsNextOperator(key.quantifier.isNormalTimeFlow(), key.direction, resolve(key.inner), channel)
                            } else {
                                AllNextOperator(key.quantifier.isNormalTimeFlow(), key.direction, resolve(key.inner), channel)
                            }
                            //Until operators are not slower, because the path null check is fast and predictable
                            is Formula.Simple.Future -> if (key.quantifier.isExistential()) {
                                ExistsUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, false, null, resolve(key.inner), channel)
                            } else {
                                AllUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, false, null, resolve(key.inner), channel)
                            }
                            is Formula.Simple.WeakFuture -> if (key.quantifier.isExistential()) {
                                ExistsUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, true, null, resolve(key.inner), channel)
                            } else {
                                AllUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, true, null, resolve(key.inner), channel)
                            }
                            //EwX = !AX!
                            //AwX = !EX!
                            is Formula.Simple.WeakNext -> resolve(not(Formula.Simple.Next(
                                    key.quantifier.invertCardinality(),
                                    not(key.inner), key.direction
                            )))
                            //EG = !AF! / !AwF!
                            //AG = !EG! / !EwF!
                            is Formula.Simple.Globally -> if (key.direction == DirectionFormula.Atom.True) {
                                resolve(not(Formula.Simple.Future(
                                        key.quantifier.invertCardinality(),
                                        not(key.inner), key.direction
                                )))
                            } else {
                                resolve(not(Formula.Simple.WeakFuture(
                                        key.quantifier.invertCardinality(),
                                        not(key.inner), key.direction
                                )))
                            }
                            else -> throw IllegalStateException()
                        }
                        is Formula.Until -> if (key.quantifier.isExistential()) {
                            ExistsUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach), channel)
                        } else {
                            AllUntilOperator(key.quantifier.isNormalTimeFlow(), key.direction, false, resolve(key.path), resolve(key.reach), channel)
                        }
                        is Formula.FirstOrder<*> -> when (key as Formula.FirstOrder<*>) {
                            //TODO: This is a bug apparently - Params needs to be specified because of list nullity
                            is Formula.FirstOrder.ForAll -> ForAllOperator<Params>(resolve(True),
                                    (0 until channel.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList(),
                                    resolve(key.bound), channel
                            )
                            is Formula.FirstOrder.Exists -> ExistsOperator<Params>(
                                    (0 until channel.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList(),
                                    resolve(key.bound), channel
                            )
                        }
                        is Formula.Hybrid<*> -> when (key as Formula.Hybrid<*>) {
                            is Formula.Hybrid.Bind -> BindOperator<Params>(
                                    (0 until channel.stateCount).map { resolve(key.target.bindReference(key.name, it)) }.toMutableList(),
                                    channel
                            )
                            is Formula.Hybrid.At -> AtOperator(key.name.toInt(), resolve(key.target), channel)
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
            operator.compute()
        }
    }
}