package com.github.sybila.checker
/*
import com.github.sybila.ctl.CompareOp
import com.github.sybila.ctl.FloatProposition
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

fun nodesOf(vararg pairs: Pair<IDNode, IDColors>): Nodes<IDNode, IDColors>
        = pairs.associateBy({ it.first }, { it.second }).toNodes(IDColors())

fun nodesOf(pairs: List<Pair<IDNode, IDColors>>): Nodes<IDNode, IDColors>
        = pairs.associateBy({ it.first }, { it.second }).toNodes(IDColors())

class ExplicitKripkeFragmentTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n3 = IDNode(2)
    val n4 = IDNode(3)
    val n5 = IDNode(4)

    @Test fun invalidStructureTest() {

        assertFailsWith(IllegalArgumentException::class) { //Extra edge
            ExplicitKripkeFragment(mapOf(), setOf(Edge(n1, n1, IDColors(1))), mapOf())
        }

        assertFailsWith(IllegalArgumentException::class) { //Extra atom
            ExplicitKripkeFragment(mapOf(), setOf(),
                    mapOf(Pair(FloatProposition("foo", CompareOp.EQ, 1.0), mapOf(Pair(n1, IDColors(2)))))
            )
        }

        assertFailsWith(IllegalArgumentException::class) { //Extra atom param
            ExplicitKripkeFragment(mapOf(Pair(n1, IDColors(1))), setOf(),
                    mapOf(Pair(FloatProposition("foo", CompareOp.EQ, 1.0), mapOf(Pair(n1, IDColors(2)))))
            )
        }

        assertFailsWith(IllegalArgumentException::class) { //Missing successor
            ExplicitKripkeFragment(mapOf(Pair(n1, IDColors(1))), setOf(), mapOf())
        }

    }

    @Test fun edgeTestWithBorders() {

        val ks = ExplicitKripkeFragment(mapOf(
                Pair(n1, IDColors(1, 2, 3)),
                Pair(n2, IDColors(1, 2, 3)),
                Pair(n3, IDColors(3, 4, 5)),
                Pair(n4, IDColors(3, 4, 5))
        ), setOf(
                Edge(n1, n2, IDColors(2, 3)),
                Edge(n1, n5, IDColors(1)),
                Edge(n2, n1, IDColors(1)),
                Edge(n2, n2, IDColors(2)),
                Edge(n2, n3, IDColors(3)),
                Edge(n3, n4, IDColors(3, 4)),
                Edge(n3, n5, IDColors(5)),
                Edge(n4, n1, IDColors(3)),
                Edge(n4, n3, IDColors(5)),
                Edge(n4, n4, IDColors(4))
        ), mapOf())

        assertEquals(nodesOf(
                Pair(n2, IDColors(2, 3)),
                Pair(n5, IDColors(1))
        ), n1.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(1)),
                Pair(n2, IDColors(2)),
                Pair(n3, IDColors(3))
        ), n2.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n4, IDColors(3, 4)),
                Pair(n5, IDColors(5))
        ), n3.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(3)),
                Pair(n3, IDColors(5)),
                Pair(n4, IDColors(4))
        ), n4.run(ks.successors))

        assertEquals(nodesOf(), n5.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n2, IDColors(1)),
                Pair(n4, IDColors(3))
        ), n1.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(2, 3)),
                Pair(n2, IDColors(2))
        ), n2.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n2, IDColors(3)),
                Pair(n4, IDColors(5))
        ), n3.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n3, IDColors(3, 4)),
                Pair(n4, IDColors(4))
        ), n4.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(1)),
                Pair(n3, IDColors(5))
        ), n5.run(ks.predecessors))
    }

    @Test fun edgeTestNoBorders() {

        val fullColors = IDColors(1, 2, 3, 4, 5)
        val nodes = setOf(n1, n2, n3, n4, n5).associateBy({ it }, { fullColors })

        val ks = ExplicitKripkeFragment(
                nodes, nodes.map { Edge(it.key, it.key, it.value) }.toSet() + setOf(
                Edge(n1, n2, IDColors(2)),
                Edge(n1, n4, IDColors(2, 4)),
                Edge(n1, n3, IDColors(2)),
                Edge(n2, n5, IDColors(3, 4)),
                Edge(n3, n1, IDColors(1, 2)),
                Edge(n4, n3, IDColors(1, 2, 3, 4, 5)),
                Edge(n3, n5, IDColors(2, 3))
        ), mapOf())

        assertEquals(nodesOf(
                Pair(n1, fullColors),
                Pair(n2, IDColors(2)),
                Pair(n4, IDColors(2, 4)),
                Pair(n3, IDColors(2))
        ), n1.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n2, fullColors),
                Pair(n5, IDColors(3, 4))
        ), n2.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n3, fullColors),
                Pair(n1, IDColors(1, 2)),
                Pair(n5, IDColors(2, 3))
        ), n3.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n4, fullColors),
                Pair(n3, fullColors)
        ), n4.run(ks.successors))

        assertEquals(nodesOf(Pair(n5, fullColors)), n5.run(ks.successors))

        assertEquals(nodesOf(
                Pair(n1, fullColors),
                Pair(n3, IDColors(1, 2))
        ), n1.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(2)),
                Pair(n2, fullColors)
        ), n2.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(2)),
                Pair(n3, fullColors),
                Pair(n4, fullColors)
        ), n3.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n1, IDColors(2, 4)),
                Pair(n4, fullColors)
        ), n4.run(ks.predecessors))

        assertEquals(nodesOf(
                Pair(n2, IDColors(3, 4)),
                Pair(n3, IDColors(2, 3)),
                Pair(n5, fullColors)
        ), n5.run(ks.predecessors))

    }

    @Test fun allNodesTest() {
        val nodes = setOf(n1, n2, n4).associateBy({ it }, { IDColors(it.id) })
        val ks = ExplicitKripkeFragment(nodes, nodes.map { Edge(it.key, it.key, it.value) }.toSet(), mapOf())
        assertEquals(nodesOf(IDColors(), Pair(n1, IDColors(0)), Pair(n2, IDColors(1)), Pair(n4, IDColors(3))), ks.allNodes())
    }

    @Test fun validNodesTest() {
        val ks = ExplicitKripkeFragment(
                setOf(n1, n3, n5).associateBy({ it }, { IDColors(1, 2, 3) })
                , setOf(n1, n3, n5).map { Edge(it, it, IDColors(1, 2, 3)) }.toSet(), mapOf(
                Pair(
                        FloatProposition("name", CompareOp.EQ, 3.14),
                        listOf(n1).map { Pair(it, IDColors(1, 2)) }.toMap()
                ),
                Pair(
                        FloatProposition("other", CompareOp.GT_EQ, 2.2),
                        listOf(n1, n5).map { Pair(it, IDColors(2, 3)) }.toMap()
                )
        ))

        assertEquals(nodesOf(listOf(n1).map { Pair(it, IDColors(1, 2)) }), ks.validNodes(FloatProposition("name", CompareOp.EQ, 3.14)))
        assertEquals(nodesOf(listOf(n1, n5).map { Pair(it, IDColors(2, 3)) }), ks.validNodes(FloatProposition("other", CompareOp.GT_EQ, 2.2)))
    }


}

class ExplicitPartitionFunctionTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n3 = IDNode(2)
    val n4 = IDNode(3)
    val n5 = IDNode(4)

    @Test fun directMappingTest() {

        val function = ExplicitPartitionFunction(
                0,
                directMapping = mapOf(
                        Pair(n1, 0),
                        Pair(n2, 1),
                        Pair(n3, 0),
                        Pair(n4, 2),
                        Pair(n5, 1)
                )
        )

        assertEquals(0, n1.run(function.ownerId))
        assertEquals(0, n3.run(function.ownerId))
        assertEquals(1, n2.run(function.ownerId))
        assertEquals(1, n5.run(function.ownerId))
        assertEquals(2, n4.run(function.ownerId))

        assertEquals(0, function.myId)

    }

    @Test fun inverseMappingTest() {

        val function = ExplicitPartitionFunction(
                0,
                inverseMapping = mapOf(
                        Pair(0, listOf(n1, n3)),
                        Pair(1, listOf(n2, n4)),
                        Pair(2, listOf(n5))
                )
        )

        assertEquals(0, n1.run(function.ownerId))
        assertEquals(0, n3.run(function.ownerId))
        assertEquals(1, n2.run(function.ownerId))
        assertEquals(1, n4.run(function.ownerId))
        assertEquals(2, n5.run(function.ownerId))

        assertEquals(0, function.myId)
    }

    @Test fun invalidMappingTest() {

        assertFailsWith(IllegalArgumentException::class) {
            ExplicitPartitionFunction(
                    0,
                    inverseMapping = mapOf(
                            Pair(0, listOf(n1, n3)),
                            Pair(1, listOf(n2, n4)),
                            Pair(2, listOf(n5, n1))
                    )
            )
        }

        val function = ExplicitPartitionFunction(0, mapOf())
        assertFailsWith(NullPointerException::class) {
            n1.run(function.ownerId)
        }
    }
}

class UniformPartitionFunctionTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n5 = IDNode(4)

    @Test fun myIdTest() {
        val f1 = UniformPartitionFunction<IDNode>()
        assertEquals(0, f1.myId)
        val f2 = UniformPartitionFunction<IDNode>(10)
        assertEquals(10, f2.myId)
    }

    @Test fun mappingTest() {
        val f = UniformPartitionFunction<IDNode>(2)
        assertEquals(2, n1.run(f.ownerId))
        assertEquals(2, n2.run(f.ownerId))
        assertEquals(2, n5.run(f.ownerId))
    }

}

class FunctionPartitionFunctionTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n5 = IDNode(4)

    @Test fun mappingTest() {
        var runs = 0
        val f = FunctionalPartitionFunction<IDNode>(4) { runs += 1; it.id * 2 }

        assertEquals(0, n1.run(f.ownerId))
        assertEquals(2, n2.run(f.ownerId))
        assertEquals(8, n5.run(f.ownerId))

        assertEquals(4, f.myId)
        assertEquals(3, runs)

    }

}

class IDColorsTest {

    @Test fun intersectTest() {
        assertEquals(IDColors(), IDColors(1, 2, 3) intersect IDColors())
        assertEquals(IDColors(), IDColors() intersect IDColors(1, 2, 3))
        assertEquals(IDColors(2), IDColors(1, 2) intersect IDColors(2, 3))
    }

    @Test fun plusTest() {
        assertEquals(IDColors(1, 2, 3), IDColors(1, 2, 3) + IDColors())
        assertEquals(IDColors(1, 2, 3), IDColors() + IDColors(1, 2, 3))
        assertEquals(IDColors(), IDColors() + IDColors())
        assertEquals(IDColors(1, 2), IDColors(1, 2) + IDColors(1, 2))
        assertEquals(IDColors(1, 2, 3), IDColors(1, 2) + IDColors(2, 3))
        assertEquals(IDColors(2, 3, 5, 6), IDColors(2, 5) + IDColors(3, 6))
        assertEquals(IDColors(2, 3, 5, 6), IDColors(2, 5) union IDColors(3, 6))
    }

    @Test fun minusTest() {
        assertEquals(IDColors(1, 2, 3), IDColors(1, 2, 3) - IDColors())
        assertEquals(IDColors(), IDColors() - IDColors(1, 2, 3))
        assertEquals(IDColors(), IDColors(1, 2, 3) - IDColors(1, 2, 3))
        assertEquals(IDColors(1, 2), IDColors(1, 2, 3) - IDColors(3, 4))
        assertEquals(IDColors(1, 2), IDColors(1, 2, 3) subtract IDColors(3, 4))
    }

    @Test fun isEmptyTest() {
        assertTrue { IDColors().isEmpty() }
        assertTrue { IDColors(1).isNotEmpty() }
    }

}*/