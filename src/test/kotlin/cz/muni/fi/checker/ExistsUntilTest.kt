package cz.muni.fi.checker

import cz.muni.fi.ctl.EU
import cz.muni.fi.ctl.False
import cz.muni.fi.ctl.True
import org.junit.Test
import kotlin.test.assertEquals

class SequentialExistsUntilTest {

    @Test
    fun oneStateModel() {

        val model = ReachModel(1, 1)

        withSingleModelChecker(model) {
            val expected = nodesOf(Pair(IDNode(0), IDColors(0, 1)))
            assertEquals(expected, it.verify(True EU ReachModel.Prop.UPPER_CORNER))
            assertEquals(expected, it.verify(True EU ReachModel.Prop.LOWER_CORNER))
            assertEquals(expected, it.verify(True EU ReachModel.Prop.CENTER))
            assertEquals(expected, it.verify(True EU ReachModel.Prop.BORDER))
        }

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)

        withSingleModelChecker(model) {
            assertEquals(it.verify(ReachModel.Prop.LOWER_CORNER), it.verify(True EU ReachModel.Prop.LOWER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(True EU ReachModel.Prop.UPPER_CORNER))
            //everyone is border
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.BORDER), it.verify(True EU ReachModel.Prop.BORDER))
            assertEquals(it.verify(ReachModel.Prop.BORDER), it.verify(False EU ReachModel.Prop.BORDER))
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)

        withSingleModelChecker(model) {
            assertEquals(it.verify(ReachModel.Prop.LOWER_CORNER), it.verify(True EU ReachModel.Prop.LOWER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(True EU ReachModel.Prop.UPPER_CORNER))
            assertEquals(nodesOf(
                    model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.BORDER), it.verify(True EU ReachModel.Prop.BORDER))
            assertEquals(it.verify(ReachModel.Prop.BORDER), it.verify(False EU ReachModel.Prop.BORDER))

            assertEquals(nodesOf(
                    it.verify(ReachModel.Prop.BORDER).entries.map { Pair(it.key, model.stateColors(it.key)) }
            ) + it.verify(ReachModel.Prop.UPPER_CORNER), it.verify(ReachModel.Prop.BORDER EU ReachModel.Prop.UPPER_CORNER))
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
    fun mediumCube() = generalModel(4, 4)

    @Test   //this can actually be kind of long! (7-10s)
    fun largeCube() = generalModel(6, 6)

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

class SmallConcurrentExistsUntilTest : ConcurrentExistsUntilTest() {
    override val workers: Int = 2
}

class MediumConcurrentExistsUntilTest : ConcurrentExistsUntilTest() {
    override val workers: Int = 4
}

class LargeConcurrentExistsUntilTest : ConcurrentExistsUntilTest() {
    override val workers: Int = 8
}

abstract class ConcurrentExistsUntilTest {

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
                fragments, partitions
        ) {
            listOf(
                    it.verify(True EU ReachModel.Prop.LOWER_CORNER),
                    it.verify(True EU ReachModel.Prop.UPPER_CORNER),
                    it.verify(True EU ReachModel.Prop.BORDER),
                    it.verify(False EU ReachModel.Prop.BORDER),
                    it.verify(ReachModel.Prop.BORDER EU ReachModel.Prop.UPPER_CORNER)
            )
        }.fold(nodesOf().repeat(5)) { l, r -> l.zip(r).map { it.first union it.second } }

        assertEquals(model.validNodes(ReachModel.Prop.LOWER_CORNER), result[0])
        assertEquals(nodesOf(
                model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
        ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[1])
        assertEquals(nodesOf(
                model.allNodes().entries.map { Pair(it.key, model.stateColors(it.key)) }
        ) + model.validNodes(ReachModel.Prop.BORDER), result[2])
        assertEquals(model.validNodes(ReachModel.Prop.BORDER), result[3])

        assertEquals(nodesOf(
                model.validNodes(ReachModel.Prop.BORDER).entries.map { Pair(it.key, model.stateColors(it.key)) }
        ) + model.validNodes(ReachModel.Prop.UPPER_CORNER), result[4])

    }

    @Test(timeout = 1000)
    fun smallCube() = generalModel(2, 2)

    @Test
    fun mediumCube() = generalModel(4, 4)

    @Test   //this can actually be kind of long! (7-10s)
    fun largeCube() = generalModel(6, 6)

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