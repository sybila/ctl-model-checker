package com.github.sybila.checker

//import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.new.*
import com.github.sybila.huctl.*
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

/*
class SequentialAllFutureTest {

    @Test
    fun oneStateModel() {

        val model = ReachModel(1, 1)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        val expected = mapOf(0 to setOf(0, 1)).asStateMap(solver.ff)

        assertEquals(expected, checker.verify(AF(UPPER_CORNER()))[0])
        assertEquals(expected, checker.verify(AF(LOWER_CORNER()))[0])
        assertEquals(expected, checker.verify(AF(CENTER()))[0])
        assertEquals(expected, checker.verify(AF(BORDER()))[0])

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(model.eval(LOWER_CORNER()), checker.verify(AF(LOWER_CORNER()))[0], solver)

        val afUpperCorner = HashMap<Int, Set<Int>>()
        model.eval(UPPER_CORNER()).forEach { afUpperCorner[it] = solver.tt }
        model.eval(True).forEach { if (it !in afUpperCorner) afUpperCorner[it] = model.stateColors(it) }

        assertDeepEquals(afUpperCorner.asStateMap(solver.ff), checker.verify(AF(UPPER_CORNER()))[0], solver)

        val afBorder = HashMap<Int, Set<Int>>()
        model.eval(BORDER()).forEach { afBorder[it] = solver.tt }
        model.eval(True).forEach { if (it !in afBorder) afBorder[it] = model.stateColors(it) }

        assertDeepEquals(afBorder.asStateMap(solver.ff), checker.verify(AF(BORDER()))[0], solver)

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)
        val solver = EnumeratedSolver(model.parameters)

        val checker = Checker(model, solver)

        assertDeepEquals(model.eval(LOWER_CORNER()), checker.verify(AF(LOWER_CORNER()))[0], solver)

        val afUpperCorner = HashMap<Int, Set<Int>>()
        model.eval(True).forEach { afUpperCorner[it] = model.stateColors(it) }
        model.eval(UPPER_CORNER()).forEach { afUpperCorner[it] = solver.tt }
        assertDeepEquals(afUpperCorner.asStateMap(solver.ff), checker.verify(AF(UPPER_CORNER()))[0], solver)

        val afBorder = HashMap<Int, Set<Int>>()
        model.eval(True).forEach { afBorder[it] = model.stateColors(it) }
        model.eval(BORDER()).forEach { afBorder[it] = solver.tt }
        assertDeepEquals(afBorder.asStateMap(solver.ff), checker.verify(AF(BORDER()))[0], solver)

        assertDeepEquals(model.eval(BORDER()), checker.verify(False AU BORDER())[0], solver)

        val auBorder = HashMap<Int, Set<Int>>()
        if (dimensionSize > 2) {
            model.states.filter { state ->
                (0 until dimensions).any { model.extractCoordinate(state, it) == dimensionSize - 1 }
            }.forEach { auBorder[it] = model.stateColors(it) }
        } else {
            model.eval(True).forEach { auBorder[it] = model.stateColors(it) }
        }
        model.eval(UPPER_CORNER()).forEach { auBorder[it] = solver.tt }
        assertDeepEquals(auBorder.asStateMap(solver.ff), checker.verify(BORDER() AU UPPER_CORNER())[0], solver)

        val auHalf = HashMap<Int, Set<Int>>()
        model.states.filter { state ->
            (0 until dimensions).all { model.extractCoordinate(state, it) >= dimensionSize / 2 }
        }.forEach { auHalf[it] = model.stateColors(it) }
        model.eval(UPPER_CORNER()).forEach { auHalf[it] = solver.tt }
        assertDeepEquals(auHalf.asStateMap(solver.ff), checker.verify(UPPER_HALF() AU UPPER_CORNER())[0], solver)

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

class SmallConcurrentAllUntilTest : ConcurrentAllUntilTest() {
    override val workers: Int = 2
}

class MediumConcurrentAllUntilTest : ConcurrentAllUntilTest() {
    override val workers: Int = 4
}

class LargeConcurrentAllUntilTest : ConcurrentAllUntilTest() {
    override val workers: Int = 8
}

abstract class ConcurrentAllUntilTest {

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

        val checker = Checker(SharedMemChannel(models.size), models.zip(solvers))

        assertDeepEquals(
                globalChecker.verify(AF(LOWER_CORNER()))[0] to globalSolver,
                checker.verify(AF(LOWER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(AF(UPPER_CORNER()))[0] to globalSolver,
                checker.verify(AF(UPPER_CORNER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(AF(BORDER()))[0] to globalSolver,
                checker.verify(AF(BORDER())).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(False AU BORDER())[0] to globalSolver,
                checker.verify(False AU BORDER()).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(BORDER() AU UPPER_CORNER())[0] to globalSolver,
                checker.verify(BORDER() AU UPPER_CORNER()).zip(solvers)
        )

        assertDeepEquals(
                globalChecker.verify(UPPER_HALF() AU UPPER_CORNER())[0] to globalSolver,
                checker.verify(UPPER_HALF() AU UPPER_CORNER()).zip(solvers)
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

}*/