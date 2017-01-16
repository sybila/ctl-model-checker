package com.github.sybila.checker.map

import com.github.sybila.checker.And
import com.github.sybila.checker.Or
import com.github.sybila.checker.TT
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LazyAndTest {

    val left = (0..9).filter { it % 2 == 0 }.map { it to TT }.toMap().asStateMap()
    val right = (0..9).filter { it % 3 == 0 }.map { it to TT }.toMap().asStateMap()
    val and = left lazyAnd right

    @Test
    fun statesTest() {
        assertEquals(listOf(0, 6), and.states.toList().sorted())
    }

    @Test
    fun entriesTest() {
        assertEquals(listOf(0, 6).map { it to And(TT, TT) }, and.entries.toList().sortedBy { it.first })
    }

    @Test
    fun containsTest() {
        assertTrue(0 in and)
        assertTrue(6 in and)
        assertTrue((-10..20).filter { it != 0 && it != 6 }.all { it !in and })
    }

    @Test
    fun getTest() {
        assertEquals(And(TT, TT), and[0])
        assertEquals(And(TT, TT), and[6])
        assertTrue((-10..20).filter { it != 0 && it != 6 }.all { and[it] == null })
    }

}


class LazyOrTest {

    val left = (0..9).filter { it % 2 == 0 }.map { it to TT }.toMap().asStateMap()
    val right = (0..9).filter { it % 3 == 0 }.map { it to TT }.toMap().asStateMap()
    val or = left lazyOr right

    @Test
    fun statesTest() {
        assertEquals(listOf(0, 2, 3, 4, 6, 8, 9), or.states.toList().sorted())
    }

    @Test
    fun entriesTest() {
        assertEquals(listOf(0, 2, 3, 4, 6, 8, 9).map {
            if (it % 2 == 0 && it % 3 == 0) {
                it to Or(TT, TT)
            } else it to TT
        }, or.entries.toList().sortedBy { it.first })
    }

    @Test
    fun containsTest() {
        assertTrue(or.states.all { it in or })
        assertTrue((-10..20).filter { it !in or.states }.all { it !in or })
    }

    @Test
    fun getTest() {
        assertTrue(or.states.all {
            if (it % 2 == 0 && it % 3 == 0) {
                or[it] == Or(TT, TT)
            } else {
                or[it] == TT
            }
        })
        assertTrue((-10..20).filter { it !in or.states }.all { or[it] == null })
    }

}