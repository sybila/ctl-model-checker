package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.new.*
import com.github.sybila.huctl.EX
import org.junit.Test
import java.util.*

class SequentialExistsNextTest {

    @Test(timeout = 500)
    fun oneStateModel() {

        val model = ReachModel(1, 1)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        val expected = mapOf(0 to setOf(1)).asStateMap(solver.ff)

        assertDeepEquals(expected, checker.verify(EX(UPPER_CORNER()))[0], solver)
        assertDeepEquals(expected, checker.verify(EX(LOWER_CORNER()))[0], solver)
        assertDeepEquals(expected, checker.verify(EX(CENTER()))[0], solver)
        assertDeepEquals(expected, checker.verify(EX(BORDER()))[0], solver)

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(
                mapOf(0 to model.parameters - 0).asStateMap(solver.ff),
                checker.verify(EX(LOWER_CORNER()))[0], solver
        )

        assertDeepEquals(
                mapOf(chainSize - 1 to setOf(chainSize), chainSize - 2 to (0 until (chainSize - 1)).toSet()).asStateMap(solver.ff),
                checker.verify(EX(UPPER_CORNER()))[0], solver
        )

        solver.apply {
            val expected = HashMap<Int, Set<Int>>()
            model.eval(BORDER()).forEach {
                for ((p, t, bound) in model.step(it, false)) {
                    if (p in expected) {
                        expected[p] = expected[p]!! or bound
                    } else {
                        expected[p] = bound
                    }
                }
            }
            assertDeepEquals(expected.asStateMap(ff), checker.verify(EX(BORDER()))[0], solver)
        }
    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(
                mapOf(0 to model.parameters - 0).asStateMap(solver.ff),
                checker.verify(EX(LOWER_CORNER()))[0], solver
        )

        val upperCorner = model.eval(ReachModel.Prop.UPPER_CORNER()).first()
        assertDeepEquals(
                model.step(upperCorner, false).asSequence().associateBy({it.target}, {it.bound}).asStateMap(solver.ff),
                checker.verify(EX(UPPER_CORNER()))[0], solver
        )

        solver.apply {
            val expected = HashMap<Int, Set<Int>>()
            model.eval(BORDER()).forEach {
                for ((p, t, bound) in model.step(it, false)) {
                    if (p in expected) {
                        expected[p] = expected[p]!! or bound
                    } else {
                        expected[p] = bound
                    }
                }
            }
            assertDeepEquals(expected.asStateMap(ff), checker.verify(EX(BORDER()))[0], solver)
        }

    }

    @Test
    fun tinyChainTest() = chainModel(2)

    @Test
    fun smallChainTest() = chainModel(10)

    @Test
    fun largeChainTest() = chainModel(1000)

    @Test
    fun smallCube() = generalModel(2, 2)

    @Test
    fun mediumCube() = generalModel(3, 3)

    @Test
    fun largeCube() = generalModel(5, 5)

    @Test
    fun smallAsymmetric1() = generalModel(2, 4)

    @Test
    fun smallAsymmetric2() = generalModel(4, 2)

    @Test
    fun mediumAsymmetric1() = generalModel(3, 6)

    @Test
    fun mediumAsymmetric2() = generalModel(6, 4)

    @Test
    fun largeAsymmetric1() = generalModel(6, 5)

    @Test
    fun largeAsymmetric2() = generalModel(5, 7)

}

class SmallConcurrentExistsNextTest : ConcurrentExistsNextTest() {
    override val workers: Int = 2
}

class MediumConcurrentExistsNextTest : ConcurrentExistsNextTest() {
    override val workers: Int = 4
}

class LargeConcurrentExistsNextTest : ConcurrentExistsNextTest() {
    override val workers: Int = 8
}

abstract class ConcurrentExistsNextTest {

    protected abstract val workers: Int

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val stateCount = pow(dimensionSize, dimensions)

        //global checker is verified in sequential tests
        val globalModel = ReachModel(dimensions, dimensionSize)
        val globalSolver = EnumeratedSolver(globalModel.parameters)
        val globalChecker = Checker(globalModel, globalSolver)

        //This might not work for the last state if it's not rounded correctly
        val const = Math.ceil(stateCount.toDouble() / workers.toDouble()).toInt()

        val partitions = (0 until workers).map { myId ->
            if (const == 0) UniformPartitionFunction(myId) else
                FunctionalPartitionFunction(myId) { it / const }
        }

        val models = partitions.map {
            ReachModel(dimensions, dimensionSize, it)
        }
        val solvers = models.map { EnumeratedSolver(it.parameters) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        assertDeepEquals(
                globalChecker.verify(EX(LOWER_CORNER()))[0] to globalSolver,
                checker.verify(EX(LOWER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(EX(UPPER_CORNER()))[0] to globalSolver,
                checker.verify(EX(UPPER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(EX(BORDER()))[0] to globalSolver,
                checker.verify(EX(BORDER())).zip(solvers)
        )

    }

    @Test(timeout = 1000)
    fun smallCube() = generalModel(2, 2)

    @Test(timeout = 1000)
    fun mediumCube() = generalModel(3, 3)

    @Test(timeout = 1000)
    fun largeCube() = generalModel(5, 5)

    @Test(timeout = 1000)
    fun smallAsymmetric1() = generalModel(2, 4)

    @Test(timeout = 1000)
    fun smallAsymmetric2() = generalModel(4, 2)

    @Test(timeout = 2000)
    fun mediumAsymmetric1() = generalModel(3, 6)

    @Test(timeout = 2000)
    fun mediumAsymmetric2() = generalModel(6, 4)

    @Test(timeout = 5000)
    fun largeAsymmetric1() = generalModel(6, 5)

    @Test(timeout = 5000)
    fun largeAsymmetric2() = generalModel(5, 7)

}