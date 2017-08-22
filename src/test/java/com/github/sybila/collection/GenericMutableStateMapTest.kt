package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenericMutableStateMapTest {


    @Test
    fun inBoundsRead() {
        val states = GenericMutableStateMap(mapOf(0 to "a"))
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun outOfBoundsRead() {
        val states = GenericMutableStateMap(mapOf(0 to "a"))
        assertEquals(null, states[-1])
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun lazyWrite() {
        val states = GenericMutableStateMap<Int, String>(mapOf())
        states.lazySet(3, "hello")
        states.lazySet(8, "world")
        assertEquals("hello", states[3])
        assertEquals("world", states[8])
        states.lazySet(3, null)
        assertEquals(null, states[3])
    }

    @Test
    fun atomicWrite() {
        val states = GenericMutableStateMap<Int, String>(mapOf())
        assertTrue(states.compareAndSet(0, null, null))
        assertTrue(states.compareAndSet(0, null, "hello"))
        assertEquals("hello", states[0])
        assertFalse(states.compareAndSet(0, "not hello", "world"))
        assertTrue(states.compareAndSet(0, "hello", "world"))
        assertEquals("world", states[0])
        assertTrue(states.compareAndSet(0, "world", null))
        assertEquals(null, states[0])
    }

    @Test
    fun statesSequence() {
        val states = GenericMutableStateMap((0..8).step(2).map { it to "a" }.toMap())
        assertEquals(listOf(0, 2, 4, 6, 8), states.states.toList())
    }

    @Test
    fun entriesSequence() {
        val states = GenericMutableStateMap((0..8).step(2).map { it to "a" }.toMap())
        assertEquals(listOf(0, 2, 4, 6, 8).map { it to "a" }, states.entries.toList())
    }

}