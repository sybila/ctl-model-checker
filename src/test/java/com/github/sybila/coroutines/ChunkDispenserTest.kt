package com.github.sybila.coroutines

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkDispenserTest {

    @Test
    fun executionTest() {
        val chunk = ChunkDispenser(meanChunkTime = 100)
        val c1 = chunk.next()
        chunk.adjust(c1, 0) // way too fast
        val c2 = chunk.next()
        chunk.adjust(c2, 0) // way too fast
        val c3 = chunk.next()
        chunk.adjust(c3, 50) // still too fast
        val c4 = chunk.next()
        chunk.adjust(c4, 150) // too slow
        val c5 = chunk.next()
        chunk.adjust(c5, 2000) // way too slow
        val c6 = chunk.next()
        chunk.adjust(c6, 110) // ok

        assertEquals(1, c1)
        assertTrue(c2 > c1)
        assertTrue(c3 > c2)
        assertTrue(c4 > c3)
        assertTrue(c5 < c4)
        assertTrue(c6 < c5)
        assertEquals(1, c6)
    }

}