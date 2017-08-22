package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals

class GenericStateMapTest {

    @Test
    fun inBoundsRead() {
        val map = GenericStateMap(mapOf("hello" to 3.14, "world" to -2.2))
        assertEquals(3.14, map["hello"])
        assertEquals(-2.2, map["world"])
    }

    @Test
    fun outOfBoundsRead() {
        val map = GenericStateMap(mapOf("hello" to 3.14, "world" to -2.2))
        assertEquals(null, map["hello-world"])
    }

    @Test
    fun statesSequence() {
        val map = GenericStateMap(mapOf("hello" to 3.14, "world" to -2.2))
        assertEquals(listOf("hello", "world"), map.states.toList())
    }

    @Test
    fun entriesSequence() {
        val map = GenericStateMap(mapOf("hello" to 3.14, "world" to -2.2))
        assertEquals(mapOf("hello" to 3.14, "world" to -2.2), map.entries.toMap())
    }

}