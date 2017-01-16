package com.github.sybila.checker.solver

import com.github.daemontus.asSome
import com.github.daemontus.none
import com.github.sybila.checker.*
import com.github.sybila.checker.map.asStateMap
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BitSetSolverTest {


    @Test
    fun unsupportedParameterTest() {
        val solver = BitSetSolver(10)
        assertFailsWith<UnsupportedParameterType> {
            solver.run {
                setOf(0,1,2,3).asParams().isSat()
            }
        }
    }

    @Test
    fun prettyPrintTest() {
        val andParams = And(TT, BitSet().apply { set(1); set(2); set(4) }.asParams())
        val orParams = Or(BitSet().apply { set(1); set(2); set(4) }.asParams(), TT)
        val notParams = Not(BitSet().apply { set(3); set(4) }.asParams())
        val map = mapOf(1 to andParams, 2 to TT).asStateMap()
        val solver = BitSetSolver(5)
        solver.run {
            assertEquals("(and (0 1 2 3 4) (1 2 4))", andParams.prettyPrint())
            assertEquals("(or (1 2 4) (0 1 2 3 4))", orParams.prettyPrint())
            assertEquals("(not (3 4))", notParams.prettyPrint())
            assertEquals("(map (1 (and (0 1 2 3 4) (1 2 4))) (2 (0 1 2 3 4)))", map.prettyPrint())
            assertEquals("(false)", null.prettyPrint())
            assertFailsWith<UnsupportedParameterType> {
                setOf(1,2,3).asParams().prettyPrint()
            }
        }
    }

    @Test
    fun isSatTest() {
        val size = 5
        BitSetSolver(size).run {
            val tt = BitSet().apply { set(0, size) }
            val p1 = BitSet().apply { set(0); set(3) }
            assertEquals(null, BitSet().asParams().isSat())         //False
            assertEquals(tt.asParams(), tt.asParams().isSat())      //TT
            assertEquals(tt.asParams(), TT.isSat())                 //explicit TT
            assertEquals(p1.asParams(), p1.asParams().isSat())      //p1
            assertEquals(p1.asParams(), And(p1.asParams(), p1.asParams()).isSat())  //and simplification
            assertEquals(p1.asParams(), Or(p1.asParams(), p1.asParams()).isSat())   //or simplification
            assertEquals(BitSet().apply {
                set(1); set(2); set(4)
            }.asParams(), p1.asParams().not()?.isSat())   //not simplification
        }
    }

    @Test
    fun extendWithTest() {
        val size = 5
        BitSetSolver(size).run {
            val p1 = BitSet().apply { set(0); set(3) }.asParams()
            val p2 = BitSet().apply { set(0); set(2) }.asParams()
            val tt = BitSet().apply { set(0, size) }.asParams()
            assertEquals(tt.asSome(), null.extendWith(tt))
            assertEquals(p1.asSome(), null.extendWith(p1))
            assertEquals(none(), tt.extendWith(null))
            assertEquals(none(), tt.extendWith(p1))
            assertEquals(tt.asSome(), p1.extendWith(tt))
            assertEquals(BitSet().apply {
                set(0); set(2); set(3)
            }.asParams().asSome(), p1.extendWith(p2))
        }
    }

}