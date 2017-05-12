package com.github.sybila.model

import com.github.sybila.solver.BitSetSolver
import com.github.sybila.solver.SetSolver
import org.junit.Test
import java.util.*
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableStateMapTest {

    val f = null
    val a = setOf(true)
    val b = setOf(false)
    val ab = setOf(true, false)

    val solver = SetSolver(universe = ab)

    @Test
    fun basicUsageTest() {
        solver.run {
            val map = mutableStateMap(10)

            for (s in 0..20) {    //we are checking also out of range
                assertTrue(s !in map)
            }

            assertTrue(map.isEmpty())
            assertFalse(map.isNotEmpty())

            assertTrue(map.increaseKey(4, a))

            assertTrue(map.isNotEmpty())
            assertFalse(map.isEmpty())

            assertFalse(map.increaseKey(4, a))
            assertTrue(map.increaseKey(4, b))
            assertTrue(map.increaseKey(5, b))

            assertTrue(map[4] equal ab)
            assertTrue(map[5] equal b)

            assertTrue(map[12] equal f) //out of range access

            for (s in 0..9) {
                assertEquals(s in map, s in 4..5)
            }

            assertEquals(listOf(4,5), map.states.toList())
            assertEquals(listOf(4 to ab, 5 to b), map.entries.toList())

        }
    }

    @Test
    fun negativeKeysTest() {
        solver.run {
            val map = mutableStateMap(0)

            assertFailsWith<IllegalArgumentException> {
                map[-1]
            }

            assertFailsWith<IllegalArgumentException> {
                map.increaseKey(-2, ab)
            }

            assertFailsWith<IllegalArgumentException> {
                -2 in map
            }

        }
    }

    @Test
    fun outOfRangeIncreaseTest() {
        solver.run {
            val map = mutableStateMap(1)

            assertFailsWith<IllegalArgumentException> {
                map.increaseKey(1, ab)
            }
        }
    }

    @Test
    fun concurrentEventualPublishTest() {

        // This test is a very basic sample of the parallel read, sequential write
        // semantics. Every thread will write to predefined position but read from positions
        // managed by other threads. Eventually, all information is exchanged and threads converge
        // to a common fixed point.

        val states = 2000
        BitSetSolver(BitSet().apply { set(0, states) }).run {

            val map = mutableStateMap(states)

            for (s in 0 until states) {
                map.increaseKey(s, BitSet().apply { set(s) })
            }

            (0..3).map { id -> thread {
                val myStates = (0 until states).filter { it % 4 == id }
                do {
                    for (s in myStates) {
                        (if (s != 0) map[s-1] else null)?.let { left ->
                            map.increaseKey(s, left)
                        }
                        map[s+1]?.let { right ->
                            map.increaseKey(s, right)
                        }
                    }
                } while (myStates.any { map[it] notEqual universe })
            } }.map { it.join() }

            assertEquals((0 until states).toList(), map.states.toList())
            assertEquals((0 until states).map { it to universe }, map.entries.toList())

        }
    }
}