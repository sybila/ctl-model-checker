package cz.muni.fi.checker

import cz.muni.fi.ctl.*
import org.junit.Test
import kotlin.test.assertEquals

val p1 = FloatProposition("x", CompareOp.EQ, 3.0)
val p2 = FloatProposition("y", CompareOp.GT, 3.4)
val p3 = FloatProposition("z", CompareOp.LT, 1.4)

/**
 * This is a simple model with fixed number of states, no transitions and
 * propositions distributed using modular arithmetic, so that their
 * validity can be easily predicted (although it might require some nontrivial
 * control flow)
 */
class RegularKripkeFragment(
        private val bounds: IntRange
) : KripkeFragment<IDNode, IDColors> {

    override val successors: IDNode.() -> Nodes<IDNode, IDColors>
            get() = throw UnsupportedOperationException()
    override val predecessors: IDNode.() -> Nodes<IDNode, IDColors>
            get() = throw UnsupportedOperationException()

    override fun allNodes(): Nodes<IDNode, IDColors>
            = bounds.map { Pair(IDNode(it), IDColors(1,2,3,4)) }.toIDNodes()

    override fun validNodes(a: Atom): Nodes<IDNode, IDColors> = when (a) {
        p1 -> bounds.filter { it % 2 == 0 }.map {
            Pair(IDNode(it), IDColors(3, 4, if (it % 4 == 0) 1 else 2))
        }.toIDNodes()
        p2 -> bounds.filter { it % 3 == 0 }.map {
            Pair(IDNode(it), IDColors(2, 3, if (it % 9 == 0) 1 else 4))
        }.toIDNodes()
        p3 -> bounds.filter { it % 5 == 0 }.map {
            Pair(IDNode(it), IDColors(1, 4, if (it % 25 == 0) 3 else 2))
        }.toIDNodes()
        else -> emptyIDNodes
    }

}

class AndVerificationTest {

    @Test
    fun simpleTest() {

        val bounds = 0..2000

        val model = RegularKripkeFragment(bounds)

        withSingleModelChecker(model) { checker ->

            val result = checker.verify(p1 and p2)

            val expected = (bounds).filter { it % 2 == 0 && it % 3 == 0 }.map {
                var c = IDColors(3)
                if (it % 4 == 0 && it % 9 == 0) c += IDColors(1)
                if (it % 4 != 0) c += IDColors(2)
                if (it % 9 != 0) c += IDColors(4)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }

    }

    @Test
    fun nestedTest() {

        val bounds = 0..2000

        val model = RegularKripkeFragment(bounds)

        withSingleModelChecker(model) { checker ->

            val result = checker.verify(p1 and p2 and p3)

            val expected = (bounds).filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }.map {
                var c = IDColors()
                if (it % 4 == 0 && it % 9 == 0) c += IDColors(1)
                if (it % 4 != 0 && it % 25 != 0) c += IDColors(2)
                if (it % 25 == 0) c += IDColors(3)
                if (it % 9 != 0) c += IDColors(4)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }
    }

    @Test
    fun nestedConcurrentTest() {

        val models = listOf(0..1667, 1668..4232, 4232..8000).map { bounds ->
            RegularKripkeFragment(bounds)
        }

        val result = withModelCheckers(models) {
            it.verify(p1 and p2 and p3)
        }.fold(emptyIDNodes) { r, l -> r union l }

        val expected = (0..8000).filter { it % 2 == 0 && it % 3 == 0 && it % 5 == 0 }.map {
            var c = IDColors()
            if (it % 4 == 0 && it % 9 == 0) c += IDColors(1)
            if (it % 4 != 0 && it % 25 != 0) c += IDColors(2)
            if (it % 25 == 0) c += IDColors(3)
            if (it % 9 != 0) c += IDColors(4)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }

    @Test
    fun simpleConcurrentTest() {

        val models = listOf(0..1867, 1868..4000).map { bounds ->
            RegularKripkeFragment(bounds)
        }

        val result = withModelCheckers(models) {
            it.verify(p1 and p2)
        }.fold(emptyIDNodes) { r, l -> r union l }

        val expected = (0..4000).filter { it % 2 == 0 && it % 3 == 0 }.map {
            var c = IDColors(3)
            if (it % 4 == 0 && it % 9 == 0) c += IDColors(1)
            if (it % 4 != 0) c += IDColors(2)
            if (it % 9 != 0) c += IDColors(4)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }

}


class OrVerificationTest {

    @Test
    fun simpleTest() {

        val bounds = 0..2000

        withSingleModelChecker(RegularKripkeFragment(bounds)) { checker ->
            val result = checker.verify(p1 or p2)

            val expected = (bounds).filter { it % 2 == 0 || it % 3 == 0 }.map {
                var c = IDColors(3)
                if (it % 3 == 0 || it % 4 != 0) c += IDColors(2)
                if (it % 9 == 0 || it % 4 == 0) c += IDColors(1)
                if (it % 2 == 0 || it % 9 != 0) c += IDColors(4)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }

    }

    @Test
    fun nestedTest() {

        val bounds = 0..2000

        withSingleModelChecker(RegularKripkeFragment(bounds)) { checker ->
            val result = checker.verify(p1 or p2 or p3)

            val expected = (bounds).filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.map {
                var c = IDColors()
                if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += IDColors(1)
                if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += IDColors(2)
                if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += IDColors(3)
                if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += IDColors(4)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }

    }

    @Test
    fun simpleConcurrentTest() {

        val models = listOf(0..1896,1897..4000).map {
            RegularKripkeFragment(it)
        }
        val result = withModelCheckers(models) {
            it.verify(p1 or p2)
        }.foldRight(emptyIDNodes) { l, r -> l union r }

        val expected = (0..4000).filter { it % 2 == 0 || it % 3 == 0 }.map {
            var c = IDColors(3)
            if (it % 3 == 0 || it % 4 != 0) c += IDColors(2)
            if (it % 9 == 0 || it % 4 == 0) c += IDColors(1)
            if (it % 2 == 0 || it % 9 != 0) c += IDColors(4)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }


    @Test
    fun nestedConcurrentTest() {

        val models = listOf(0..1896,1897..3975, 3976..8000).map {
            RegularKripkeFragment(it)
        }

        val result = withModelCheckers(models) {
            it.verify(p1 or p2 or p3)
        }.foldRight(emptyIDNodes) { l, r -> l union r }


        val expected = (0..8000).filter { it % 2 == 0 || it % 3 == 0 || it % 5 == 0 }.map {
            var c = IDColors()
            if (it % 5 == 0 || it % 9 == 0 || it % 4 == 0) c += IDColors(1)
            if (it % 3 == 0 || (it % 4 != 0 && it % 2 == 0) || (it % 25 != 0 && it % 5 == 0)) c += IDColors(2)
            if (it % 2 == 0 || it % 3 == 0 || it % 25 == 0) c += IDColors(3)
            if (it % 2 == 0 || (it % 9 != 0 && it % 3 == 0) || it % 5 == 0) c += IDColors(4)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }

}

class NegationTest {

    @Test
    fun simpleTest() {

        val bounds = 0..2000

        val model = RegularKripkeFragment(bounds)

        withSingleModelChecker(model) { checker ->

            val result = checker.verify(not(p1))

            val expected = (bounds).map {
                var c = IDColors()
                if (it % 2 != 0 || it % 4 != 0) c += IDColors(1)
                if (it % 2 != 0 || it % 4 == 0) c += IDColors(2)
                if (it % 2 != 0) c += IDColors(3,4)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }
    }

    @Test
    fun nestedTest() {

        val bounds = 0..2000

        val model = RegularKripkeFragment(bounds)

        withSingleModelChecker(model) { checker ->
            val result = checker.verify(not(not(p1)))

            val expected = (bounds).filter { it % 2 == 0 }.map {
                var c = IDColors(3,4)
                if (it % 4 == 0) c += IDColors(1) else c+= IDColors(2)
                Pair(IDNode(it), c)
            }.toIDNodes()

            assertEquals(expected, result)
            assertEquals(model.validNodes(p1), result)
            assert(result.isNotEmpty())
        }


    }


    @Test
    fun simpleConcurrentTest() {

        val models = listOf(0..1896,1897..4000).map {
            RegularKripkeFragment(it)
        }
        val result = withModelCheckers(models) {
            it.verify(not(p1))
        }.foldRight(emptyIDNodes) { l, r -> l union r }


        val expected = (0..4000).map {
            var c = IDColors()
            if (it % 2 != 0 || it % 4 != 0) c += IDColors(1)
            if (it % 2 != 0 || it % 4 == 0) c += IDColors(2)
            if (it % 2 != 0) c += IDColors(3,4)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }


    @Test
    fun nestedConcurrentTest() {


        val models = listOf(0..1896,1897..3975, 3976..8000).map {
            RegularKripkeFragment(it)
        }
        val result = withModelCheckers(models) {
            it.verify(not(not(p1)))
        }.foldRight(emptyIDNodes) { l, r -> l union r }


        val expected = (0..8000).filter { it % 2 == 0 }.map {
            var c = IDColors(3,4)
            if (it % 4 == 0) c += IDColors(1) else c+= IDColors(2)
            Pair(IDNode(it), c)
        }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())

    }
}

class MixedBooleanTest() {

    @Test
    fun complexTest() {

        val bounds = 0..2000

        val model = RegularKripkeFragment(bounds)

        withSingleModelChecker(model) { checker ->
            //obfuscated (p1 || p2) && !p3
            val result = checker.verify((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2))

            val expected = (bounds).filter { (it % 2 == 0 || it % 3 == 0) }.map {
                var c = IDColors()
                if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += IDColors(1)
                if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += IDColors(2)
                if (it % 5 != 0 || it % 25 != 0) c += IDColors(3)
                if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += IDColors(4)
                Pair(IDNode(it), c)
            }.filter { it.second.isNotEmpty() }.toIDNodes()

            assertEquals(expected, result)
            assert(result.isNotEmpty())
        }

    }

    @Test
    fun complexConcurrentTest() {


        val models = listOf(0..1896,1897..3975, 3976..8000).map {
            RegularKripkeFragment(it)
        }
        val result = withModelCheckers(models) {
            it.verify((p1 or not(not(p2)) or p2) and not(p3) and (p1 or p2))
        }.foldRight(emptyIDNodes) { l, r -> l union r }

        val expected = (0..8000).filter { (it % 2 == 0 || it % 3 == 0) }.map {
            var c = IDColors()
            if ((it % 9 == 0 || it % 4 == 0) && it % 5 != 0) c += IDColors(1)
            if ((it % 4 != 0 || it % 3 == 0) && (it % 5 != 0 || it % 25 == 0)) c += IDColors(2)
            if (it % 5 != 0 || it % 25 != 0) c += IDColors(3)
            if ((it % 2 == 0 || it % 9 != 0) && it % 5 != 0) c += IDColors(4)
            Pair(IDNode(it), c)
        }.filter { it.second.isNotEmpty() }.toIDNodes()

        assertEquals(expected, result)
        assert(result.isNotEmpty())
    }

}