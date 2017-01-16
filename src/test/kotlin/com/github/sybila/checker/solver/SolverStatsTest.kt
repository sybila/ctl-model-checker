package com.github.sybila.checker.solver

import com.github.sybila.checker.TT
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

class SolverStatsTest {

    @Test
    fun bitSetSolverTest() {
        BitSetSolver(1).run {
            //dry run
            SolverStats.reset(null)
            TT.isSat()
            SolverStats.printGlobal()
            //actually print something
            val bytes = ByteArrayOutputStream()
            val printTo = PrintStream(bytes)
            printTo.println()   //prettier result regex
            SolverStats.reset(printTo)
            TT.isSat()
            SolverStats.printGlobal()
            val printed = String(bytes.toByteArray(), StandardCharsets.UTF_8)
            assertTrue(printed.matches(
"""
Started solver throughput measuring\. Metric: No\. of solver calls per second\.
Total elapse time: \d+ ms
Average throughput: 1/s
""".toRegex()
            ))
        }
    }

    @Test
    fun intSetSolverTest() {
        IntSetSolver(setOf(0,1)).run {
            //dry run
            SolverStats.reset(null)
            TT.isSat()
            SolverStats.printGlobal()
            //actually print something
            val bytes = ByteArrayOutputStream()
            val printTo = PrintStream(bytes)
            printTo.println()   //prettier result regex
            SolverStats.reset(printTo)
            TT.isSat()
            SolverStats.printGlobal()
            val printed = String(bytes.toByteArray(), StandardCharsets.UTF_8)
            assertTrue(printed.matches(
"""
Started solver throughput measuring\. Metric: No\. of solver calls per second\.
Total elapse time: \d+ ms
Average throughput: 1/s
""".toRegex()
            ))
        }
    }


}
