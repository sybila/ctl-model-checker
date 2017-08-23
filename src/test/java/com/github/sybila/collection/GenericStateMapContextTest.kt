package com.github.sybila.collection

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericStateMapContextTest {

    @Test
    fun atomsTest() {
        val ctx = GenericStateMapContext(mapOf("a" to 10, "b" to 20))
        assertTrue(ctx.emptyMap.states.toList().isEmpty())
        assertEquals(mapOf("a" to 10, "b" to 20), ctx.fullMap.entries.toMap())
    }

    @Test
    fun genericToMutableTest() {
        val ctx = GenericStateMapContext(mapOf("a" to 10, "b" to 20))
        val m = mapOf("a" to 10)
        val map = GenericStateMap(m)
        val mut = ctx.run { map.toMutable() }
        assertEquals(m, mut.entries.toMap())
        mut.lazySet("b", 20)
    }

    @Test
    fun arbitraryToMutableTest() {
        val ctx = GenericStateMapContext(mapOf("a" to 10, "b" to 20))
        val m = mapOf("a" to 10)
        val map = SingletonStateMap("a", 10)
        val mut = ctx.run { map.toMutable() }
        assertEquals(m, mut.entries.toMap())
        mut.lazySet("b", 20)
    }

    @Test
    fun genericToReadOnly() {
        val ctx = GenericStateMapContext(mapOf("a" to 10, "b" to 20))
        val m = mapOf("a" to 10)
        val map = GenericMutableStateMap<String, Int>(emptyMap())
        map.lazySet("a", 10)
        val read = ctx.run { map.toReadOnly() }
        assertEquals(m, read.entries.toMap())
    }

    @Test
    fun arbitraryToReadOnly() {
        val ctx = GenericStateMapContext(mapOf(1 to 10, 2 to 20))
        val m = mapOf(1 to 10)
        val map = AtomicArrayStateMap<Int>(3)
        map.lazySet(1, 10)
        val read = ctx.run { map.toReadOnly() }
        assertEquals(m, read.entries.toMap())
    }

}