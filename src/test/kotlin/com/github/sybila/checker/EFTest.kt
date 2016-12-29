package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.channel.connectWithSharedMemory
import com.github.sybila.checker.map.mutable.ContinuousStateMap
import com.github.sybila.checker.partition.asUniformPartitions
import com.github.sybila.huctl.EF
import com.github.sybila.huctl.EU
import com.github.sybila.huctl.False
import org.junit.Test


class SequentialExistsFutureTest {

    @Test
    fun oneStateModel() {

        ReachModel(1,1).run {
            SequentialChecker(this).use { checker ->
                val expected = 0.asStateMap(tt)

                expected.assertDeepEquals(checker.verify(EF(UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(EF(LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(EF(CENTER())))
                expected.assertDeepEquals(checker.verify(EF(BORDER())))
            }
        }

    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            SequentialChecker(this).use { checker ->

                LOWER_CORNER().eval().assertDeepEquals(checker.verify(EF(LOWER_CORNER())))

                val reach = ContinuousStateMap(0, stateCount, ff)
                (0 until stateCount).forEach { reach[it] = stateColors(it) }

                (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(UPPER_CORNER())))

                (BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(BORDER())))
            }
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            SequentialChecker(this).use { checker ->

                LOWER_CORNER().eval().assertDeepEquals(checker.verify(EF(LOWER_CORNER())))

                val reach = ContinuousStateMap(0, stateCount, ff)
                (0 until stateCount).forEach { reach[it] = stateColors(it) }

                (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(UPPER_CORNER())))

                (BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(BORDER())))

                BORDER().eval().assertDeepEquals(checker.verify(False EU BORDER()))

                (UPPER_CORNER().eval() lazyOr (reach lazyAnd BORDER().eval())).assertDeepEquals(
                        checker.verify(BORDER() EU UPPER_CORNER())
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

        ReachModel(dimensions, dimensionSize).run {
            SequentialChecker(this).use { sequential ->

                val partitions = (0 until workers).map { ReachModel(dimensions, dimensionSize) }.asUniformPartitions()

                Checker(partitions.connectWithSharedMemory()).use { parallel ->

                    LOWER_CORNER().eval().assertDeepEquals(partitions.zip(parallel.verify(EF(LOWER_CORNER()))))

                    val reach = ContinuousStateMap(0, stateCount, ff)
                    (0 until stateCount).forEach { reach[it] = stateColors(it) }

                    (UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(
                            partitions.zip(parallel.verify(EF(UPPER_CORNER())))
                    )

                    (BORDER().eval() lazyOr reach).assertDeepEquals(
                            partitions.zip(parallel.verify(EF(BORDER())))
                    )

                    BORDER().eval().assertDeepEquals(
                            partitions.zip(parallel.verify(False EU BORDER()))
                    )

                    (UPPER_CORNER().eval() lazyOr (reach lazyAnd BORDER().eval())).assertDeepEquals(
                            partitions.zip(parallel.verify(BORDER() EU UPPER_CORNER()))
                    )

                }
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