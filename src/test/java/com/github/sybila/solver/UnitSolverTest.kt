package com.github.sybila.solver

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnitSolverTest {

    private val solver = UnitSolver()

    @Test
    fun isSatTest() {
        solver.run {
            assertTrue(universe.isSat())
            assertTrue(null.isNotSat())
            assertFalse(universe.isNotSat())
            assertFalse(null.isSat())
        }
    }

    @Test
    fun equalTest() {
        solver.run {
            assertTrue(universe equal universe)
            assertTrue(null equal null)
            assertTrue(universe notEqual null)
            assertTrue(null notEqual universe)
            assertFalse(universe notEqual universe)
            assertFalse(null notEqual null)
            assertFalse(universe equal null)
            assertFalse(null equal universe)
        }
    }

    @Test
    fun notTest() {
        solver.run {
            assertTrue(universe.not().isNotSat())
            assertTrue(null.not().isSat())
        }
    }

    @Test
    fun andTest() {
        solver.run {
            assertTrue((universe and universe).isSat())
            assertTrue((universe and null).isNotSat())
            assertTrue((null and universe).isNotSat())
            assertTrue((null and null).isNotSat())
        }
    }

    @Test
    fun orTest() {
        solver.run {
            assertTrue((universe or universe).isSat())
            assertTrue((universe or null).isSat())
            assertTrue((null or universe).isSat())
            assertTrue((null or null).isNotSat())
        }
    }

    @Test
    fun tryOrTest() {
        solver.run {
            assertEquals(universe tryOr universe, null)
            assertEquals(universe tryOr null, null)
            assertEquals(null tryOr universe, universe)
            assertEquals(null tryOr null, null)
        }
    }

}