package cz.muni.fi.checker

import cz.muni.fi.ctl.EX
import org.junit.Test
import kotlin.test.assertEquals

class SequentialExistsNextTest {

    @Test
    fun oneStateModel() {

        val model = ReachModel(1, 1)

        withSingleModelChecker(model) {
            val expected = nodesOf(Pair(IDNode(0), IDColors(1)))
            assertEquals(expected, it.verify(EX(ReachModel.Prop.UPPER_CORNER)))
            assertEquals(expected, it.verify(EX(ReachModel.Prop.LOWER_CORNER)))
            assertEquals(expected, it.verify(EX(ReachModel.Prop.CENTER)))
            assertEquals(expected, it.verify(EX(ReachModel.Prop.BORDER)))
        }

    }

    fun chainModel(chainSize: Int) {

        val model = ReachModel(1, chainSize)

        withSingleModelChecker(model) {
            assertEquals(nodesOf(Pair(IDNode(0), model.parameters - IDColors(0))), it.verify(EX(ReachModel.Prop.LOWER_CORNER)))   //just self loop
            assertEquals(nodesOf(
                    Pair(IDNode(chainSize-1), IDColors(chainSize)),
                    Pair(IDNode(chainSize-2), IDColors((0 until (chainSize-1)).toSet()))
            ), it.verify(EX(ReachModel.Prop.UPPER_CORNER)))
            assertEquals(
                    model.validNodes(ReachModel.Prop.BORDER).entries
                            .map { model.predecessors.invoke(it.key) }
                            .fold(emptyIDNodes) { l, r -> l + r }
            , it.verify(EX(ReachModel.Prop.BORDER)))
        }

    }

    fun generalModel(dimensions: Int, dimensionSize: Int) {

        val model = ReachModel(dimensions, dimensionSize)

        withSingleModelChecker(model) {
            assertEquals(nodesOf(Pair(IDNode(0), model.parameters - IDColors(0))), it.verify(EX(ReachModel.Prop.LOWER_CORNER)))
            val upperCorner = it.verify(ReachModel.Prop.UPPER_CORNER).entries.first()
            assertEquals(nodesOf(
                    (0..dimensions).map { IDNode(upperCorner.key.id - pow(dimensionSize, it)) }
                            .filter { it.id >= 0 }.map { Pair(it, model.stateColors(it)) }
                    + Pair(upperCorner.key, IDColors((dimensionSize-1) * dimensions + 1))
            ), it.verify(EX(ReachModel.Prop.UPPER_CORNER)))
            assertEquals(
                    //very slow, but works!
                    model.validNodes(ReachModel.Prop.BORDER).entries
                            .map { model.predecessors.invoke(it.key) }
                            .fold(emptyIDNodes) { l, r -> l + r }
            , it.verify(EX(ReachModel.Prop.BORDER)))
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
                    it.verify(EX(ReachModel.Prop.LOWER_CORNER)),
                    it.verify(EX(ReachModel.Prop.UPPER_CORNER)),
                    it.verify(EX(ReachModel.Prop.BORDER))
            )
        }.fold(nodesOf().repeat(3)) { l, r -> l.zip(r).map { it.first union it.second } }

        assertEquals(nodesOf(Pair(IDNode(0), model.parameters - IDColors(0))), result[0])

        val upperCorner = model.validNodes(ReachModel.Prop.UPPER_CORNER).entries.first()
        assertEquals(nodesOf(
                (0..dimensions).map { IDNode(upperCorner.key.id - pow(dimensionSize, it)) }
                        .filter { it.id >= 0 }.map { Pair(it, model.stateColors(it)) }
                        + Pair(upperCorner.key, IDColors((dimensionSize-1) * dimensions + 1))
        ), result[1])

        assertEquals(
                //very slow, but works!
                model.validNodes(ReachModel.Prop.BORDER).entries
                        .map { model.predecessors.invoke(it.key) }
                        .fold(emptyIDNodes) { l, r -> l + r }
        , result[2])

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