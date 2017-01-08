package com.github.sybila.checker.shared

import com.github.sybila.huctl.AF
import com.github.sybila.huctl.AU
import com.github.sybila.huctl.False
import org.junit.Test


class SequentialAllFutureTest {

    @Test
    fun oneStateModel() {

        ReachModel(1,1).run {
            Checker(this, parallelism = 1).use { checker ->
                val expected = 0.asStateMap(TT)

                expected.assertDeepEquals(checker.verify(AF(ReachModel.Prop.UPPER_CORNER())))
                expected.assertDeepEquals(checker.verify(AF(ReachModel.Prop.LOWER_CORNER())))
                expected.assertDeepEquals(checker.verify(AF(ReachModel.Prop.CENTER())))
                expected.assertDeepEquals(checker.verify(AF(ReachModel.Prop.BORDER())))
            }
        }

    }

    fun chainModel(chainSize: Int) {

        ReachModel(1, chainSize).run {
            Checker(this, parallelism = 1).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(AF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.BORDER())))
            }
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        ReachModel(dimensions, dimensionSize).run {
            Checker(this, parallelism = 1).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(AF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.BORDER())))

                ReachModel.Prop.BORDER().eval().assertDeepEquals(checker.verify(False AU ReachModel.Prop.BORDER()))

                if (dimensionSize > 2) {
                    val border = (0 until stateCount).filter { state ->
                        (0 until dimensions).any { extractCoordinate(state, it) == dimensionSize - 1 }
                    }.associateBy({it}, { stateColors(it) }).asStateMap()
                    (ReachModel.Prop.UPPER_CORNER().eval() lazyOr border).assertDeepEquals(
                            checker.verify(ReachModel.Prop.BORDER() AU ReachModel.Prop.UPPER_CORNER())
                    )
                } else {
                    (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(ReachModel.Prop.BORDER() AU ReachModel.Prop.UPPER_CORNER()))
                }

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr (reach lazyAnd ReachModel.Prop.UPPER_HALF().eval())).assertDeepEquals(
                        checker.verify(ReachModel.Prop.UPPER_HALF() AU ReachModel.Prop.UPPER_CORNER())
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

            Checker(this, parallelism = workers).use { checker ->

                ReachModel.Prop.LOWER_CORNER().eval().assertDeepEquals(checker.verify(AF(ReachModel.Prop.LOWER_CORNER())))

                val reach = Array<Params?>(stateCount) { stateColors(it) }.asStateMap()

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.UPPER_CORNER())))

                (ReachModel.Prop.BORDER().eval() lazyOr reach).assertDeepEquals(checker.verify(AF(ReachModel.Prop.BORDER())))

                ReachModel.Prop.BORDER().eval().assertDeepEquals(checker.verify(False AU ReachModel.Prop.BORDER()))

                if (dimensionSize > 2) {
                    val border = (0 until stateCount).filter { state ->
                        (0 until dimensions).any { extractCoordinate(state, it) == dimensionSize - 1 }
                    }.associateBy({it}, { stateColors(it) }).asStateMap()
                    (ReachModel.Prop.UPPER_CORNER().eval() lazyOr border).assertDeepEquals(
                            checker.verify(ReachModel.Prop.BORDER() AU ReachModel.Prop.UPPER_CORNER())
                    )
                } else {
                    (ReachModel.Prop.UPPER_CORNER().eval() lazyOr reach).assertDeepEquals(checker.verify(ReachModel.Prop.BORDER() AU ReachModel.Prop.UPPER_CORNER()))
                }

                (ReachModel.Prop.UPPER_CORNER().eval() lazyOr (reach lazyAnd ReachModel.Prop.UPPER_HALF().eval())).assertDeepEquals(
                        checker.verify(ReachModel.Prop.UPPER_HALF() AU ReachModel.Prop.UPPER_CORNER())
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