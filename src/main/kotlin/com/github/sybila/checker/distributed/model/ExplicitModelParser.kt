package com.github.sybila.checker.distributed.model

import com.github.sybila.checker.distributed.Checker
import com.github.sybila.checker.distributed.Solver
import com.github.sybila.checker.distributed.StateMap
import com.github.sybila.checker.Transition
import com.github.sybila.checker.antlr.ModelBaseListener
import com.github.sybila.checker.antlr.ModelLexer
import com.github.sybila.checker.antlr.ModelParser
import com.github.sybila.checker.distributed.channel.connectWithSharedMemory
import com.github.sybila.checker.distributed.solver.IntSetSolver
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.HUCTLParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

fun String.asExperiment(): () -> Unit {
    val parser = ModelParser(CommonTokenStream(ModelLexer(ANTLRInputStream(this))))
    val experiment = ModelContext()
    ParseTreeWalker().walk(experiment, parser.root())
    return {
        val paramsMapping = if (experiment.params.isEmpty()) listOf(1) else experiment.params.toList()
        fun String.mapParam(): Int = paramsMapping.indexOf(this).apply {
            if (this == -1) throw IllegalStateException("${this@mapParam} is not a parameter name")
        }

        val stateMapping = experiment.states.flatMap { it }.sorted()
        fun String.mapState(): Int = stateMapping.indexOf(this).apply {
            if (this == -1) throw IllegalStateException("${this@mapState} is not a state name")
        }

        fun Set<String>.readColors(solver: Solver<Set<Int>>): Set<Int> = if (this.isEmpty()) solver.tt else {
            this.map(String::mapParam).toSet()
        }

        val fullParams = (0..Math.max(0, paramsMapping.size - 1)).toSet()

        val globalSolver = IntSetSolver(fullParams)

        val partitioning = experiment.states.map { it.map(String::mapState).toSet() }

        val partitions = (0 until partitioning.size).map {
            val partition = partitioning[it]
            val solver = IntSetSolver(fullParams)

            val transitionFunction: Map<Int, List<Transition<Set<Int>>>>
                    = experiment.edges.groupBy { it.from.mapState() }
                    .mapValues {
                        it.value.map {
                            val (from, to, dir, bound) = it
                            Transition(it.to.mapState(), dir, bound.readColors(solver))
                        }
                    }

            val atom: Map<Formula.Atom, StateMap<Set<Int>>> = experiment.atom.map {
                val (atom, map) = it
                solver.run {
                    atom to map.mapKeys { it.key.mapState() }.mapValues {
                        it.value.readColors(solver)
                    }.filterKeys { it in partition }.asStateMap()
                }
            }.toMap()

            ExplicitPartition(
                    partitionId = it,
                    partitionCount = partitioning.size,
                    stateCount = stateMapping.size,
                    states = partitioning,
                    successorMap = transitionFunction,
                    validity = atom,
                    solver = solver
            )
        }

        Checker(partitions.connectWithSharedMemory()).use { checker ->

            experiment.verify.forEach {
                val result = checker.verify(it)
                println("$it -> ${result.zip(partitions).map { it.second.run { it.first.prettyPrint() } }}")
            }

            experiment.assert.forEach {
                println("Check assert ${it.first}")
                val (formula, input) = it

                globalSolver.run {
                    val expected: StateMap<Set<Int>> = input.mapKeys {
                        it.key.mapState()
                    }.mapValues {
                        it.value.readColors(globalSolver)
                    }.asStateMap()

                    val result = checker.verify(formula)

                    if (!(expected.deepEquals(partitions.zip(result)))) {
                        //Print with original names
                        throw IllegalStateException("$formula error: expected $input, but got ${result.map { map ->
                            map.entries().asSequence().map {
                                val (state, params) = it
                                stateMapping[state] to params.map { paramsMapping[it] }
                            }.filter { it.second.isNotEmpty() }.toList()
                        }}")
                    }
                }

            }

        }

    }
}

private data class Edge(
        val from: String,
        val to: String,
        val direction: DirectionFormula.Atom,
        val bound: Set<String>
)

private class ModelContext : ModelBaseListener() {

    private val parser = HUCTLParser()

    internal val params: MutableSet<String> = HashSet()
    internal val states: MutableList<MutableSet<String>> = ArrayList()
    internal val edges: MutableList<Edge> = ArrayList()
    internal val verify: MutableList<Formula> = ArrayList()
    internal val assert: MutableList<Pair<Formula, Map<String, Set<String>>>> = ArrayList()
    internal val atom: MutableList<Pair<Formula.Atom, Map<String, Set<String>>>> = ArrayList()

    override fun exitParams(ctx: ModelParser.ParamsContext) {
        params.addAll(ctx.param().map { it.text })
    }

    override fun exitStates(ctx: ModelParser.StatesContext) {
        val partition = ctx.NUM()?.text?.toInt() ?: 0
        while (states.size <= partition) states.add(HashSet())
        states[partition].addAll(ctx.state().map { it.text })
    }

    override fun exitEdges(ctx: ModelParser.EdgesContext) {
        edges.addAll(ctx.edge().map {
            Edge(
                    from = it.state(0).text,
                    to = it.state(1).text,
                    direction = parser.dirAtom(it.STRING()?.readString() ?: "True"),
                    bound = it.param().map { it.text }.toSet()
            )
        })
    }

    override fun exitVerify(ctx: ModelParser.VerifyContext) {
        verify.add(parser.formula(ctx.STRING().readString()))
    }

    override fun exitAssert(ctx: ModelParser.AssertContext) {
        assert.add(parser.formula(ctx.STRING().readString()) to ctx.stateParams().map {
            it.state().text to it.param().map { it.text }.toSet()
        }.toMap())
    }

    override fun exitAtom(ctx: ModelParser.AtomContext) {
        atom.add(parser.atom(ctx.STRING().readString()) to ctx.stateParams().map {
            it.state().text to it.param().map { it.text }.toSet()
        }.toMap())
    }

    override fun visitErrorNode(node: ErrorNode) {
        throw IllegalStateException("Syntax error at '${node.text}' in ${node.symbol.line}")
    }

    private fun TerminalNode.readString() = this.text.filter { it != '"' }

}