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
            assertTrue(tt.isSat())
            assertTrue(ff.isNotSat())
            assertFalse(tt.isNotSat())
            assertFalse(ff.isSat())
        }
    }

    @Test
    fun equalTest() {
        solver.run {
            assertTrue(tt equal tt)
            assertTrue(ff equal ff)
            assertTrue(tt notEqual ff)
            assertTrue(ff notEqual tt)
            assertFalse(tt notEqual tt)
            assertFalse(ff notEqual ff)
            assertFalse(tt equal ff)
            assertFalse(ff equal tt)
        }
    }

    @Test
    fun notTest() {
        solver.run {
            assertTrue(tt.not().isNotSat())
            assertTrue(ff.not().isSat())
        }
    }

    @Test
    fun andTest() {
        solver.run {
            assertTrue((tt and tt).isSat())
            assertTrue((tt and ff).isNotSat())
            assertTrue((ff and tt).isNotSat())
            assertTrue((ff and ff).isNotSat())
        }
    }

    @Test
    fun orTest() {
        solver.run {
            assertTrue((tt or tt).isSat())
            assertTrue((tt or ff).isSat())
            assertTrue((ff or tt).isSat())
            assertTrue((ff or ff).isNotSat())
        }
    }

    @Test
    fun tryOrTest() {
        solver.run {
            assertEquals(tt tryOr tt, null)
            assertEquals(tt tryOr ff, null)
            assertEquals(ff tryOr tt, tt)
            assertEquals(ff tryOr ff, null)
        }
    }

    @Test
    fun tryAndTest() {
        solver.run {
            assertEquals(tt tryAnd tt, null)
            assertEquals(tt tryAnd ff, ff)
            assertEquals(ff tryAnd tt, null)
            assertEquals(ff tryAnd ff, null)
        }
    }
}