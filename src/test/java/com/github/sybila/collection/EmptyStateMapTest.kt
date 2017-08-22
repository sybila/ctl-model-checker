package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals

class EmptyStateMapTest {

    @Test
    fun read() {
        val e = EmptyStateMap<String, Int>()
        assertEquals(null, e["A"])
        assertEquals(null, e["foo"])
    }

    @Test
    fun statesSequence() {
        val e = EmptyStateMap<String, Int>()
        assertEquals(emptySequence(), e.states)
    }

    @Test
    fun entriesSequence() {
        val e = EmptyStateMap<String, Int>()
        assertEquals(emptySequence(), e.entries)
    }

}