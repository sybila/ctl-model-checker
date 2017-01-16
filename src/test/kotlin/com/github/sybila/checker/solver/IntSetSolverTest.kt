package com.github.sybila.checker.solver

import com.github.daemontus.asSome
import com.github.daemontus.none
import com.github.sybila.checker.*
import com.github.sybila.checker.map.asStateMap
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class IntSetSolverTest {


    @Test
    fun unsupportedParameterTest() {
        val solver = IntSetSolver(setOf(0,1,2,3,4))
        assertFailsWith<UnsupportedParameterType> {
            solver.run {
                BitSet().apply { set(1, 3) }.asParams().isSat()
            }
        }
    }

    @Test
    fun prettyPrintTest() {
        val andParams = And(TT, setOf(1,2,4).asParams())
        val orParams = Or(setOf(1,2,4).asParams(), TT)
        val notParams = Not(setOf(3,4).asParams())
        val map = mapOf(1 to andParams, 2 to TT).asStateMap()
        val solver = IntSetSolver(setOf(0,1,2,3,4))
        solver.run {
            assertEquals("(and (0 1 2 3 4) (1 2 4))", andParams.prettyPrint())
            assertEquals("(or (1 2 4) (0 1 2 3 4))", orParams.prettyPrint())
            assertEquals("(not (3 4))", notParams.prettyPrint())
            assertEquals("(map (1 (and (0 1 2 3 4) (1 2 4))) (2 (0 1 2 3 4)))", map.prettyPrint())
            assertEquals("(false)", null.prettyPrint())
            assertFailsWith<UnsupportedParameterType> {
                BitSet().apply { set(3) }.asParams().prettyPrint()
            }
        }
    }

    @Test
    fun isSatTest() {
        val size = 5
        IntSetSolver((0 until size).toSet()).run {
            val tt = (0 until size).toSet()
            val p1 = setOf(0, 3)
            assertEquals(null, setOf<Int>().asParams().isSat())         //False
            assertEquals(tt.asParams(), tt.asParams().isSat())      //TT
            assertEquals(tt.asParams(), TT.isSat())                 //explicit TT
            assertEquals(p1.asParams(), p1.asParams().isSat())      //p1
            assertEquals(p1.asParams(), And(p1.asParams(), p1.asParams()).isSat())  //and simplification
            assertEquals(p1.asParams(), Or(p1.asParams(), p1.asParams()).isSat())   //or simplification
            assertEquals(setOf(1,2,4).asParams(), p1.asParams().not()?.isSat())   //not simplification
        }
    }

    @Test
    fun extendWithTest() {
        val size = 5
        IntSetSolver((0 until size).toSet()).run {
            val p1 = setOf(0, 3).asParams()
            val p2 = setOf(0, 2).asParams()
            val tt = (0 until size).toSet().asParams()
            assertEquals(tt.asSome(), null.extendWith(tt))
            assertEquals(p1.asSome(), null.extendWith(p1))
            assertEquals(none(), tt.extendWith(null))
            assertEquals(none(), tt.extendWith(p1))
            assertEquals(tt.asSome(), p1.extendWith(tt))
            assertEquals(setOf(0, 2, 3).asParams().asSome(), p1.extendWith(p2))
        }
    }

}