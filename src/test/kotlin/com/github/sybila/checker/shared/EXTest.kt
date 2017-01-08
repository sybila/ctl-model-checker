package com.github.sybila.checker.shared

import com.github.sybila.checker.shared.solver.asParams
import com.github.sybila.huctl.EX
import org.junit.Test
import java.util.*

private val zero = BitSet().apply { set(0) }.asParams()

class SequentialExistsNextTest {

    @Test(timeout = 500)
    fun oneStateModel() {
        ReachModel(1, 1).run {
            Checker(this, parallelism = 1).use { checker ->
                val expected = 0.asStateMap(BitSet().apply { set(1) }.asParams())

                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.CENTER())))
                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.BORDER())))
            }
        }
    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            Checker(this, parallelism = 1).use { checker ->

                0.asStateMap(zero.not()).assertDeepEquals(checker.verify(EX(ReachModel.Prop.LOWER_CORNER())))

                mapOf(
                        chainSize - 1 to BitSet().apply { set(chainSize) }.asParams(),
                        chainSize - 2 to BitSet().apply { for (p in 0..(chainSize-2)) set(p) }.asParams()

                ).asStateMap().assertDeepEquals(checker.verify(EX(ReachModel.Prop.UPPER_CORNER())))

                val expected = ReachModel.Prop.BORDER().eval().states.flatMap { state ->
                    state.predecessors(true).asSequence().map { it.target to it.bound }
                }.groupBy({ it.first }, { it.second }).mapValues { Or(it.value) }.asStateMap()

                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.BORDER())))

            }
        }
    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            Checker(this, parallelism = 1).use { checker ->

                0.asStateMap(zero.not()).assertDeepEquals(checker.verify(EX(ReachModel.Prop.LOWER_CORNER())))

                val upperCorner = ReachModel.Prop.UPPER_CORNER().eval().states.first()
                upperCorner.predecessors(true)
                        .asSequence().associateBy({it.target}, {it.bound})
                        .asStateMap()
                        .assertDeepEquals(checker.verify(EX(ReachModel.Prop.UPPER_CORNER())))

                val expected = ReachModel.Prop.BORDER().eval().states.flatMap { state ->
                    state.predecessors(true).asSequence().map { it.target to it.bound }
                }.groupBy({ it.first }, { it.second }).mapValues { Or(it.value) }.asStateMap()
                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.BORDER())))

            }
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

        ReachModel(dimensions, dimensionSize).run {
            Checker(this, parallelism = workers).use { checker ->

                0.asStateMap(zero.not()).assertDeepEquals(checker.verify(EX(ReachModel.Prop.LOWER_CORNER())))

                val upperCorner = ReachModel.Prop.UPPER_CORNER().eval().states.first()
                upperCorner.predecessors(true)
                        .asSequence().associateBy({it.target}, {it.bound})
                        .asStateMap()
                        .assertDeepEquals(checker.verify(EX(ReachModel.Prop.UPPER_CORNER())))

                val expected = ReachModel.Prop.BORDER().eval().states.flatMap { state ->
                    state.predecessors(true).asSequence().map { it.target to it.bound }
                }.groupBy({ it.first }, { it.second }).mapValues { Or(it.value) }.asStateMap()
                expected.assertDeepEquals(checker.verify(EX(ReachModel.Prop.BORDER())))

            }
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

    @Test(timeout = 2000)
    fun mediumAsymmetric1() = generalModel(3, 6)

    @Test(timeout = 2000)
    fun mediumAsymmetric2() = generalModel(6, 4)

    @Test(timeout = 5000)
    fun largeAsymmetric1() = generalModel(6, 5)

    @Test(timeout = 5000)
    fun largeAsymmetric2() = generalModel(5, 7)

}