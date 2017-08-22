package com.github.sybila.collection

import org.junit.Test
import kotlin.coroutines.experimental.buildSequence
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicArrayStateMapTest {

    @Test
    fun inBoundsRead() {
        val states = ArrayStateMap(2) { if (it == 0) "a" else null }.toAtomic()
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun outOfBoundsRead() {
        val states = ArrayStateMap(1) { "a" }.toAtomic()
        assertEquals(null, states[-1])
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun inBoundsWrite() {
        val states = AtomicArrayStateMap<String>(10)
        states.lazySet(3, "hello")
        states.lazySet(8, "world")
        assertEquals("hello", states[3])
        assertEquals("world", states[8])
        states.lazySet(3, null)
        assertEquals(null, states[3])
    }

    @Test
    fun outOfBoundsWrite() {
        val states = AtomicArrayStateMap<String>(2)
        assertFailsWith<IndexOutOfBoundsException> { states.lazySet(-1, "hello") }
        assertFailsWith<IndexOutOfBoundsException> { states.lazySet(4, "world") }
    }

    @Test
    fun inBoundsAtomicWrite() {
        val states = AtomicArrayStateMap<String>(1)
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
    fun outOfBoundsAtomicWrite() {
        val states = AtomicArrayStateMap<String>(1)
        assertFailsWith<IndexOutOfBoundsException> { states.compareAndSet(-1, null, "hello") }
        assertFailsWith<IndexOutOfBoundsException> { states.compareAndSet(3, null, "world") }
    }

    @Test
    fun statesSequence() {
        val states = ArrayStateMap(10) { if (it % 2 == 0) "a" else null }.toAtomic()
        assertEquals(listOf(0, 2, 4, 6, 8), states.states.toList())
    }

    @Test
    fun entriesSequence() {
        val states = ArrayStateMap(10) { if (it % 2 == 0) "a" else null }.toAtomic()
        assertEquals(listOf(0, 2, 4, 6, 8).map { it to "a" }, states.entries.toList())
    }


}