package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.channel.connectWithSharedMemory
import com.github.sybila.checker.map.mutable.ContinuousStateMap
import com.github.sybila.checker.partition.asUniformPartitions
import com.github.sybila.huctl.AF
import com.github.sybila.huctl.AU
import com.github.sybila.huctl.False
import org.junit.Test


class SequentialAllFutureTest {

    @Test
    fun oneStateModel() {

        ReachModel(1,1).run {
            SequentialChecker(this).use { checker ->
                val expected = 0.asStateMap(tt)

                expected.assertDeepEquals(checker.verify(AF(UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(AF(LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(AF(CENTER())))
                expected.assertDeepEquals(checker.verify(AF(BORDER())))
            }
        }

    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            SequentialChecker(this).use { checker ->

                LOWER_CORNER().eval().assertDeepEquals(checker.verify(AF(LOWER_CORNER())))

                val reach = ContinuousStateMap(0, stateCount, ff)
                (0 until stateCount).forEach { reach[it] = stateColors(it) }

                (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(UPPER_CORNER())))

                (BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(BORDER())))
            }
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            SequentialChecker(this).use { checker ->

                LOWER_CORNER().eval().assertDeepEquals(checker.verify(AF(LOWER_CORNER())))

                val reach = ContinuousStateMap(0, stateCount, ff)
                (0 until stateCount).forEach { reach[it] = stateColors(it) }

                (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(UPPER_CORNER())))

                (BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(BORDER())))

                BORDER().eval().assertDeepEquals(checker.verify(False AU BORDER()))

                if (dimensionSize > 2) {
                    val border = (0 until stateCount).filter { state ->
                        (0 until dimensions).any { extractCoordinate(state, it) == dimensionSize - 1 }
                    }.associateBy({it}, { stateColors(it) }).asStateMap()
                    (UPPER_CORNER().eval() lazyOr border).assertDeepEquals(
                            checker.verify(BORDER() AU UPPER_CORNER())
                    )
                } else {
                    (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(BORDER() AU UPPER_CORNER()))
                }

                (UPPER_CORNER().eval() lazyOr (reach lazyAnd UPPER_HALF().eval())).assertDeepEquals(
                        checker.verify(UPPER_HALF() AU UPPER_CORNER())
                )
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

        ReachModel(dimensions, dimensionSize).run {

            val partitions = (0 until workers).map { ReachModel(dimensions, dimensionSize) }.asUniformPartitions()

            Checker(partitions.connectWithSharedMemory()).use { parallel ->

                LOWER_CORNER().eval().assertDeepEquals(
                        partitions.zip(parallel.verify(AF(LOWER_CORNER())))
                )

                val reach = ContinuousStateMap(0, stateCount, ff)
                (0 until stateCount).forEach { reach[it] = stateColors(it) }

                (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(
                        partitions.zip(parallel.verify(AF(UPPER_CORNER())))
                )

                (BORDER().eval() lazyOr reach).assertDeepEquals(
                        partitions.zip(parallel.verify(AF(BORDER())))
                )

                BORDER().eval().assertDeepEquals(
                        partitions.zip(parallel.verify(False AU BORDER()))
                )

                if (dimensionSize > 2) {
                    val border = (0 until stateCount).filter { state ->
                        (0 until dimensions).any { extractCoordinate(state, it) == dimensionSize - 1 }
                    }.associateBy({it}, { stateColors(it) }).asStateMap()
                    (UPPER_CORNER().eval() lazyOr border).assertDeepEquals(
                            partitions.zip(parallel.verify(BORDER() AU UPPER_CORNER()))
                    )
                } else {
                    (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(
                            partitions.zip(parallel.verify(BORDER() AU UPPER_CORNER()))
                    )
                }

                (UPPER_CORNER().eval() lazyOr (reach lazyAnd UPPER_HALF().eval())).assertDeepEquals(
                        partitions.zip(parallel.verify(UPPER_HALF() AU UPPER_CORNER()))
                )
            }
        }

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