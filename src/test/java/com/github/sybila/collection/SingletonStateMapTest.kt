package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals

class SingletonStateMapTest {

    @Test
    fun readTest() {
        val s = SingletonStateMap("hello", 10)
        assertEquals(null, s["world"])
        assertEquals(10, s["hello"])
    }

    @Test
    fun statesSequence() {
        val s = SingletonStateMap("hello", 10)
        assertEquals(listOf("hello"), s.states.toList())
    }

    @Test
    fun entriesSequence() {
        val s = SingletonStateMap("hello", 10)
        assertEquals(listOf("hello" to 10), s.entries.toList())
    }

}