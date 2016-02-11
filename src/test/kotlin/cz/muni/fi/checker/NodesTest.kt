package cz.muni.fi.checker

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val n0 = IDNode(0)
val n1 = IDNode(1)
val n2 = IDNode(2)
val n3 = IDNode(3)
val n4 = IDNode(4)

val cEmpty = IDColors()
val c0 = IDColors(0)
val c1 = IDColors(1)
val c2 = IDColors(2)
val c3 = IDColors(3)
val c4 = IDColors(4)

class MutableMapNodesTest {

    @Test fun putOrUnionTest() {
        val s1 = MutableMapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c3), Pair(n2, c1 + c2)
        ))
        assertFalse(s1.putOrUnion(n0, c0))
        assertFalse(s1.putOrUnion(n0, c3))
        assertTrue(s1.putOrUnion(n0, c2))
        assertTrue(s1.putOrUnion(n0, c1))
        assertFalse(s1.putOrUnion(n0, c1 + c2 + c3))
        assertFalse(s1.putOrUnion(n2, c1))
        assertFalse(s1.putOrUnion(n2, c2))
        assertTrue(s1.putOrUnion(n2, c3))
        assertTrue(s1.putOrUnion(n2, c0))
        assertTrue(s1.putOrUnion(n1, c0))
        assertTrue(s1.putOrUnion(n1, c1))
        assertTrue(s1.putOrUnion(n1, c2))

        assertEquals(MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c1 + c2 + c3),
                Pair(n1, c0 + c1 + c2),
                Pair(n2, c0 + c1 + c2 + c3)
        )), s1.toNodes())

    }
}

class MapNodesTest {

    @Test fun isEmptyTest() {
        val s1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c3)
        ))
        assertTrue { s1.isNotEmpty() }
        val s2 = MapNodes(cEmpty, mapOf())
        assertTrue { s2.isEmpty() }
    }

    @Test fun containsTest() {
        val s1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2)
        ))
        assertTrue { s1.contains(n0) }
        assertFalse { s1.contains(n1) }
    }

    @Test fun getTest() {
        val s = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2)
        ))
        assertEquals(cEmpty, s[n2])
        assertEquals(cEmpty, s[n3])
        assertEquals(c1 + c2, s[n0])
    }

    @Test fun validKeysEntriesTest() {
        val s = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2),
                Pair(n3, c3)
        ))
        assertEquals(setOf(
                AbstractMap.SimpleEntry(n0, c1 + c2),
                AbstractMap.SimpleEntry(n3, c3)
        ), s.entries)
    }

    @Test fun intersectTest() {
        val s1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2 + c3),
                Pair(n1, c2 + c4),
                Pair(n2, c3 + c4)
        ))

        val s2 = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c1 + c4),
                Pair(n1, c2 + c4),
                Pair(n3, c0 + c3)
        ))

        val sE = MapNodes(cEmpty, mapOf<IDNode, IDColors>())

        assertEquals(sE, sE intersect sE)
        assertEquals(sE, s1 intersect sE)
        assertEquals(sE, s2 intersect sE)
        assertEquals(sE, sE intersect s1)
        assertEquals(sE, sE intersect s2)

        val r = MapNodes(cEmpty, mapOf(
                Pair(n0, c1),
                Pair(n1, c2 + c4)
        ))

        assertEquals(r, s1 intersect s2)
        assertEquals(r, s2 intersect s1)
    }

    @Test fun plusTest() {
        val s1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2 + c3),
                Pair(n1, c2 + c4),
                Pair(n2, c3 + c4)
        ))

        val s2 = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c1 + c4),
                Pair(n1, c2 + c4),
                Pair(n4, c0 + c3)
        ))

        val sE = MapNodes(cEmpty, mapOf<IDNode, IDColors>())

        assertEquals(sE, sE + sE)
        assertEquals(s1, s1 + sE)
        assertEquals(s2, s2 + sE)
        assertEquals(s1, sE + s1)
        assertEquals(s2, sE + s2)

        val r = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c1 + c2 + c3 + c4),
                Pair(n1, c2 + c4),
                Pair(n2, c3 + c4),
                Pair(n4, c0 + c3)
        ))

        assertEquals(r, s1 + s2)
        assertEquals(r, s2 + s1)
    }

    @Test fun minusTest() {
        val s1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c1 + c2 + c3),
                Pair(n1, c2 + c4),
                Pair(n2, c3 + c4)
        ))

        val s2 = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c1 + c4),
                Pair(n1, c2 + c4),
                Pair(n3, c0 + c3)
        ))

        val sE = MapNodes(cEmpty, mapOf<IDNode, IDColors>())

        assertEquals(sE, sE - sE)
        assertEquals(s1, s1 - sE)
        assertEquals(sE, sE - s1)
        assertEquals(s2, s2 - sE)
        assertEquals(sE, sE - s2)

        val r1 = MapNodes(cEmpty, mapOf(
                Pair(n0, c2 + c3),
                Pair(n2, c3 + c4)
        ))

        val r2 = MapNodes(cEmpty, mapOf(
                Pair(n0, c0 + c4),
                Pair(n3, c0 + c3)
        ))

        assertEquals(r1, s1 - s2)
        assertEquals(r2, s2 - s1)
    }

}