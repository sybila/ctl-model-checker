package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.new.*
import com.github.sybila.huctl.EF
import com.github.sybila.huctl.EU
import com.github.sybila.huctl.False
import com.github.sybila.huctl.True
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class SequentialExistsFutureTest {

    @Test
    fun oneStateModel() {

        val model = ReachModel(1, 1)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        val expected = mapOf(0 to setOf(0, 1)).asStateMap(solver.ff)

        assertEquals(expected, checker.verify(EF(UPPER_CORNER()))[0])
        assertEquals(expected, checker.verify(EF(LOWER_CORNER()))[0])
        assertEquals(expected, checker.verify(EF(CENTER()))[0])
        assertEquals(expected, checker.verify(EF(BORDER()))[0])

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(model.eval(ReachModel.Prop.LOWER_CORNER()), checker.verify(EF(LOWER_CORNER()))[0], solver)

        val efUpperCorner = HashMap<Int, Set<Int>>()
        model.eval(UPPER_CORNER()).forEach { efUpperCorner[it] = solver.tt }
        model.eval(True).forEach { if (it !in efUpperCorner) efUpperCorner[it] = model.stateColors(it) }

        assertDeepEquals(efUpperCorner.asStateMap(solver.ff), checker.verify(EF(UPPER_CORNER()))[0], solver)

        val efBorder = HashMap<Int, Set<Int>>()
        model.eval(BORDER()).forEach { efBorder[it] = solver.tt }
        model.eval(True).forEach { if (it !in efBorder) efBorder[it] = model.stateColors(it) }

        assertDeepEquals(efBorder.asStateMap(solver.ff), checker.verify(EF(BORDER()))[0], solver)

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(model.eval(LOWER_CORNER()), checker.verify(EF(LOWER_CORNER()))[0], solver)

        val efUpperCorner = HashMap<Int, Set<Int>>()
        model.eval(True).forEach { efUpperCorner[it] = model.stateColors(it) }
        model.eval(UPPER_CORNER()).forEach { efUpperCorner[it] = solver.tt }
        assertDeepEquals(efUpperCorner.asStateMap(solver.ff), checker.verify(EF(UPPER_CORNER()))[0], solver)

        val efBorder = HashMap<Int, Set<Int>>()
        model.eval(True).forEach { efBorder[it] = model.stateColors(it) }
        model.eval(BORDER()).forEach { efBorder[it] = solver.tt }
        assertDeepEquals(efBorder.asStateMap(solver.ff), checker.verify(EF(BORDER()))[0], solver)

        assertDeepEquals(model.eval(BORDER()), checker.verify(False EU BORDER())[0], solver)

        val euBorder = HashMap<Int, Set<Int>>()
        model.eval(BORDER()).forEach { euBorder[it] = model.stateColors(it) }
        model.eval(UPPER_CORNER()).forEach { euBorder[it] = solver.tt }
        assertDeepEquals(euBorder.asStateMap(solver.ff), checker.verify(BORDER() EU UPPER_CORNER())[0], solver)
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

class SmallConcurrentExistsFutureTest : ConcurrentExistsFutureTest() {
    override val workers: Int = 2
}

class MediumConcurrentExistsFutureTest : ConcurrentExistsFutureTest() {
    override val workers: Int = 4
}

class LargeConcurrentExistsFutureTest : ConcurrentExistsFutureTest() {
    override val workers: Int = 8
}

abstract class ConcurrentExistsFutureTest {

    protected abstract val workers: Int

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val globalModel = ReachModel(dimensions, dimensionSize)
        val globalSolver = EnumeratedSolver(globalModel.parameters)
        val globalChecker = Checker(globalModel, globalSolver)

        //This might not work for the last state if it's not rounded correctly
        val const = Math.ceil(globalModel.stateCount.toDouble() / workers.toDouble()).toInt()

        val partitions = (0 until workers).map { myId ->
            if (const == 0) UniformPartitionFunction(myId) else
                FunctionalPartitionFunction(myId) { it / const }
        }

        val models = partitions.map { ReachModel(dimensions, dimensionSize, it) }
        val solvers = models.map { EnumeratedSolver(it.parameters) }

        val checker = Checker(SharedMemComm(models.size), models.zip(solvers))

        assertDeepEquals(
                globalChecker.verify(EF(LOWER_CORNER()))[0] to globalSolver,
                checker.verify(EF(LOWER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(EF(UPPER_CORNER()))[0] to globalSolver,
                checker.verify(EF(UPPER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(EF(BORDER()))[0] to globalSolver,
                checker.verify(EF(BORDER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(False EU BORDER())[0] to globalSolver,
                checker.verify(False EU BORDER()).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(BORDER() EU UPPER_CORNER())[0] to globalSolver,
                checker.verify(BORDER() EU UPPER_CORNER()).zip(solvers)
        )

    }

    @Test(timeout = 1000)
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