package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.new.*
import com.github.sybila.huctl.EX
import org.junit.Test
import java.util.*
import kotlin.test.assertFalse

class SequentialExistsNextTest {

    @Test(timeout = 500)
    fun oneStateModel() {

        val model = ReachModel(1, 1)
        val solver = EnumerativeSolver(model.parameters)

        val checker = Checker(listOf(model to solver))

        val expected = mapOf(0 to setOf(1)).asStateMap(solver.ff)

        expected.assertDeepEquals(checker.verify(EX(UPPER_CORNER()))[0], solver)
        expected.assertDeepEquals(checker.verify(EX(LOWER_CORNER()))[0], solver)
        expected.assertDeepEquals(checker.verify(EX(CENTER()))[0], solver)
        expected.assertDeepEquals(checker.verify(EX(BORDER()))[0], solver)

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)
        val solver = EnumerativeSolver(model.parameters)

        val checker = Checker(listOf(model to solver))

        mapOf(0 to model.parameters - 0).asStateMap(solver.ff).assertDeepEquals(
                checker.verify(EX(LOWER_CORNER()))[0], solver
        )
        mapOf(chainSize - 1 to setOf(chainSize), chainSize - 2 to (0 until (chainSize - 1)).toSet()).asStateMap(solver.ff).assertDeepEquals(
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
            expected.asStateMap(ff).assertDeepEquals(checker.verify(EX(BORDER()))[0], solver)
        }
    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)
        val solver = EnumerativeSolver(model.parameters)

        val checker = Checker(listOf(model to solver))

        mapOf(0 to model.parameters - 0).asStateMap(solver.ff).assertDeepEquals(
                checker.verify(EX(LOWER_CORNER()))[0], solver
        )

        val upperCorner = model.eval(ReachModel.Prop.UPPER_CORNER()).first()
        model.step(upperCorner, false).asSequence().associateBy({it.target}, {it.bound}).asStateMap(solver.ff).assertDeepEquals(
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
            expected.asStateMap(ff).assertDeepEquals(checker.verify(EX(BORDER()))[0], solver)
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

        //This might not work for the last state if it's not rounded correctly
        val const = Math.ceil(stateCount.toDouble() / workers.toDouble()).toInt()

        val partitions = (0 until workers).map { myId ->
            if (const == 0) UniformPartitionFunction(myId) else
                FunctionalPartitionFunction(myId) { it / const }
        }

        val models = partitions.map {
            ReachModel(dimensions, dimensionSize, it)
        }
        val solvers = models.map { EnumerativeSolver(it.parameters) }

        val checker = Checker(models.zip(solvers))

        val r1 = checker.verify(EX(LOWER_CORNER()))
        val r2 = checker.verify(EX(UPPER_CORNER()))
        val r3 = checker.verify(EX(BORDER()))

        for ((m, s) in models.zip(solvers)) {
            m.apply {
            s.apply {
                //r1
                if (m.id == 0) {
                    mapOf(0 to m.parameters - 0).asStateMap(ff).assertDeepEquals(r1[0], this)
                } else {
                    assertFalse(r1[m.id].iterator().hasNext())
                }
                //r2
                val expectedR2 = HashMap<Int, Set<Int>>()
                m.eval(UPPER_CORNER()).forEach {
                    for ((p, t, bound) in m.step(it, false)) {
                        if (p.owner() == id) {
                            if (p in expectedR2) {
                                expectedR2[p] = expectedR2[p]!! or bound
                            } else {
                                expectedR2[p] = bound
                            }
                        }
                    }
                }
                expectedR2.asStateMap(ff).assertDeepEquals(r2[id], this)
                //r3
                val expectedR3 = HashMap<Int, Set<Int>>()
                m.eval(BORDER()).forEach {
                    for ((p, t, bound) in m.step(it, false)) {
                        if (p.owner() == id) {
                            if (p in expectedR2) {
                                expectedR3[p] = expectedR3[p]!! or bound
                            } else {
                                expectedR3[p] = bound
                            }
                        }
                    }
                }
                expectedR3.asStateMap(ff).assertDeepEquals(r3[id], this)
            } }
        }

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

    @Test(timeout = 1000)
    fun mediumAsymmetric1() = generalModel(3, 6)

    @Test(timeout = 1000)
    fun mediumAsymmetric2() = generalModel(6, 4)

    @Test(timeout = 1000)
    fun largeAsymmetric1() = generalModel(6, 5)

    @Test(timeout = 1000)
    fun largeAsymmetric2() = generalModel(5, 7)

}