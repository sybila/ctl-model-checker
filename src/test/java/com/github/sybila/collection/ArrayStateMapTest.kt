package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals

class ArrayStateMapTest {

    @Test
    fun inBoundsRead() {
        val states = ArrayStateMap(2) { if (it == 0) "a" else null }
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun outOfBoundsRead() {
        val states = ArrayStateMap(1) { "a" }
        assertEquals(null, states[-1])
        assertEquals("a", states[0])
        assertEquals(null, states[1])
    }

    @Test
    fun statesSequence() {
        val states = ArrayStateMap(10) { if (it % 2 == 0) "a" else null }
        assertEquals(listOf(0, 2, 4, 6, 8), states.states.toList())
    }

    @Test
    fun entriesSequence() {
        val states = ArrayStateMap(10) { if (it % 2 == 0) "a" else null }
        assertEquals(listOf(0, 2, 4, 6, 8).map { it to "a" }, states.entries.toList())
    }

    @Test
    fun makeAtomic() {
        val states = ArrayStateMap(3) { if (it == 1) null else (it * 2).toString() }
        val atomic = states.makeMutable()
        assertEquals(states[0], atomic[0])
        assertEquals(states[1], atomic[1])
        assertEquals(states[2], atomic[2])
    }

}