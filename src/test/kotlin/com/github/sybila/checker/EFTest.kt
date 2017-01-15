package com.github.sybila.checker

import com.github.sybila.checker.map.asStateMap
import com.github.sybila.checker.map.lazyAnd
import com.github.sybila.checker.map.lazyOr
import com.github.sybila.huctl.EF
import com.github.sybila.huctl.EU
import com.github.sybila.huctl.False
import org.junit.Test

class SequentialExistsFutureTest {

    @Test
    fun oneStateModel() {

        ReachModel(1,1).run {
            Checker(this, parallelism = 1).use { checker ->
                val expected = 0.asStateMap(TT)

                expected.assertDeepEquals(checker.verify(EF(ReachModel.Prop.UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(EF(ReachModel.Prop.LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(EF(ReachModel.Prop.CENTER())))
                expected.assertDeepEquals(checker.verify(EF(ReachModel.Prop.BORDER())))
            }
        }

    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            Checker(this, parallelism = 1).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(EF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.BORDER())))
            }
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            Checker(this, parallelism = 1).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(EF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.BORDER())))

                ReachModel.Prop.BORDER().eval().assertDeepEquals(checker.verify(False EU ReachModel.Prop.BORDER()))

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr (reach lazyAnd ReachModel.Prop.BORDER().eval())).assertDeepEquals(
                        checker.verify(ReachModel.Prop.BORDER() EU ReachModel.Prop.UPPER_CORNER())
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
            Checker(this, parallelism = workers).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(EF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(EF(ReachModel.Prop.BORDER())))

                ReachModel.Prop.BORDER().eval().assertDeepEquals(checker.verify(False EU ReachModel.Prop.BORDER()))

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr (reach lazyAnd ReachModel.Prop.BORDER().eval())).assertDeepEquals(
                        checker.verify(ReachModel.Prop.BORDER() EU ReachModel.Prop.UPPER_CORNER())
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