package com.github.sybila.checker

import com.github.daemontus.egholm.collections.listWithInitial
import com.github.sybila.ctl.AU
import com.github.sybila.ctl.False
import com.github.sybila.ctl.True
import org.junit.Test
import kotlin.test.assertEquals

class SequentialAllUntilTest {

    @Test
    fun oneStateModel() {

        val model = ReachModel(1, 1)

        withSingleModelChecker(model) {
            val expected = nodesOf(Pair(IDNode(0), IDColors(0, 1)))
            assertEquals(expected, it.verify(True AU ReachModel.Prop.UPPER_CORNER))
            assertEquals(expected, it.verify(True AU ReachModel.Prop.LOWER_CORNER))
            assertEquals(expected, it.verify(True AU ReachModel.Prop.CENTER))
            assertEquals(expected, it.verify(True AU ReachModel.Prop.BORDER))
        }

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)

        withSingleModelChecker(model) {
            assertEquals(it.verify(ReachModel.Prop.LOWER_CORNER), it.verify(True AU ReachModel.Prop.LOWER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(True AU ReachModel.Prop.UPPER_CORNER))
            //everyone is border
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.BORDER), it.verify(True AU ReachModel.Prop.BORDER))
            assertEquals(it.verify(ReachModel.Prop.BORDER), it.verify(False AU ReachModel.Prop.BORDER))
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)

        withSingleModelChecker(model) {
            assertEquals(it.verify(ReachModel.Prop.LOWER_CORNER), it.verify(True AU ReachModel.Prop.LOWER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(True AU ReachModel.Prop.UPPER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.BORDER), it.verify(True AU ReachModel.Prop.BORDER))
            assertEquals(it.verify(ReachModel.Prop.BORDER), it.verify(False AU ReachModel.Prop.BORDER))

            if (dimensionSize > 2) {
                //if dimension is only of size 2, we can actually reach more!
                assertEquals(nodesOf(
                        model.states.filter { state ->
                            (0 until dimensions).any { model.extractCoordinate(state, it) == dimensionSize - 1 }
                        }.map { Pair(it, model.stateColors(it)) }
                ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(ReachModel.Prop.BORDER AU ReachModel.Prop.UPPER_CORNER))
            } else {
                assertEquals(nodesOf(
                        model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
                ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(ReachModel.Prop.BORDER AU ReachModel.Prop.UPPER_CORNER))
            }

            assertEquals(nodesOf(
                    model.states.filter { state ->
                        (0 until dimensions).all { model.extractCoordinate(state, it) >= dimensionSize / 2 }
                    }.map { Pair(it, model.stateColors(it)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(ReachModel.Prop.UPPER_HALF AU ReachModel.Prop.UPPER_CORNER))
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

        val model = ReachModel(dimensions, dimensionSize)

        //This might not work for the last state if it's not rounded correctly
        val const = Math.ceil(model.stateCount.toDouble() / workers.toDouble()).toInt()

        val partitions = (0 until workers).map { myId ->
            if (const == 0) UniformPartitionFunction<IDNode>(myId) else
                FunctionalPartitionFunction<IDNode>(myId) { it.id / const }
        }

        val fragments = partitions.map {
            ReachModelPartition(model, it)
        }

        val result = withModelCheckers(
                fragments, { partitions[it] }
        ) {
            listOf(
                    it.verify(True AU ReachModel.Prop.LOWER_CORNER),
                    it.verify(True AU ReachModel.Prop.UPPER_CORNER),
                    it.verify(True AU ReachModel.Prop.BORDER),
                    it.verify(False AU ReachModel.Prop.BORDER),
                    it.verify(ReachModel.Prop.BORDER AU ReachModel.Prop.UPPER_CORNER),
                    it.verify(ReachModel.Prop.UPPER_HALF AU ReachModel.Prop.UPPER_CORNER)
            )
        }.fold(listWithInitial(6, nodesOf())) { l, r -> l.zip(r).map { it.first union it.second } }

        assertEquals(model.validNodes(ReachModel.Prop.LOWER_CORNER), result[0])
        assertEquals(nodesOf(
                model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
        ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[1])
        assertEquals(nodesOf(
                model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
        ) + model.validNodes(ReachModel.Prop.BORDER), result[2])
        assertEquals(model.validNodes(ReachModel.Prop.BORDER), result[3])

        if (dimensionSize > 2) {    //if dimension is only of size 2, we can actually reach more!
            assertEquals(nodesOf(
                    model.states.filter { state ->
                        (0 until dimensions).any { model.extractCoordinate(state, it) == dimensionSize - 1 }
                    }.map { Pair(it, model.stateColors(it)) }
            ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[4])
        } else {
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[4])
        }

        assertEquals(nodesOf(
                model.states.filter { state ->
                    (0 until dimensions).all { model.extractCoordinate(state, it) >= dimensionSize / 2 }
                }.map { Pair(it, model.stateColors(it)) }
        ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[5])

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