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

class DecreasingStateMapTest {

    val a = setOf(true)
    val b = setOf(false)
    val ab = setOf(true, false)

    val solver = SetSolver(tt = ab)

    @Test
    fun basicUsageTest() {
        solver.run {
            val map = decreasingStateMap(10)

            for (s in -10..20) {    //we are checking also out of range
                assertEquals(s in map, s in 0..9)
            }

            assertTrue(map.isNotEmpty())
            assertFalse(map.isEmpty())

            assertTrue(map.decreaseKey(4, a))

            assertFalse(map.decreaseKey(4, a))
            assertTrue(map.decreaseKey(4, b))
            assertTrue(map.decreaseKey(5, b))

            assertTrue(map[4] equal ff)
            assertTrue(map[5] equal b)

            assertTrue(map[12] equal ff) //out of range access

            assertTrue(map.decreaseKey(5, ff))

            for (s in 0..9) {
                assertEquals(s in map, s !in 4..5)
            }
        }
    }

    @Test
    fun outOfRangeIncreaseTest() {
        solver.run {
            val map = decreasingStateMap(1)

            assertFailsWith<IllegalArgumentException> {
                map.decreaseKey(1, ab)
            }

            assertFailsWith<IllegalArgumentException> {
                map.decreaseKey(-1, ab)
            }
        }
    }

    @Test(timeout = 5000)
    fun concurrentEventualPublishTest() {

        // This test is a very basic sample of the parallel read, sequential write
        // semantics. Every thread will write to predefined position but read from positions
        // managed by other threads. Eventually, all information is exchanged and threads converge
        // to a common fixed point.

        val states = 2000
        BitSetSolver(BitSet().apply { set(0, states) }).run {

            val map = decreasingStateMap(states)

            for (s in 0 until states) {
                map.decreaseKey(s, BitSet().apply { set(s) })
            }

            (0..3).map { id -> thread {
                val myStates = (0 until states).filter { it % 4 == id }
                do {
                    for (s in myStates) {
                        map.decreaseKey(s, map[s-1])
                        map.decreaseKey(s, map[s+1])
                    }
                } while (myStates.any { map[it].isSat() })
            } }.map { it.join() }

            assertTrue(map.isEmpty())
        }
    }
}