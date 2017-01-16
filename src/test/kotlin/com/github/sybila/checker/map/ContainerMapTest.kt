package com.github.sybila.checker.map

import com.github.sybila.checker.Params
import com.github.sybila.checker.TT
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArrayTest {

    val map = Array<Params?>(10) { if (it % 2 == 0) null else TT }.asStateMap()

    @Test
    fun statesTest() {
        assertEquals((0..9).filter { it % 2 == 1 }, map.states.toList())
    }

    @Test
    fun entriesTest() {
        assertEquals((0..9).filter { it % 2 == 1 }.map { it to TT }, map.entries.toList())
    }

    @Test
    fun containsTest() {
        assertTrue((0..9).filter { it % 2 == 1 }.all { map.contains(it) })
        assertTrue((0..9).filter { it % 2 == 0 }.none { map.contains(it) })
        assertTrue((10..20).none { map.contains(it) })
        assertTrue((-5..-2).none { map.contains(it) })
    }

    @Test
    fun getTest() {
        assertTrue((0..9).filter { it % 2 == 1 }.all { map[it] == TT })
        assertTrue((0..9).filter { it % 2 == 0 }.all { map[it] == null })
        assertTrue((10..20).all { map[it] == null })
        assertTrue((-5..-2).all { map[it] == null })
    }

}

class MapTest {

    val map = (0..9).filter { it % 2 == 1 }.map { it to TT }.toMap().asStateMap()

    @Test
    fun statesTest() {
        assertEquals((0..9).filter { it % 2 == 1 }, map.states.toList())
    }

    @Test
    fun entriesTest() {
        assertEquals((0..9).filter { it % 2 == 1 }.map { it to TT }, map.entries.toList())
    }

    @Test
    fun containsTest() {
        assertTrue((0..9).filter { it % 2 == 1 }.all { map.contains(it) })
        assertTrue((0..9).filter { it % 2 == 0 }.none { map.contains(it) })
        assertTrue((10..20).none { map.contains(it) })
        assertTrue((-5..-2).none { map.contains(it) })
    }

    @Test
    fun getTest() {
        assertTrue((0..9).filter { it % 2 == 1 }.all { map[it] == TT })
        assertTrue((0..9).filter { it % 2 == 0 }.all { map[it] == null })
        assertTrue((10..20).all { map[it] == null })
        assertTrue((-5..-2).all { map[it] == null })
    }

}