package com.github.sybila.algorithm

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TierStateQueueTest {

    @Test
    fun simpleTest() {
        val queue = TierStateQueue(10)

        assertTrue(queue.isEmpty())
        assertFalse(queue.isNotEmpty())

        queue.add(0, null)

        assertFalse(queue.isEmpty())
        assertTrue(queue.isNotEmpty())

        queue.add(1, null)

        assertEquals(listOf(0, 1), queue.remove().toList())

        queue.add(2, 0)
        queue.add(3, 2)
        queue.add(4, 3)

        // decrease distance of 4 while it is in the queue
        queue.add(4, 1)
        queue.add(5, 4)

        assertEquals(listOf(2, 4), queue.remove().toList())
        assertEquals(listOf(3, 5), queue.remove().toList())

        // decrease distance of 5 it is not in the queue
        queue.add(5, 1)
        queue.add(6, 3)

        assertEquals(listOf(5), queue.remove().toList())
        assertEquals(listOf(6), queue.remove().toList())

    }

    @Test
    fun brokenStrategyTest() {

        val queue = TierStateQueue(10)

        queue.add(0, null)
        queue.add(1, null)

        queue.add(2, 0)

        assertFailsWith<IllegalStateException> {
            queue.add(4, 3)
        }

    }

    @Test
    fun removeFromEmptyQueueTest() {

        val queue = TierStateQueue(10)

        assertFailsWith<IllegalStateException> {
            queue.remove()
        }

    }

    @Test
    fun variousAddSituationsTest() {

        val queue = TierStateQueue(10)

        queue.add(0, null)
        queue.add(9, null)

        queue.add(1, 0)
        queue.add(2, 1)

        // distance is the same and the state is already in correct tier
        queue.add(1, 9)

        // distance decreased and the state has to be moved to another tier
        queue.add(2, 0)

        assertEquals(listOf(0, 9), queue.remove().toList())
        assertEquals(listOf(1, 2), queue.remove().toList())

        // distance is the same but the state is not in any tier
        queue.add(0, null)

        // distance decreased and the state is not in any tier
        queue.add(1, null)

        assertEquals(listOf(0, 1), queue.remove().toList())

    }

}