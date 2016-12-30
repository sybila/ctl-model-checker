package com.github.sybila.checker

import com.github.sybila.checker.ReachModel.Prop.*
import com.github.sybila.checker.channel.connectWithSharedMemory
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.checker.partition.asUniformPartitions
import com.github.sybila.huctl.EX
import org.junit.Test
import java.util.*

private val zero = BitSet().apply { set(0) }

class SequentialExistsNextTest {

    @Test(timeout = 500)
    fun oneStateModel() {
        ReachModel(1, 1).run {
            SequentialChecker(this).use { checker ->
                val expected = 0.asStateMap(BitSet().apply { set(1) })

                expected.assertDeepEquals(checker.verify(EX(UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(EX(LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(EX(CENTER())))
                expected.assertDeepEquals(checker.verify(EX(BORDER())))
            }
        }
    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            SequentialChecker(this).use { checker ->

                0.asStateMap(zero.not()).assertDeepEquals(checker.verify(EX(LOWER_CORNER())))

                mapOf(
                        chainSize - 1 to BitSet().apply { set(chainSize) },
                        chainSize - 2 to BitSet().apply { for (p in 0..(chainSize-2)) set(p) }

                ).asStateMap().assertDeepEquals(checker.verify(EX(UPPER_CORNER())))

                val expected = HashStateMap(ff)
                BORDER().eval().states().asSequence().forEach { state ->
                    for ((p, t, bound) in state.predecessors(true)) {
                        expected.setOrUnion(p, bound)
                    }
                }
                expected.assertDeepEquals(checker.verify(EX(BORDER())))

            }
        }
    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            SequentialChecker(this).use { checker ->

                0.asStateMap(zero.not()).assertDeepEquals(checker.verify(EX(LOWER_CORNER())))

                val upperCorner = UPPER_CORNER().eval().states().next()
                upperCorner.predecessors(true)
                        .asSequence().associateBy({it.target}, {it.bound})
                        .asStateMap()
                        .assertDeepEquals(checker.verify(EX(UPPER_CORNER())))

                val expected = HashStateMap(ff)
                BORDER().eval().states().asSequence().forEach { state ->
                    for ((p, t, bound) in state.predecessors(true)) {
                        expected.setOrUnion(p, bound)
                    }
                }
                expected.assertDeepEquals(checker.verify(EX(BORDER())))

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
            SequentialChecker(this).use { sequential ->

                val partitions = (0 until workers).map { ReachModel(dimensions, dimensionSize) }.asUniformPartitions()

                Checker(partitions.connectWithSharedMemory()).use { parallel ->

                    val formulas = listOf(
                            EX(LOWER_CORNER()),
                            EX(UPPER_CORNER()),
                            EX(BORDER())
                    )

                    formulas.forEach {
                        sequential.verify(it).assertDeepEquals(partitions.zip(parallel.verify(it)))
                    }

                }
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