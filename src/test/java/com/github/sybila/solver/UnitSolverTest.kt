package com.github.sybila.solver

import org.junit.Test
import kotlin.test.assertTrue

class UnitSolverTest {

    private val solver = UnitSolver()

    @Test
    fun isSatTest() {
        solver.run {
            assertTrue(universe.isSat())
            assertTrue(null.isNotSat())
        }
    }

    @Test
    fun equalTest() {
        solver.run {
            assertTrue(universe equal universe)
            assertTrue(null equal null)
            assertTrue(universe notEqual null)
            assertTrue(null notEqual universe)
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

}