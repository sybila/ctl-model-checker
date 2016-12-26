package com.github.sybila.checker.model

import com.github.sybila.checker.antlr.ModelBaseListener
import com.github.sybila.checker.antlr.ModelLexer
import com.github.sybila.checker.antlr.ModelParser
import com.github.sybila.checker.new.*
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.HUCTLParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

fun String.asExperiment(): () -> String? {
    val parser = ModelParser(CommonTokenStream(ModelLexer(ANTLRInputStream(this))))
    val experiment = ModelContext()
    ParseTreeWalker().walk(experiment, parser.root())
    return {
        val paramsMapping = experiment.params.toList()
        val stateMapping = experiment.states.flatMap { it }
        val fullParams = (0..Math.max(0, paramsMapping.size - 1)).toSet()
        val partitionMapping = experiment.states.mapIndexed { i, set ->
            i to set.map { stateMapping.indexOf(it) }
        }.toMap()
        val partitions = experiment.states.mapIndexed { i, set ->
            ExplicitPartitionFunction(i, inverseMapping = partitionMapping)
        }

        val solvers = partitions.map { EnumeratedSolver(fullParams) }

        val fragments: List<Pair<Fragment<Set<Int>>, Solver<Set<Int>>>> = partitions.zip(solvers).map {
            val (partition, solver) = it
            val transitionFunction: Map<Int, List<Transition<Set<Int>>>>
                    = experiment.edges.groupBy { stateMapping.indexOf(it.from) }
                    .mapValues {
                        it.value.map {
                            val (from, to, dir, bound) = it
                            Transition(stateMapping.indexOf(it.to), dir, if (bound.isEmpty()) {
                                solver.tt
                            } else bound.map { paramsMapping.indexOf(it) }.toSet())
                        }
                    }
            val atom: Map<Formula.Atom, Map<Int, Set<Int>>> = experiment.atom.map {
                val (atom, map) = it
                atom to map.mapKeys { stateMapping.indexOf(it.key) }.mapValues {
                    it.value.map { paramsMapping.indexOf(it) }.toSet()
                }
            }.toMap()
            ExplicitFragment(partition, transitionFunction, atom, solver) to solver
        }

        val checker = Checker(SharedMemComm(fragments.size), fragments)

        experiment.assert.forEach {
            val (formula, input) = it

            val globalSolver = EnumeratedSolver(fullParams)

            val expected: StateMap<Set<Int>> = input.mapKeys {
                stateMapping.indexOf(it.key)
            }.mapValues {
                it.value.map { paramsMapping.indexOf(it) }.toSet()
            }.asStateMap(globalSolver.ff)

            val result = checker.verify(formula)

            if (!deepEquals(expected to globalSolver, result.zip(solvers))) {
                throw IllegalStateException("Expected $expected, but got $result")
            }
        }

        experiment.verify.forEach {
            println(checker.verify(it))
        }
        null
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
        assert.add(parser.formula(ctx.STRING().readString()) to ctx.stateParams().map {
            it.state().text to it.param().map { it.text }.toSet()
        }.toMap())
    }

    private fun TerminalNode.readString() = this.text.filter { it != '"' }

}