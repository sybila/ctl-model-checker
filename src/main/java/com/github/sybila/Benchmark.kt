package com.github.sybila

import com.github.sybila.algorithm.HybridLogic
import com.github.sybila.algorithm.TemporalLogic
import com.github.sybila.algorithm.asMono
import com.github.sybila.model.StateMap
import com.github.sybila.model.TransitionSystem
import com.github.sybila.model.ode.Grid2TransitionSystem
import com.github.sybila.model.toStateMap
import com.github.sybila.ode.model.OdeModel
import com.github.sybila.ode.model.Parser
import com.github.sybila.ode.model.Summand
import com.github.sybila.solver.Solver
import com.github.sybila.solver.grid.Grid2
import com.github.sybila.solver.grid.Grid2Solver
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

val model_2D_2P = OdeModel(
        variables = listOf(modelVarOneParam(0, 0), modelVarOneParam(1, 1)),
        parameters = listOf(modelParam(0), modelParam(1))
)


private fun modelParam(index: Int) = OdeModel.Parameter(
        name = "p$index",
        range = 0.0 to 10.0
)

private fun modelVarNoParam(index: Int) = OdeModel.Variable(
        name = "v$index",
        range = 0.0 to 10.0,
        thresholds = listOf(0.0, 10.0),
        varPoints = null,
        equation = listOf(
                Summand(constant = -1.0, variableIndices = listOf(0)),
                Summand(constant = 5.5)
        )
)

private fun modelVarOneParam(index: Int, param: Int) = OdeModel.Variable(
        name = "v$index",
        range = 0.0 to 10.0,
        thresholds = listOf(0.0, 10.0),
        varPoints = null,
        equation = listOf(
                Summand(constant = -1.0, variableIndices = listOf(0)),
                Summand(paramIndex = param)
        )
)

private fun Pair<Double, Double>.splitInto(stateCount: Int): List<Double> {
    val min = this.first
    val max = this.second

    val step = (max - min) / stateCount

    val result = ArrayList<Double>(stateCount + 1)
    result.add(min)
    while (result.size < stateCount) {
        result.add(result.last() + step)
    }
    result.add(max)

    return result
}

//val extraThresholds = listOf(5.0, 6.0)  // added because of properties

fun main(args: Array<String>) {

    val timeLimit = args[0].toLong()
    val modelPrototype = Parser().parse(File(args[1])) //model_2D_2P
    val parallelism = args[2].toInt()
    val scheduler = Schedulers.newParallel("my-parallel", parallelism)//args[1].toInt())

    var varIndex = 0
    val stateCounts = modelPrototype.variables.map { 1 }.toMutableList()
    val results = ArrayList<Pair<Int, Long>>()
    try {
        do {
            //increase state count
            stateCounts[varIndex] = (stateCounts[varIndex] * 1.1).toInt() + 1
            varIndex = (varIndex + 1) % stateCounts.size
            val model = modelPrototype.copy(
                    variables = modelPrototype.variables.zip(stateCounts).map { (variable, count) ->
                        variable.copy(
                                thresholds = variable.range.splitInto(count).toSet().toList().sorted()
                        )
                    }
            )

            val solver = Grid2Solver(model.parameters[0].range, model.parameters[1].range)

            // also computes transitions!
            val transitionSystem = Grid2TransitionSystem(model, solver, scheduler)

            // repeat 5 times and take average
            val measuredTime = (1..5).map {
                measureTimeMillis {
                    val center = mapOf(transitionSystem.stateCount / 2 to solver.tt).toStateMap(solver, transitionSystem.stateCount)

                    object : TemporalLogic<Grid2>, HybridLogic<Grid2>, TransitionSystem<Grid2> by transitionSystem {
                        override val scheduler: Scheduler = scheduler
                        override val solver: Solver<Grid2> = solver
                        override val fork: Int = parallelism + 1
                    }.run {
                        //existsFinally(center.asMono()).block()
                        val inner: Iterable<Mono<Pair<Int, StateMap<Int, Grid2>>>> = object : Iterable<Mono<Pair<Int, StateMap<Int, Grid2>>>> {
                            override fun iterator(): Iterator<Mono<Pair<Int, StateMap<Int, Grid2>>>> = object : Iterator<Mono<Pair<Int, StateMap<Int, Grid2>>>> {

                                private var state = 0

                                override fun hasNext(): Boolean = state < transitionSystem.stateCount

                                override fun next(): Mono<Pair<Int, StateMap<Int, Grid2>>> {
                                    val state = this.state
                                    this.state += 1
                                    return existsNext(existsFinally(
                                            mapOf(state to solver.tt).toStateMap(solver, stateCount).asMono()
                                    )).map { state to it }
                                }
                            }
                        }
                        bind(Flux.fromIterable(inner)).block()
                    }
                }
            }.sum() / 5

            results.add(transitionSystem.stateCount to measuredTime)

            println("${transitionSystem.stateCount} in $measuredTime")
            System.gc()
        } while (measuredTime < timeLimit)
    } finally {
        // print even on error
        for ((s, t) in results) {
            println("$s \t $t")
        }
        scheduler.dispose()
    }


}